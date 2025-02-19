package software.theear.service.auth;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import jakarta.annotation.Nonnull;

import org.apache.wicket.util.lang.Objects;

/** Single functional permission.
 * 
 * Functional permissions are required to execute functions in the system.
 * 
 * @author bjoern@liwuest.net
 */
public final class FunctionalPermission implements Comparable<FunctionalPermission> {
  /** The Java operation where the functional permission was seen at. */
  public final static class FunctionalPermissionSource {
    /** The Java type this functional permission was seen at. */
    public final String TypeName;
    /** The name of the Java operation this functional permission was seen at. */
    public final String OperationName;
    /** The time the functional permission was first seen at this source. */
    public final Instant CreatedAt;
    /** The time the functional permission was last seen at this source. */
    public final Instant LastSeenAt;
    
    @Override public int hashCode() { return Objects.hashCode(this.TypeName, this.OperationName, this.CreatedAt, this.LastSeenAt); }
    @Override public boolean equals(Object obj) {
      if (obj instanceof FunctionalPermissionSource other) { return Objects.equal(this.TypeName, other.TypeName) && Objects.equal(this.OperationName, other.OperationName) && Objects.equal(this.CreatedAt, other.CreatedAt) && Objects.equal(this.LastSeenAt, other.LastSeenAt); }
      return super.equals(obj);
    }

    FunctionalPermissionSource(@Nonnull String TypeName, @Nonnull String OperationName, @Nonnull Instant CreatedAt, @Nonnull Instant LastSeenAt) {
      this.TypeName = TypeName;
      this.OperationName = OperationName;
      this.CreatedAt = CreatedAt;
      this.LastSeenAt = LastSeenAt;
    }
  }
  
  transient final Set<FunctionalPermissionGroup> _AssignedFunctionalPermissionGroups = new TreeSet<>();
  final Collection<FunctionalPermissionSource> _FunctionalPermissionSources = new LinkedHashSet<>();
  /** The technical identifier of this functional permission. */
  public final UUID FunctionalPermissionID;
  /** The name of the functional permission. */
  public final String PermissionName;
  /** Descriptional text for the functional permission as given by the coder. */
  public final String PermissionDescription;
  /** The time this functional permission was first seen in the source code. */
  public final Instant CreatedAt;
  /** The time this functional permission was last seen in the source code. Functional permissions are scanned during code loading. */
  public final Instant LastSeenAt;
  /** The Java methods where the functional permissions were detected. */
  public final Collection<FunctionalPermissionSource> FunctionalPermissionSources = Collections.unmodifiableCollection(this._FunctionalPermissionSources);
  /** The functional permission groups this functional permission is assigned to. */
  transient public final Set<FunctionalPermissionGroup> AssignedFunctionalPermissionGroups = Collections.unmodifiableSet(this._AssignedFunctionalPermissionGroups);
  
  FunctionalPermission(@Nonnull UUID FunctionalPermissionID, @Nonnull String PermissionName, @Nonnull String PermissionDescription, @Nonnull Instant CreatedAt, @Nonnull Instant LastSeenAt) {
    this.FunctionalPermissionID = FunctionalPermissionID;
    this.PermissionName = PermissionName;
    this.PermissionDescription = PermissionDescription;
    this.CreatedAt = CreatedAt;
    this.LastSeenAt = LastSeenAt;
  }

  @Override public int compareTo(FunctionalPermission o) {
    if (null == o) return 1;
    return this.FunctionalPermissionID.compareTo(o.FunctionalPermissionID);
  }
}
