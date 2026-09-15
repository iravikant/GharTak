-- GharTak database schema
-- Run against Railway-managed MySQL (or local MySQL/MariaDB 8+) before deploying the API.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- users: shared identity for customers and riders (role decides which
-- profile table applies). Admin accounts also live here with role='admin'.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(160) NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('customer', 'rider', 'admin') NOT NULL,
    profile_photo_url VARCHAR(500) NULL,
    status ENUM('active', 'suspended') NOT NULL DEFAULT 'active',
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customers (
    user_id CHAR(36) PRIMARY KEY,
    default_payment_method VARCHAR(30) NULL,
    CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS riders (
    user_id CHAR(36) PRIMARY KEY,
    approval_status ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending',
    is_online TINYINT(1) NOT NULL DEFAULT 0,
    vehicle_id CHAR(36) NULL,
    rating_avg DECIMAL(3,2) NOT NULL DEFAULT 5.00,
    total_deliveries INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_riders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rider_documents (
    id CHAR(36) PRIMARY KEY,
    rider_id CHAR(36) NOT NULL,
    document_type VARCHAR(60) NOT NULL, -- e.g. 'license', 'id_proof', 'vehicle_rc'
    document_url VARCHAR(500) NOT NULL,
    verified TINYINT(1) NOT NULL DEFAULT 0,
    uploaded_at BIGINT NOT NULL,
    CONSTRAINT fk_docs_rider FOREIGN KEY (rider_id) REFERENCES riders(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS vehicles (
    id CHAR(36) PRIMARY KEY,
    rider_id CHAR(36) NOT NULL,
    vehicle_type VARCHAR(40) NOT NULL, -- bike, scooter, van, etc.
    registration_number VARCHAR(30) NOT NULL,
    model VARCHAR(80) NULL,
    CONSTRAINT fk_vehicles_rider FOREIGN KEY (rider_id) REFERENCES riders(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Latest known rider location. A high-frequency append-only history table would be
-- added later if replay/analytics is needed; for live tracking, latest-only suffices.
CREATE TABLE IF NOT EXISTS rider_locations (
    rider_id CHAR(36) PRIMARY KEY,
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    bearing FLOAT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT fk_riderloc_rider FOREIGN KEY (rider_id) REFERENCES riders(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS addresses (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    label VARCHAR(60) NULL, -- "Home", "Office"
    formatted_address VARCHAR(500) NOT NULL,
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS service_areas (
    id CHAR(36) PRIMARY KEY,
    city VARCHAR(120) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    center_lat DOUBLE NOT NULL,
    center_lng DOUBLE NOT NULL,
    radius_km DOUBLE NOT NULL DEFAULT 15
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pricing_rules (
    id CHAR(36) PRIMARY KEY,
    service_area_id CHAR(36) NULL,
    base_fare DECIMAL(10,2) NOT NULL,
    per_km_rate DECIMAL(10,2) NOT NULL,
    platform_fee DECIMAL(10,2) NOT NULL,
    parcel_type_multiplier JSON NULL, -- e.g. {"ELECTRONICS": 1.2, "DOCUMENTS": 0.9}
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_pricing_area FOREIGN KEY (service_area_id) REFERENCES service_areas(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- deliveries — the core entity. Fields mirror spec section 15 exactly.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deliveries (
    id CHAR(36) PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    rider_id CHAR(36) NULL,
    pickup_lat DOUBLE NOT NULL,
    pickup_lng DOUBLE NOT NULL,
    pickup_address VARCHAR(500) NOT NULL,
    drop_lat DOUBLE NOT NULL,
    drop_lng DOUBLE NOT NULL,
    drop_address VARCHAR(500) NOT NULL,
    receiver_name VARCHAR(120) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    parcel_type VARCHAR(30) NOT NULL,
    parcel_weight DECIMAL(6,2) NULL,
    parcel_description VARCHAR(500) NULL,
    parcel_image_url VARCHAR(500) NULL,
    delivery_fee DECIMAL(10,2) NOT NULL,
    platform_fee DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    payment_status ENUM('PENDING','PROCESSING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    delivery_status ENUM(
        'CREATED','SEARCHING_RIDER','RIDER_ASSIGNED','RIDER_GOING_TO_PICKUP',
        'ARRIVED_AT_PICKUP','PARCEL_PICKED_UP','OUT_FOR_DELIVERY',
        'RIDER_NEAR_DESTINATION','DELIVERED','CANCELLED','FAILED'
    ) NOT NULL DEFAULT 'CREATED',
    otp CHAR(6) NULL,
    otp_expires_at BIGINT NULL,
    created_at BIGINT NOT NULL,
    accepted_at BIGINT NULL,
    picked_up_at BIGINT NULL,
    delivered_at BIGINT NULL,
    cancelled_at BIGINT NULL,
    cancel_reason VARCHAR(255) NULL,
    CONSTRAINT fk_deliveries_customer FOREIGN KEY (customer_id) REFERENCES users(id),
    CONSTRAINT fk_deliveries_rider FOREIGN KEY (rider_id) REFERENCES users(id),
    INDEX idx_deliveries_customer (customer_id),
    INDEX idx_deliveries_rider (rider_id),
    INDEX idx_deliveries_status (delivery_status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS delivery_status_history (
    id CHAR(36) PRIMARY KEY,
    delivery_id CHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL,
    changed_at BIGINT NOT NULL,
    changed_by CHAR(36) NULL, -- user_id of rider/customer/admin/system that triggered it
    CONSTRAINT fk_history_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS delivery_items (
    id CHAR(36) PRIMARY KEY,
    delivery_id CHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_items_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS delivery_proofs (
    id CHAR(36) PRIMARY KEY,
    delivery_id CHAR(36) NOT NULL,
    proof_type ENUM('photo', 'signature', 'otp') NOT NULL,
    proof_url VARCHAR(500) NULL,
    receiver_name VARCHAR(120) NULL,
    lat DOUBLE NULL,
    lng DOUBLE NULL,
    captured_at BIGINT NOT NULL,
    CONSTRAINT fk_proofs_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
    id CHAR(36) PRIMARY KEY,
    delivery_id CHAR(36) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    method ENUM('COD', 'UPI', 'ONLINE', 'WALLET') NOT NULL,
    status ENUM('PENDING','PROCESSING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    provider_reference VARCHAR(120) NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT fk_payments_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transactions (
    id CHAR(36) PRIMARY KEY,
    payment_id CHAR(36) NOT NULL,
    type ENUM('charge', 'refund') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_transactions_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(500) NOT NULL,
    data JSON NULL,
    read_at BIGINT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chat_rooms (
    id CHAR(36) PRIMARY KEY,
    delivery_id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    rider_id CHAR(36) NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_chatrooms_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chat_messages (
    id CHAR(36) PRIMARY KEY,
    chat_room_id CHAR(36) NOT NULL,
    sender_id CHAR(36) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    sent_at BIGINT NOT NULL,
    read_at BIGINT NULL,
    CONSTRAINT fk_messages_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ratings (
    id CHAR(36) PRIMARY KEY,
    delivery_id CHAR(36) NOT NULL,
    rated_by CHAR(36) NOT NULL,
    rated_user CHAR(36) NOT NULL,
    score TINYINT NOT NULL, -- 1-5
    comment VARCHAR(500) NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_ratings_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS complaints (
    id CHAR(36) PRIMARY KEY,
    delivery_id CHAR(36) NULL,
    raised_by CHAR(36) NOT NULL,
    category VARCHAR(60) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status ENUM('open', 'in_review', 'resolved', 'dismissed') NOT NULL DEFAULT 'open',
    created_at BIGINT NOT NULL,
    resolved_at BIGINT NULL,
    CONSTRAINT fk_complaints_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE SET NULL
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;
