CREATE TABLE member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE place (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(30) NOT NULL,
    region VARCHAR(50) NOT NULL,
    address VARCHAR(255) NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    price_range VARCHAR(50) NULL,
    open_hours VARCHAR(1000) NULL,
    opening_time TIME(6) NULL,
    closing_time TIME(6) NULL,
    estimated_cost INT NULL,
    recommended_visit_minutes INT NULL,
    source VARCHAR(30) NOT NULL,
    external_id VARCHAR(255) NULL,
    last_synced_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_place_source_external_id UNIQUE (source, external_id),
    INDEX idx_place_region_category (region, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE itinerary (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    budget INT NULL,
    status VARCHAR(20) NOT NULL,
    share_token VARCHAR(64) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_itinerary_share_token UNIQUE (share_token),
    CONSTRAINT fk_itinerary_owner FOREIGN KEY (owner_id) REFERENCES member (id),
    INDEX idx_itinerary_owner_id (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE itinerary_participant (
    id BIGINT NOT NULL AUTO_INCREMENT,
    itinerary_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    participant_role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_itinerary_participant UNIQUE (itinerary_id, member_id),
    CONSTRAINT fk_participant_itinerary FOREIGN KEY (itinerary_id) REFERENCES itinerary (id),
    CONSTRAINT fk_participant_member FOREIGN KEY (member_id) REFERENCES member (id),
    INDEX idx_participant_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE itinerary_day (
    id BIGINT NOT NULL AUTO_INCREMENT,
    itinerary_id BIGINT NOT NULL,
    day_number INT NOT NULL,
    date DATE NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_itinerary_day UNIQUE (itinerary_id, day_number),
    CONSTRAINT fk_itinerary_day_itinerary FOREIGN KEY (itinerary_id) REFERENCES itinerary (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE itinerary_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    itinerary_day_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    visit_order INT NOT NULL,
    start_time TIME(6) NULL,
    end_time TIME(6) NULL,
    memo VARCHAR(500) NULL,
    travel_minutes_before INT NULL,
    distance_km_before DECIMAL(10,2) NULL,
    estimated_cost INT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_itinerary_item_order UNIQUE (itinerary_day_id, visit_order),
    CONSTRAINT fk_item_itinerary_day FOREIGN KEY (itinerary_day_id) REFERENCES itinerary_day (id),
    CONSTRAINT fk_item_place FOREIGN KEY (place_id) REFERENCES place (id),
    INDEX idx_itinerary_item_place_id (place_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE place_opening_hour (
    place_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    opening_time TIME(6) NOT NULL,
    closing_time TIME(6) NOT NULL,
    closes_next_day BOOLEAN NOT NULL,
    CONSTRAINT fk_opening_hour_place FOREIGN KEY (place_id) REFERENCES place (id) ON DELETE CASCADE,
    INDEX idx_opening_hour_place_id (place_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
