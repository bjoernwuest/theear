package software.theear.service.user;

import java.time.Instant;
import java.util.UUID;

import javax.annotation.Nonnull;

public final class Userprofile {
  public final UUID UserID;
  public final String Email;
  public final String Fullname;
  public final String GivenName;
  public final String FamilyName;
  public final Instant CreatedAt;
  private Instant m_LastSeenAt;
  
  Userprofile(@Nonnull UUID UserID, @Nonnull String Email, @Nonnull String FullName, @Nonnull String GivenName, @Nonnull String FamilyName, @Nonnull Instant CreatedAt, @Nonnull Instant LastSeenAt) {
    this.UserID = UserID;
    this.Email = Email;
    this.Fullname = FullName;
    this.GivenName = GivenName;
    this.FamilyName = FamilyName;
    this.CreatedAt = CreatedAt;
    this.m_LastSeenAt = LastSeenAt;
  }
  
  public Instant LastSeenAt() { return this.m_LastSeenAt; }
  Userprofile _LastSeenAt(@Nonnull Instant LastSeenAt) { this.m_LastSeenAt = LastSeenAt; return this; }
}
