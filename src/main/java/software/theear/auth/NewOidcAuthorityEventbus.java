package software.theear.auth;

import org.springframework.stereotype.Component;

import software.theear.event.SimpleEventbus;

@Component public final class NewOidcAuthorityEventbus extends SimpleEventbus<OidcAuthority>{}
