CREATE TABLE field_master (
                              id BIGSERIAL PRIMARY KEY,
                              field_name VARCHAR(100) NOT NULL UNIQUE,
                              display_name VARCHAR(150) NOT NULL,
                              data_type VARCHAR(30) NOT NULL,
                              description VARCHAR(500),
                              is_active BOOLEAN NOT NULL DEFAULT TRUE,
                              created_by VARCHAR(100),
                              updated_by VARCHAR(100),
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE conditional_operator (
                                      id BIGSERIAL PRIMARY KEY,
                                      short_name VARCHAR(50) NOT NULL UNIQUE,
                                      symbol VARCHAR(30) NOT NULL,
                                      display_name VARCHAR(100) NOT NULL,
                                      description VARCHAR(500),
                                      category VARCHAR(30) NOT NULL,
                                      is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                      created_by VARCHAR(100),
                                      updated_by VARCHAR(100),
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE field_operator_mapping (
                                        id BIGSERIAL PRIMARY KEY,
                                        field_id BIGINT NOT NULL,
                                        operator_id BIGINT NOT NULL,
                                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                        CONSTRAINT fk_field_operator_mapping_field
                                            FOREIGN KEY (field_id) REFERENCES field_master(id) ON DELETE CASCADE,

                                        CONSTRAINT fk_field_operator_mapping_operator
                                            FOREIGN KEY (operator_id) REFERENCES conditional_operator(id) ON DELETE CASCADE,

                                        CONSTRAINT uk_field_operator_mapping UNIQUE (field_id, operator_id)
);

CREATE INDEX idx_field_master_active
    ON field_master(is_active);

CREATE INDEX idx_conditional_operator_active
    ON conditional_operator(is_active);