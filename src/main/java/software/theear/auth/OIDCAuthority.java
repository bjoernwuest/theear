package software.theear.auth;

import java.time.Instant;

import org.springframework.security.core.GrantedAuthority;

public final class OIDCAuthority implements GrantedAuthority {
  private static final long serialVersionUID = 5208857787336399844L;
  public final String Identifier;
  public final String Issuer;
  public final String Groupname;
  public final Instant CreatedAt;
  private Instant LastSeenAt;
  public OIDCAuthority(String Issuer, String Groupname, Instant CreatedAt, Instant LastSeenAt) {
    this.Identifier = Issuer + "/" + Groupname;
    this.Issuer = Issuer;
    this.Groupname = Groupname;
    this.CreatedAt = CreatedAt;
    this.LastSeenAt = LastSeenAt;
  }
  public final Instant LastSeenAt() { return this.LastSeenAt; }
  @Override public String getAuthority() { return this.Identifier; }
}
