-- Create version table and set initial version mark
CREATE TABLE IF NOT EXISTS __theear_versions (version VARCHAR(255) NOT NULL, applied_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY (version));
INSERT INTO __theear_versions (version) VALUES ('initial') ON CONFLICT (version) DO NOTHING;

-- Tables for user profiles
CREATE TABLE IF NOT EXISTS user_profiles (user_id UUID NOT NULL, email TEXT NOT NULL, full_name TEXT NOT NULL, given_name TEXT NOT NULL, family_name TEXT NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(), last_seen_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(), PRIMARY KEY(user_id));
INSERT INTO user_profiles (user_id, email, full_name, given_name, family_name) VALUES (uuid_nil(), 'system@system', 'system', 'system', 'system') ON CONFLICT (user_id) DO NOTHING;

-- Initialize tables for permissions, permission groups, and oidc groups (which are mapped by accounts)
CREATE TABLE IF NOT EXISTS auth_functional_permissions (perm_id UUID NOT NULL DEFAULT gen_random_uuid(), perm_name VARCHAR(255) NOT NULL, perm_description TEXT NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), last_seen_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY(perm_id), UNIQUE(perm_name));

CREATE TABLE IF NOT EXISTS auth_functional_permissions_on_java_element (perm_id UUID REFERENCES auth_functional_permissions(perm_id) ON DELETE CASCADE NOT NULL, type_name TEXT NOT NULL, operation_name TEXT NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), last_seen_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY (perm_id, type_name, operation_name));

CREATE TABLE IF NOT EXISTS auth_functional_permission_groups (perm_group_id UUID NOT NULL DEFAULT gen_random_uuid(), perm_group_name VARCHAR(255) NOT NULL, perm_group_description TEXT NOT NULL DEFAULT '', created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), created_by UUID REFERENCES user_profiles(user_id) ON DELETE RESTRICT NOT NULL, PRIMARY KEY (perm_group_id, perm_group_name), UNIQUE (perm_group_id), UNIQUE (perm_group_name));

CREATE TABLE IF NOT EXISTS auth_functional_permission_groups_permissions (perm_group_id UUID REFERENCES auth_functional_permission_groups(perm_group_id) ON DELETE CASCADE NOT NULL, perm_id UUID REFERENCES auth_functional_permissions(perm_id) ON DELETE CASCADE NOT NULL, row_version BIGINT NOT NULL DEFAULT 1, created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), created_by UUID REFERENCES user_profiles(user_id) ON DELETE RESTRICT NOT NULL, deleted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT null, deleted_by UUID REFERENCES user_profiles(user_id) ON DELETE RESTRICT DEFAULT NULL, PRIMARY KEY (perm_group_id, perm_id, row_version));
-- FIXME: implement check if any groups are defined, and if not, request "setup"


CREATE TABLE IF NOT EXISTS auth_oidc_groups (group_id UUID NOT NULL DEFAULT gen_random_uuid(), oidc_issuer TEXT NOT NULL, oidc_group_name TEXT NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(), last_seen_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), PRIMARY KEY (group_id, oidc_issuer, oidc_group_name), UNIQUE (group_id), UNIQUE (oidc_issuer, oidc_group_name));

CREATE TABLE IF NOT EXISTS auth_functional_permission_group_oidc_group_map (perm_group_id UUID REFERENCES auth_functional_permission_groups(perm_group_id) ON DELETE CASCADE NOT NULL, group_id UUID REFERENCES auth_oidc_groups(group_id) ON DELETE CASCADE NOT NULL, row_version BIGINT NOT NULL DEFAULT 1, created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(), created_by UUID REFERENCES user_profiles(user_id) ON DELETE RESTRICT NOT NULL, deleted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT null, deleted_by UUID REFERENCES user_profiles(user_id) ON DELETE RESTRICT DEFAULT NULL, PRIMARY KEY (perm_group_id, group_id, row_version));

-- Tables for user accounts
CREATE TABLE IF NOT EXISTS user_account (user_id UUID NOT NULL DEFAULT gen_random_uuid(), oidc_issuer TEXT NOT NULL, oidc_subject TEXT NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(), last_seen_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(), PRIMARY KEY (oidc_issuer, oidc_subject));
  
-- Table to map users to OIDC groups; updated every time the user log's in
CREATE TABLE IF NOT EXISTS user_oidc_group_map (user_id UUID REFERENCES user_profiles(user_id) ON DELETE CASCADE NOT NULL, group_id UUID REFERENCES auth_oidc_groups(group_id) ON DELETE CASCADE NOT NULL, PRIMARY KEY (user_id, group_id));

