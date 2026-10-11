package com.ghartak.app.data.repository

import android.net.Uri
import com.ghartak.app.data.firebase.FirestoreSchema
import com.ghartak.app.data.firebase.toAppResult
import com.ghartak.app.data.model.buildDeliveryCreateMap
import com.ghartak.app.data.model.buildDeliveryRequestMap
import com.ghartak.app.data.model.toDelivery
import com.ghartak.app.data.model.toRiderLocation
import com.ghartak.app.domain.model.Address
import com.ghartak.app.domain.model.Delivery
import com.ghartak.app.domain.model.DeliveryStatus
import com.ghartak.app.domain.model.ParcelType
import com.ghartak.app.domain.model.PaymentMethod
import com.ghartak.app.domain.model.PricingRules
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
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

@Singleton
class DeliveryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val storage: FirebaseStorage
) : DeliveryRepository {

    private val deliveries get() = firestore.collection(FirestoreSchema.Collections.DELIVERIES)
    private val requests get() = firestore.collection(FirestoreSchema.Collections.DELIVERY_REQUESTS)
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
        specialInstructions: String?,
        parcelImageUri: String?,
        paymentMethod: PaymentMethod,
        deliveryFee: Double,
        platformFee: Double,
        distanceKm: Double
    ): Result<Delivery> {
        return try {
            // Reserve the document id up-front so the parcel photo can be stored under it.
            val docRef = deliveries.document()

            // A failed photo upload must not block the booking itself.
            val imageUrl = parcelImageUri?.let { uri ->
                (uploadParcelImage(uri, docRef.id) as? Result.Success)?.data
            }

            // The OTP is generated here for the scaffold; a production build should generate it
            // (and the fee above) in a Cloud Function so a tampered client can't forge either.
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
                parcelImageUrl = imageUrl,
                specialInstructions = specialInstructions,
                paymentMethod = paymentMethod,
                deliveryFee = deliveryFee,
                platformFee = platformFee,
                otp = otp
            )

            // The rider-visible offer carries only what a rider needs to decide (coarse areas, distance,
            // earnings) — never the receiver's name/phone or exact addresses. Both docs commit together.
            val uid = requireUid()
            val offer = buildDeliveryRequestMap(
                deliveryId = docRef.id,
                customerId = uid,
                pickup = pickup,
                drop = drop,
                parcelType = parcelType,
                weightKg = parcelWeightKg,
                distanceKm = distanceKm,
                riderEarnings = PricingRules.riderEarnings(deliveryFee)
            )
            firestore.batch()
                .set(docRef, data)
                .set(requests.document(docRef.id), offer)
                .commit()
                .await()
            Result.Success(docRef.get().await().toDelivery())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.toAppResult()
        }
    }

    override suspend fun cancelDelivery(deliveryId: String): Result<Unit> {
        return try {
            deliveries.document(deliveryId)
                .update(
                    mapOf(
                        "status" to DeliveryStatus.CANCELLED.name,
                        "cancelledAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            // Withdraw the rider offer too (best effort: older deliveries have none).
            runCatching { requests.document(deliveryId).update("status", "CANCELLED").await() }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.toAppResult()
        }
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
