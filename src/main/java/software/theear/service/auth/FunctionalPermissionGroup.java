package software.theear.service.auth;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import jakarta.annotation.Nonnull;

/** Group of functional permissions.
 * 
 * @author bjoern@liwuest.net
 */
public final class FunctionalPermissionGroup implements Comparable<FunctionalPermissionGroup> {
  private final AuthorizationService m_Repo;
  transient final Set<FunctionalPermission> _AssignedFunctionalPermissions = new TreeSet<>();
  /** Unique technical identifier of this functional permission group. */
  public final UUID FunctionalPermissionGroupID;
  private String m_Name;
  private String m_Description;
  /** The time this functional permission was created. */
  public final Instant CreatedAt;
  /** Returns collection of all functional permissions this group has been granted with. */
  transient public final Set<FunctionalPermission> AssignedFunctionalPermissions = Collections.unmodifiableSet(this._AssignedFunctionalPermissions);
  
  FunctionalPermissionGroup(@Nonnull AuthorizationService Repo, @Nonnull UUID FunctionalPermissionGroupID, @Nonnull String Name, @Nonnull String Description, @Nonnull Instant CreatedAt) {
    this.m_Repo = Repo;
    this.FunctionalPermissionGroupID = FunctionalPermissionGroupID;
    this.m_Name = Name;
    this.m_Description = Description;
    this.CreatedAt = CreatedAt;
  }
  
  /** Gets name of this functional permission group.
   * 
   * @return The name of this functional permission group.
   */
  public String Name() { return this.m_Name; }
  /** Sets name of this functional permission group.
   * 
   * @param NewName The new name of for this functional permission group.
   * @return This functional permission group for command chaining.
   */
  public FunctionalPermissionGroup Name(@Nonnull String NewName) {
    this.m_Name = NewName;
    this.m_Repo.update(this);
    return this;
  }
  void _Name(@Nonnull String NewName) { this.m_Name = NewName; }
  /** Gets descriptional text for this functional permission group.
   * 
   * @return The descriptional text for this functional permission group.
   */
  public String Description() { return this.m_Description; }
  /** Sets descriptional text for this functional permission group.
   * 
   * @param NewDescription The new description for this functional permission group.
   * @return This functional permission group for command chaining.
   */
  public FunctionalPermissionGroup Description(@Nonnull String NewDescription) {
    this.m_Description = NewDescription;
    this.m_Repo.update(this);
    return this;
  }
  void _Description(@Nonnull String NewDescription) { this.m_Description = NewDescription; }
  /** Grant given functional permission to this functional permission group.
   * 
   * @param PermissionToGrant The functional permission to grant.
   * @param UserID The user issuing the grant.
   */
  public void grant(@Nonnull FunctionalPermission PermissionToGrant, @Nonnull UUID UserID) { this.m_Repo.grant(PermissionToGrant, this.m_Repo.getFunctionalPermissionGroup(this.FunctionalPermissionGroupID).get(), UserID); }
  /** Revoke given functional permission from this functional permission group.
   * 
   * @param PermissionToRevoke The functional permission to revoke.
   * @param UserID The user issuing the revoke.
   */
  public void revoke(@Nonnull FunctionalPermission PermissionToRevoke, @Nonnull UUID UserID) { this.m_Repo.revoke(PermissionToRevoke, this.m_Repo.getFunctionalPermissionGroup(this.FunctionalPermissionGroupID).get(), UserID); }

  @Override public int compareTo(FunctionalPermissionGroup o) {
    if (null == o) return 1;
    return this.FunctionalPermissionGroupID.compareTo(o.FunctionalPermissionGroupID);
  }
  
  /** Return all groups/roles from any IdP assigned to this functional permission group.
   * 
   * @return All groups/roles from any IdP assigned to this functional permission group.
   */
  public LinkedList<OidcGroup> getOidcGroups() { return this.m_Repo.getOidcGroups(this.FunctionalPermissionGroupID); }
}
