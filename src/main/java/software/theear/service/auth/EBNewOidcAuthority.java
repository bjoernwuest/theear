package software.theear.service.auth;

import software.theear.util.SimpleEventbus;

/** Event bus for new OIDC authorities (i.e. groups/roles defined in IDP). 
 * 
 * @author bjoern@liwuest.net
 */
public final class EBNewOidcAuthority extends SimpleEventbus<OidcAuthority>{
  private EBNewOidcAuthority() {}
  public final static EBNewOidcAuthority GET = new EBNewOidcAuthority();
}
