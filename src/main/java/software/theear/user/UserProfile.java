package software.theear.user;

import java.time.Instant;
import java.util.UUID;

public record UserProfile(UUID UserID, String Email, String FullName, String GivenName, String FamilyName, Instant CreatedAt, Instant LastSeenAt) {

}
