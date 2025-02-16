package software.theear.service.user;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import software.theear.service.data.DatabaseService;
import software.theear.util.Reference;
import software.theear.util.UnsettableHardReference;

/** Repository for user profiles.
 * 
 * @author bjoern@liwuest.net
 */
@Service public final class UserprofileRepository {
  private final static Logger log = LoggerFactory.getLogger(UserprofileRepository.class);
  /** Singleton-pattern implementation */
  private static UserprofileRepository m_THIS = null;
  /** Gets instance of this service. This function may block until the instance is available. */
  public static UserprofileRepository getInstance() {
    // Wait for this being initialized
    while (null == m_THIS) {
      log.trace("Wait for Spring framework to construct service.");
      try { Thread.sleep(100); } catch (InterruptedException WakeUp) {}
    }
    return m_THIS;
  }
  
  private final Map<UUID, Reference<Userprofile>> m_LoadedProfiles = new TreeMap<>();
  
  UserprofileRepository() throws SQLException {
    // FIXME: rework similar to AuthorizationService
    m_THIS = this;
    DatabaseService.scheduleTransaction((Conn) -> {
      // Load all user profiles into memory
      try (Statement stmt = Conn.createStatement(); ResultSet rSet = stmt.executeQuery("SELECT user_id, email, full_name, given_name, family_name, created_at, last_seen_at FROM user_profiles")) {
        while (rSet.next()) {
          this.m_LoadedProfiles.put(rSet.getObject(1, UUID.class), new UnsettableHardReference<>(new Userprofile(rSet.getObject(1, UUID.class), rSet.getString(2), rSet.getString(3), rSet.getString(4), rSet.getString(5), rSet.getTime(6).toInstant(), rSet.getTime(7).toInstant())));
        }
      }
      return null;
    });
  }
  
  /** Create (if required) and return user profile with given data.
   * 
   * If the user profile does not exist, it is persisted in the data base and then returned. Otherwise, the {@link Userprofile#LastSeenAt()} information is updated.
   * 
   * @param UserID The ID of the user, as defined by the account.
   * @param Email The email of the user.
   * @param FullName The full name of the user.
   * @param GivenName The given name of the user.
   * @param FamilyName The family name of the user.
   * @return The user's profile.
   * @throws SQLException If there is any issue with the data base.
   */
  public Optional<Reference<Userprofile>> createAndReturn(@Nonnull UUID UserID, @Nonnull String Email, @Nonnull String FullName, @Nonnull String GivenName, @Nonnull String FamilyName) {
    try {
      DatabaseService.scheduleTransaction((Conn) -> {
        // UPSERT entry in data base
        try (PreparedStatement pStmt = Conn.prepareStatement("INSERT INTO user_profiles (user_id, email, full_name, given_name, family_name) VALUES (?, ?, ?, ?, ?) ON CONFLICT (user_id) DO UPDATE SET full_name = ?, given_name = ?, family_name = ?, last_seen_at = now() RETURNING created_at, last_seen_at")) {
          pStmt.setObject(1, UserID);
          pStmt.setString(2, Email);
          pStmt.setString(3, FullName);
          pStmt.setString(4, GivenName);
          pStmt.setString(5, FamilyName);
          pStmt.setString(6, FullName);
          pStmt.setString(7, GivenName);
          pStmt.setString(8, FamilyName);
          try (ResultSet rSet = pStmt.executeQuery()) {
            rSet.next();
            if (this.m_LoadedProfiles.containsKey(UserID)) {
              // Update memorized userprofile
              this.m_LoadedProfiles.get(UserID).get()._LastSeenAt(rSet.getTimestamp(2).toInstant());
            } else {
              // Create user profile object
              this.m_LoadedProfiles.put(UserID, new UnsettableHardReference<>(new Userprofile(UserID, Email, FullName, GivenName, FamilyName, rSet.getTimestamp(1).toInstant(), rSet.getTimestamp(2).toInstant())));
              // Announce new user in eventbus
              NewUserEventbus.GET.send(UserID);
            }
          }
        }
        return null;
      }).get();
    } catch (ExecutionException Ex) { log.warn("Could not create or update user profile in data base. See nested exception for details. Application continues.", Ex); }
    catch (InterruptedException Ignore) { /* ignore for waiting */ }
    // Return entry
    return Optional.of(this.m_LoadedProfiles.get(UserID));
  }
  
  public Optional<Reference<Userprofile>> findOne(UUID UserID) {
    if ((null != UserID) && !this.m_LoadedProfiles.containsKey(UserID)) {
      try {
        this.m_LoadedProfiles.put(UserID, new UnsettableHardReference<>(DatabaseService.scheduleTransaction((Conn) -> {
          try (PreparedStatement pStmt = Conn.prepareStatement("SELECT email, full_name, given_name, family_name, created_at, last_seen_at FROM user_profiles WHERE user_id = ?")) {
            pStmt.setObject(1, UserID);
            try (ResultSet rSet = pStmt.executeQuery()) {
              if (rSet.next()) {
                return new Userprofile(UserID, rSet.getString(1), rSet.getString(2), rSet.getString(3), rSet.getString(4), rSet.getTime(5).toInstant(), rSet.getTime(6).toInstant());
              }
            }
          }
          return null;
        }).get()));
      } catch (ExecutionException Ex) { log.warn("Failed to get user profile from data base. See nested exception for details. Application continues.", Ex); }
      catch (InterruptedException Ignore) { /* ignore for waiting */ }
    }
    return Optional.of(this.m_LoadedProfiles.get(UserID));
  }
  
  public Iterable<Reference<Userprofile>> findAll() { return this.m_LoadedProfiles.values(); }
}
