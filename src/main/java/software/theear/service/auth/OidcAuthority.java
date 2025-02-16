package software.theear.service.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;

/** Representation of an authority, i.e. a group/role granted by an IDP to a user.
 * 
 * @author bjoern@liwuest.net
 */
public final class OidcAuthority implements GrantedAuthority {
  private static final long serialVersionUID = 5208857787336399844L;
  /** The issuer, i.e. IDP, where this authority is from. */
  public final String Issuer;
  /** The UUID of the authority as provided by the IDP. */
  public final UUID AuthorityID;
  /** The actual name of the authority. */
  public final String Authorityname;
  /** When this authority was created, i.e. first detected by the application. */
  public final Instant CreatedAt;
  /** When this authority was last seen with any user logging in. This can be a hint on expired authorities no longer in use. */
  public final Instant LastSeenAt;
  public OidcAuthority(String Issuer, UUID GroupID, String Groupname, Instant CreatedAt, Instant LastSeenAt) {
    this.AuthorityID = GroupID;
    this.Issuer = Issuer;
    this.Authorityname = Groupname;
    this.CreatedAt = CreatedAt;
    this.LastSeenAt = LastSeenAt;
  }
  @Override public String getAuthority() { return this.AuthorityID.toString(); }
}
