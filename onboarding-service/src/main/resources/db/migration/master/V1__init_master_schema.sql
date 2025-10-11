CREATE TABLE plans (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(50) UNIQUE NOT NULL,
                       description TEXT,
                       price NUMERIC(12,2) NOT NULL,
                       duration_days INT NOT NULL DEFAULT 30,
                       features JSONB,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE hospitals (
                           id SERIAL PRIMARY KEY,
                           name VARCHAR(255) UNIQUE NOT NULL,
                           email VARCHAR(255) UNIQUE NOT NULL,
                           phone VARCHAR(50),
                           hospital_type VARCHAR(100),
                           country VARCHAR(100),
                           state VARCHAR(100),
                           city VARCHAR(100),
                           address TEXT,
                           plan_id INT REFERENCES plans(id) ON DELETE SET NULL,
                           db_name VARCHAR(100) NOT NULL,
                           db_user VARCHAR(100) NOT NULL,
                           db_password VARCHAR(255) NOT NULL,
                           is_active BOOLEAN DEFAULT FALSE,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE hospital_payments (
                                   id SERIAL PRIMARY KEY,
                                   hospital_id INT REFERENCES hospitals(id) ON DELETE CASCADE,
                                   plan_id INT REFERENCES plans(id) ON DELETE SET NULL,
                                   payment_reference VARCHAR(100) UNIQUE NOT NULL,
                                   payment_gateway VARCHAR(50) DEFAULT 'Paystack',
                                   amount NUMERIC(12,2) NOT NULL,
                                   status VARCHAR(50) DEFAULT 'PENDING',
                                   paid_at TIMESTAMP,
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE hospital_admins (
                                 id SERIAL PRIMARY KEY,
                                 hospital_id INT REFERENCES hospitals(id) ON DELETE CASCADE,
                                 first_name VARCHAR(150) NOT NULL,
                                 last_name VARCHAR(150) NOT NULL,
                                 email VARCHAR(150) UNIQUE NOT NULL,
                                 phone VARCHAR(20),
                                 password_hash VARCHAR(255) NOT NULL,
                                 role VARCHAR(50) DEFAULT 'Admin',
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
