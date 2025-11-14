-- V1__init_tenant_schema.sql

-- Create users table (combined version with all fields)
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(50),
    password VARCHAR(255),
    role VARCHAR(50) NOT NULL,                -- ADMIN, DOCTOR, NURSE, PHARMACIST, LAB TECHNICIAN, STAFF, PATIENT
    profile_picture VARCHAR(250),
    status VARCHAR(20) DEFAULT 'PENDING',-- PENDING, ACTIVE, REJECTED
    auth_user_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Create doctors table
CREATE TABLE IF NOT EXISTS doctors (
                                       id SERIAL PRIMARY KEY,
                                       user_id INT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    specialization VARCHAR(150),
    department VARCHAR(100),
    license_number VARCHAR(100) UNIQUE NOT NULL,
    license_issue_date DATE NOT NULL,
    license_expiry_date DATE NOT NULL,
    license_authority VARCHAR(150),
    availability JSONB,  -- e.g. { "monday": ["08:00-12:00"], "tuesday": [] }
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Create laboratory scientists table
CREATE TABLE IF NOT EXISTS laboratory_scientists (
                                                     id SERIAL PRIMARY KEY,
                                                     user_id INT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    license_number VARCHAR(100) UNIQUE NOT NULL,
    license_issue_date DATE NOT NULL,
    license_expiry_date DATE NOT NULL,
    specialization VARCHAR(150),          -- e.g. Hematology, Microbiology, Immunology, Chemical Pathology
    department VARCHAR(100),
    license_authority VARCHAR(150),
    years_of_experience INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Create nurses table
CREATE TABLE IF NOT EXISTS nurses (
                                      id SERIAL PRIMARY KEY,
                                      user_id INT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    license_number VARCHAR(100) UNIQUE NOT NULL,
    license_issue_date DATE NOT NULL,
    license_expiry_date DATE NOT NULL,
    specialization VARCHAR(150),          -- e.g. Registered Nurse, Midwife, Pediatric Nurse
    department VARCHAR(100),
    shift_hours VARCHAR(100),
    years_of_experience INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Create pharmacists table
CREATE TABLE IF NOT EXISTS pharmacists (
                                           id SERIAL PRIMARY KEY,
                                           user_id INT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    license_number VARCHAR(50) UNIQUE NOT NULL,
    license_issue_date DATE NOT NULL,
    license_expiry_date DATE NOT NULL,
    specialization VARCHAR(150),
    department VARCHAR(100),
    years_of_experience INT,
    license_authority VARCHAR(150),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Create hospital staff table
CREATE TABLE IF NOT EXISTS hospital_staff (
                                              id SERIAL PRIMARY KEY,
                                              user_id INT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    department VARCHAR(100),
    position VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Create patients table
CREATE TABLE IF NOT EXISTS patients (
                                        id SERIAL PRIMARY KEY,
                                        user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date_of_birth DATE,
    hospital_number VARCHAR(50) UNIQUE NOT NULL,
    gender VARCHAR(20),
    blood_group VARCHAR(10),
    genotype VARCHAR(10),
    marital_status VARCHAR(20),
    occupation VARCHAR(150),
    country VARCHAR(100),
    state VARCHAR(100),
    city VARCHAR(100),
    address_line TEXT,
    next_of_kin_name VARCHAR(150),
    next_of_kin_relationship VARCHAR(100),
    next_of_kin_phone VARCHAR(50),
    emergency_contact_name VARCHAR(150),
    emergency_contact_phone VARCHAR(50),
    allergies TEXT,
    chronic_conditions TEXT,
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approval_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_doctors_user_id ON doctors(user_id);
CREATE INDEX IF NOT EXISTS idx_nurses_user_id ON nurses(user_id);
CREATE INDEX IF NOT EXISTS idx_pharmacists_user_id ON pharmacists(user_id);
CREATE INDEX IF NOT EXISTS idx_patients_user_id ON patients(user_id);
CREATE INDEX IF NOT EXISTS idx_patients_hospital_number ON patients(hospital_number);
CREATE INDEX IF NOT EXISTS idx_lab_scientists_user_id ON laboratory_scientists(user_id);