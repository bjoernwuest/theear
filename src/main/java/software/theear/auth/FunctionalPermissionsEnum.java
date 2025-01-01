package software.theear.auth;

import jakarta.annotation.Nonnull;

public enum FunctionalPermissionsEnum {
  Hello("First test"),
  World("Second test"),
  Out("Third test");
  
  final String description;
  private FunctionalPermissionsEnum(@Nonnull String Description) { this.description = Description; }
}
