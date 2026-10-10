package com.ghartak.app.data.repository

import android.net.Uri
import com.ghartak.app.data.firebase.FirestoreSchema
import com.ghartak.app.data.firebase.toAppResult
import com.ghartak.app.data.model.buildDeliveryCreateMap
import com.ghartak.app.data.model.toDelivery
import com.ghartak.app.data.model.toRiderLocation
import com.ghartak.app.domain.model.Address
import com.ghartak.app.domain.model.Delivery
import com.ghartak.app.domain.model.DeliveryStatus
import com.ghartak.app.domain.model.ParcelType
import com.ghartak.app.domain.model.RiderLocationUpdate
import com.ghartak.app.domain.repository.DeliveryRepository
import com.ghartak.app.util.Result
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DeliveryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val storage: FirebaseStorage
) : DeliveryRepository {

    private val deliveries get() = firestore.collection(FirestoreSchema.Collections.DELIVERIES)
    private val riderLocations get() = firestore.collection(FirestoreSchema.Collections.RIDER_LOCATIONS)

    private fun requireUid(): String =
        firebaseAuth.currentUser?.uid ?: error("Not signed in")

    override suspend fun createDelivery(
        pickup: Address,
        drop: Address,
        receiverName: String,
        receiverPhone: String,
        parcelType: ParcelType,
        parcelWeightKg: Double?,
        parcelDescription: String?,
        parcelImageUrl: String?
    ): Result<Delivery> = try {
        // Pricing kept client-side for this scaffold; a production build should compute this
        // (and the OTP below) in a Cloud Function so a tampered client can't under-pay or read
        // another user's OTP before the write is authorized by security rules.
        val distanceKm = pickup.point.distanceKmTo(drop.point)
        val deliveryFee = (25 + distanceKm * 8).coerceAtLeast(30.0)
        val platformFee = 10.0
        val otp = Random.nextInt(1000, 9999).toString()

        val data = buildDeliveryCreateMap(
            customerId = requireUid(),
            pickup = pickup,
            drop = drop,
            receiverName = receiverName,
            receiverPhone = receiverPhone,
            parcelType = parcelType,
            parcelWeightKg = parcelWeightKg,
            parcelDescription = parcelDescription,
            parcelImageUrl = parcelImageUrl,
            deliveryFee = deliveryFee,
            platformFee = platformFee,
            otp = otp
        )

        val docRef = deliveries.add(data).await()
        val created = docRef.get().await()
        Result.Success(created.toDelivery())
    } catch (e: Exception) {
        e.toAppResult()
    }

    override suspend fun cancelDelivery(deliveryId: String): Result<Unit> = try {
        deliveries.document(deliveryId)
            .update("status", DeliveryStatus.CANCELLED.name)
            .await()
        Result.Success(Unit)
    } catch (e: Exception) {
        e.toAppResult()
    }

    override suspend fun getDelivery(deliveryId: String): Result<Delivery> = try {
        val doc = deliveries.document(deliveryId).get().await()
        if (doc.exists()) Result.Success(doc.toDelivery())
        else Result.Error("Delivery not found", "NOT_FOUND")
    } catch (e: Exception) {
        e.toAppResult()
    }

    /**
     * Two nested Firestore snapshot listeners: one on the delivery doc itself, and — once a
     * rider is assigned — a second on that rider's live-location doc. Both pushes are
     * server-driven, so the customer-facing map updates the moment the rider's location write
     * lands, no polling interval needed.
     */
    override fun observeTracking(deliveryId: String): Flow<Result<Pair<Delivery, RiderLocationUpdate?>>> = callbackFlow {
        var latestDelivery: Delivery? = null
        var latestLocation: RiderLocationUpdate? = null
        var riderLocationRegistration: ListenerRegistration? = null
        var currentRiderId: String? = null

        fun emitIfReady() {
            val delivery = latestDelivery ?: return
            trySend(Result.Success(delivery to latestLocation))
        }

        fun attachRiderLocationListener(riderId: String) {
            if (riderId == currentRiderId) return
            riderLocationRegistration?.remove()
            currentRiderId = riderId
            riderLocationRegistration = riderLocations.document(riderId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(error.toAppResult())
                        return@addSnapshotListener
                    }
                    latestLocation = snapshot?.toRiderLocation()
                    emitIfReady()
                }
        }

        val deliveryRegistration = deliveries.document(deliveryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(error.toAppResult())
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(Result.Error("Delivery not found", "NOT_FOUND"))
                    return@addSnapshotListener
                }
                val delivery = snapshot.toDelivery()
                latestDelivery = delivery
                delivery.riderId?.let(::attachRiderLocationListener)
                emitIfReady()
            }

        awaitClose {
            deliveryRegistration.remove()
            riderLocationRegistration?.remove()
        }
    }

    override suspend fun getDeliveryHistory(pageSize: Int, lastVisible: Delivery?): Result<List<Delivery>> = try {
        var query: Query = deliveries
            .whereEqualTo(FirestoreSchema.DeliveryField.CUSTOMER_ID, requireUid())
            .orderBy(FirestoreSchema.DeliveryField.CREATED_AT, Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (lastVisible != null) {
            query = query.startAfter(Timestamp(Date(lastVisible.createdAt)))
        }

        val snapshot = query.get().await()
        Result.Success(snapshot.documents.map { it.toDelivery() })
    } catch (e: Exception) {
        e.toAppResult()
    }

    /**
     * Runs as a Firestore transaction so a stale read can never mark a delivery DELIVERED off
     * an OTP another concurrent write already changed.
     */
    override suspend fun verifyDeliveryOtp(deliveryId: String, otp: String): Result<Delivery> = try {
        val docRef = deliveries.document(deliveryId)
        firestore.runTransaction { tx ->
            val snapshot = tx.get(docRef)
            val storedOtp = snapshot.getString(FirestoreSchema.DeliveryField.OTP)
            if (storedOtp.isNullOrBlank() || storedOtp != otp) {
                throw IllegalStateException("INVALID_OTP")
            }
            tx.update(
                docRef,
                mapOf(
                    FirestoreSchema.DeliveryField.STATUS to DeliveryStatus.DELIVERED.name,
                    "deliveredAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
        val refreshed = docRef.get().await()
        Result.Success(refreshed.toDelivery())
    } catch (e: Exception) {
        if (e.message == "INVALID_OTP") Result.Error("Invalid or expired OTP", "INVALID_OTP")
        else e.toAppResult()
    }

    override suspend fun uploadParcelImage(localFileUri: String, deliveryId: String): Result<String> = try {
        val fileName = "${UUID.randomUUID()}.jpg"
        // uid is its own path segment (not just embedded in the file name) so storage.rules can
        // enforce "only the uploader may write here" with a plain wildcard match — see
        // /firebase/storage.rules.
        val ref = storage.reference.child("deliveries/$deliveryId/${requireUid()}/$fileName")
        ref.putFile(Uri.parse(localFileUri)).await()
        val downloadUrl = ref.downloadUrl.await()
        Result.Success(downloadUrl.toString())
    } catch (e: Exception) {
        e.toAppResult()
    }
}

/** Haversine distance in kilometers — used only for the client-side fee estimate above. */
private fun com.ghartak.app.domain.model.LatLngPoint.distanceKmTo(other: com.ghartak.app.domain.model.LatLngPoint): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(other.lat - lat)
    val dLng = Math.toRadians(other.lng - lng)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat)) *
        Math.sin(dLng / 2) * Math.sin(dLng / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadiusKm * c
}
