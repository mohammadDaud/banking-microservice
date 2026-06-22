INSERT INTO field_master (
    field_name,
    display_name,
    data_type,
    description,
    created_by
)
VALUES
    ('amount', 'Transaction Amount', 'NUMBER',
     'Transaction amount used for approval rules', 'SYSTEM'),

    ('bankType', 'Bank Type', 'STRING',
     'INTERNAL or EXTERNAL beneficiary bank', 'SYSTEM'),

    ('requestedLimit', 'Requested Transaction Limit', 'NUMBER',
     'Requested account transaction limit', 'SYSTEM'),

    ('currentLimit', 'Current Transaction Limit', 'NUMBER',
     'Current account transaction limit', 'SYSTEM'),

    ('kycStatus', 'KYC Status', 'STRING',
     'Current KYC status of customer', 'SYSTEM'),

    ('customerType', 'Customer Type', 'STRING',
     'Customer category such as REGULAR, PREMIUM, CORPORATE', 'SYSTEM');

INSERT INTO conditional_operator (
    short_name,
    symbol,
    display_name,
    description,
    category,
    created_by
)
VALUES
    ('EQ', '=', 'Equals', 'Checks if values are equal', 'COMPARISON', 'SYSTEM'),
    ('NEQ', '!=', 'Not Equals', 'Checks if values are not equal', 'COMPARISON', 'SYSTEM'),
    ('GT', '>', 'Greater Than', 'Checks if left value is greater', 'COMPARISON', 'SYSTEM'),
    ('GTE', '>=', 'Greater Than or Equal', 'Checks if left value is greater than or equal', 'COMPARISON', 'SYSTEM'),
    ('LT', '<', 'Less Than', 'Checks if left value is less', 'COMPARISON', 'SYSTEM'),
    ('LTE', '<=', 'Less Than or Equal', 'Checks if left value is less than or equal', 'COMPARISON', 'SYSTEM'),
    ('IN', 'IN', 'In', 'Checks if value exists in a list', 'COLLECTION', 'SYSTEM'),
    ('NOT_IN', 'NOT IN', 'Not In', 'Checks if value does not exist in a list', 'COLLECTION', 'SYSTEM'),
    ('CONTAINS', 'CONTAINS', 'Contains', 'Checks if text contains another text', 'STRING', 'SYSTEM');

INSERT INTO field_operator_mapping (field_id, operator_id)
SELECT f.id, o.id
FROM field_master f
         JOIN conditional_operator o
              ON o.short_name IN ('EQ', 'NEQ', 'GT', 'GTE', 'LT', 'LTE')
WHERE f.field_name IN ('amount', 'requestedLimit', 'currentLimit');

INSERT INTO field_operator_mapping (field_id, operator_id)
SELECT f.id, o.id
FROM field_master f
         JOIN conditional_operator o
              ON o.short_name IN ('EQ', 'NEQ', 'IN', 'NOT_IN', 'CONTAINS')
WHERE f.field_name IN ('bankType', 'kycStatus', 'customerType');