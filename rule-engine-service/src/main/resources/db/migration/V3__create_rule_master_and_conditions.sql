CREATE TABLE rule_master (
                             id BIGSERIAL PRIMARY KEY,

                             rule_code VARCHAR(100) NOT NULL UNIQUE,
                             rule_name VARCHAR(200) NOT NULL,
                             rule_type VARCHAR(100) NOT NULL,

                             description VARCHAR(1000),

                             decision VARCHAR(50) NOT NULL,
                             priority INTEGER NOT NULL DEFAULT 100,

                             expression VARCHAR(2000),

                             is_active BOOLEAN NOT NULL DEFAULT TRUE,

                             created_by VARCHAR(100),
                             updated_by VARCHAR(100),

                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rule_condition (
                                id BIGSERIAL PRIMARY KEY,

                                rule_id BIGINT NOT NULL,
                                field_id BIGINT NOT NULL,
                                operator_id BIGINT NOT NULL,

                                condition_value VARCHAR(1000),

                                sequence_order INTEGER NOT NULL DEFAULT 1,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_rule_condition_rule
                                    FOREIGN KEY (rule_id) REFERENCES rule_master(id) ON DELETE CASCADE,

                                CONSTRAINT fk_rule_condition_field
                                    FOREIGN KEY (field_id) REFERENCES field_master(id),

                                CONSTRAINT fk_rule_condition_operator
                                    FOREIGN KEY (operator_id) REFERENCES conditional_operator(id)
);

CREATE INDEX idx_rule_master_type_active_priority
    ON rule_master(rule_type, is_active, priority);

CREATE INDEX idx_rule_condition_rule_sequence
    ON rule_condition(rule_id, sequence_order);