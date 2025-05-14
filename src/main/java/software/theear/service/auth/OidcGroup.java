package software.theear.service.auth;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import org.apache.wicket.util.lang.Objects;

import jakarta.annotation.Nonnull;

public final class OidcGroup implements Comparable<OidcGroup>, Serializable {
  private static final long serialVersionUID = 3250945274785743435L;
  public final UUID GroupID;
  public final String Issuer;
  public final String GroupName;
  public final Instant CreatedAt;
  
  OidcGroup(@Nonnull UUID GroupID, @Nonnull String Issuer, @Nonnull String GroupName, @Nonnull Instant CreatedAt) {
    this.GroupID = GroupID;
    this.Issuer = Issuer;
    this.GroupName = GroupName;
    this.CreatedAt = CreatedAt;
  }
  
  @Override public int hashCode() { return Objects.hashCode(this.GroupID, this.Issuer, this.GroupName, this.CreatedAt); }
  @Override public boolean equals(Object obj) {
    if (obj instanceof OidcGroup other) { return Objects.equal(this.GroupID, other.GroupID) && Objects.equal(this.Issuer, other.Issuer) && Objects.equal(this.GroupName, other.GroupName) && Objects.equal(this.CreatedAt, other.CreatedAt); }
    return super.equals(obj);
  }
  @Override public int compareTo(OidcGroup o) {
    if (null == o) return 1;
    return this.GroupID.compareTo(o.GroupID);
  }
}
