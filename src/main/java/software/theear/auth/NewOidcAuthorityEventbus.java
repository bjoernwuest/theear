package software.theear.auth;

import org.springframework.stereotype.Component;

import software.theear.event.SimpleEventbus;

/** Event bus for new OIDC authorities (i.e. groups/roles defined in IDP). 
 * 
 * @author bjoern@liwuest.net
 */
@Component public final class NewOidcAuthorityEventbus extends SimpleEventbus<OidcAuthority>{}
