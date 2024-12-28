package software.theear.auth;

import jakarta.annotation.Nonnull;

public enum FunctionalPermissions {
  Hello("First test"),
  World("Second test"),
  Out("Third test");
  
  final String description;
  private FunctionalPermissions(@Nonnull String Description) { this.description = Description; }
}
