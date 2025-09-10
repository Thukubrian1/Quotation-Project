-- Create payment_transactions table
CREATE TABLE IF NOT EXISTS payment_transactions (
                                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                    merchant_request_id VARCHAR(255),
    checkout_request_id VARCHAR(255),
    phone_number VARCHAR(20) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    account_reference VARCHAR(255) NOT NULL,
    transaction_description VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    mpesa_receipt_number VARCHAR(255),
    transaction_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_merchant_request_id ON payment_transactions(merchant_request_id);
CREATE INDEX IF NOT EXISTS idx_checkout_request_id ON payment_transactions(checkout_request_id);
CREATE INDEX IF NOT EXISTS idx_phone_number ON payment_transactions(phone_number);
CREATE INDEX IF NOT EXISTS idx_status ON payment_transactions(status);
CREATE INDEX IF NOT EXISTS idx_created_at ON payment_transactions(created_at);