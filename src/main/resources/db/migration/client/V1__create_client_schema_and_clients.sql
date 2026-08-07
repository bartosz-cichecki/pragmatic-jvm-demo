CREATE SCHEMA client;

CREATE TABLE client.clients (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT clients_name_not_blank CHECK (length(btrim(name)) > 0),
    CONSTRAINT clients_updated_after_creation CHECK (updated_at >= created_at),
    CONSTRAINT clients_deactivation_after_creation CHECK (deactivated_at IS NULL OR deactivated_at >= created_at)
);
