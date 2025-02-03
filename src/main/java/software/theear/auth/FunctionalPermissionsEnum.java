package software.theear.auth;

import jakarta.annotation.Nonnull;

/** Constants for functional permissions.
 * 
 * Use this enum to assign functional permission.
 * 
 * @author bjoern@liwuest.net
 */
public enum FunctionalPermissionsEnum {
  Hello("First test"),
  World("Second test"),
  Out("Third test");
  
  final String description;
  private FunctionalPermissionsEnum(@Nonnull String Description) { this.description = Description; }
}
