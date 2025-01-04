-- Create version table and set initial version mark
CREATE TABLE IF NOT EXISTS __theear_versions (version VARCHAR(255) NOT NULL, applied_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY (version));
INSERT INTO __theear_versions (version) VALUES ('initial') ON CONFLICT (version) DO NOTHING;

-- Initialize tables for permissions and roles
CREATE TABLE IF NOT EXISTS auth_functional_permissions (perm_id UUID NOT NULL DEFAULT gen_random_uuid(), perm_name VARCHAR(255) NOT NULL, perm_action VARCHAR(255) NOT NULL DEFAULT '', perm_description TEXT NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), last_seen_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY(perm_id), UNIQUE(perm_name, perm_action));
CREATE TABLE IF NOT EXISTS auth_functional_permissions_on_java_element (perm_id UUID REFERENCES auth_functional_permissions(perm_id) ON DELETE CASCADE NOT NULL, type_name TEXT NOT NULL, operation_name TEXT NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), last_seen_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY (perm_id, type_name, operation_name));
CREATE TABLE IF NOT EXISTS auth_functional_permission_groups (perm_group_id UUID NOT NULL DEFAULT gen_random_uuid(), perm_group_name VARCHAR(255) NOT NULL, perm_group_description TEXT NOT NULL DEFAULT '', created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY (perm_group_id, perm_group_name), UNIQUE (perm_group_id), UNIQUE (perm_group_name));
-- FIXME: implement check if any groups are defined, and if not, request "setup"
CREATE TABLE IF NOT EXISTS auth_functional_permission_groups_permissions (perm_group_id UUID REFERENCES auth_functional_permission_groups(perm_group_id) ON DELETE CASCADE NOT NULL, perm_id UUID REFERENCES auth_functional_permissions(perm_id) ON DELETE CASCADE NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY (perm_group_id, perm_id));
CREATE TABLE IF NOT EXISTS auth_oidc_groups (group_id UUID NOT NULL DEFAULT gen_random_uuid(), oidc_issuer TEXT NOT NULL, oidc_group_name TEXT NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(), last_seen_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY (group_id, oidc_issuer, oidc_group_name), UNIQUE (group_id), UNIQUE (oidc_issuer, oidc_group_name));
CREATE TABLE IF NOT EXISTS auth_functional_permission_group_oidc_group_map (perm_group_id UUID REFERENCES auth_functional_permission_groups(perm_group_id) ON DELETE CASCADE NOT NULL, group_id UUID REFERENCES auth_oidc_groups(group_id) ON DELETE CASCADE NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY (perm_group_id, group_id));

-- Tables for users and profiles
CREATE TABLE IF NOT EXISTS user_account (user_id UUID NOT NULL DEFAULT gen_random_uuid(), oidc_issuer TEXT NOT NULL, oidc_subject TEXT NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(), last_seen_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(), PRIMARY KEY (oidc_issuer, oidc_subject));
CREATE TABLE IF NOT EXISTS user_profile (user_id UUID NOT NULL, email TEXT NOT NULL, full_name TEXT NOT NULL, given_name TEXT NOT NULL, family_name TEXT NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(), last_seen_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(), PRIMARY KEY(user_id));

-- Table to map users to OIDC groups; updated every time the user log's in
CREATE TABLE IF NOT EXISTS user_oidc_group_map (user_id UUID REFERENCES user_profile(user_id) ON DELETE CASCADE NOT NULL, group_id UUID REFERENCES auth_oidc_groups(group_id) ON DELETE CASCADE NOT NULL, PRIMARY KEY (user_id, group_id));
