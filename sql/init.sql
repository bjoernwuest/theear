-- Create version table and set initial version mark
CREATE TABLE IF NOT EXISTS __theear_versions (version VARCHAR(255) NOT NULL, applied_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY (version));
INSERT INTO __theear_versions (version) VALUES ('initial') ON CONFLICT (version) DO NOTHING;

-- Initialize tables for permissions and roles
CREATE TABLE IF NOT EXISTS auth_functional_permissions (perm_id UUID NOT NULL DEFAULT gen_random_uuid(), perm_name VARCHAR(255) NOT NULL, perm_description TEXT NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), last_seen_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY(perm_id), UNIQUE(perm_name));
CREATE TABLE IF NOT EXISTS auth_functional_permission_usage (perm_id UUID REFERENCES auth_functional_permissions(perm_id) ON DELETE RESTRICT NOT NULL, used_at_operation TEXT NOT NULL, used_at_type TEXT NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), last_seen_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY (perm_id, used_at_operation, used_at_type));
