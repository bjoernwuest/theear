Register application at Microsoft Entra ID for login

1. Go to Microsoft Azure portal
2. Register new Enterprise Application
    1. Give any name, e.g. "Enterprise Architecture Repository"
    2. Select supported account types (recommended: only accounts in my organization)
    3. Enter redirection URI, i.e. https://<your-domain-name-for-the-application>:<the-port-the-application-is-exposed-at>/login/oauth2/code/<the-registration-given-in-application.yaml>
    4. select "Access token" and "ID-token"
    5. Permit public client flows
3. Go to token configuration
    1. Add group claim
        * Security groups
    2. Add optional claims from ID token type:
        * email
        * family_name
        * given_name
4. Go to certificates and secrets
    1. Create new secret client key
    2. Copy the secret value and add in application.yaml to spring.security.oauth2.client.registration.<your-registration-name>.client-secret
