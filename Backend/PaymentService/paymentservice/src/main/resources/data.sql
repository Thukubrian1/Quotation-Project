-- Insert sample data for testing (optional)
-- You can remove this if you don't want sample data

INSERT INTO payment_transactions
(merchant_request_id, checkout_request_id, phone_number, amount, account_reference,
 transaction_description, status, transaction_date, created_at, updated_at)
VALUES
    ('TEST001', 'ws_CO_test123', '254741819799', 100.00, 'ACC001',
     'Test payment', 'COMPLETED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TEST002', 'ws_CO_test456', '254741819800', 50.00, 'ACC002',
     'Another test payment', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);