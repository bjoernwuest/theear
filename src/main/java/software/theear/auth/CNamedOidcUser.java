package software.theear.auth;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

public final class CNamedOidcUser extends DefaultOidcUser {
  private static final long serialVersionUID = -4671186815253070087L;
  private final String m_UserName;
  private final boolean m_IsRoot;
  
  // FIXME: actually resolve "groups" as given in the authorities to permissions
  private Collection<String> m_GetPermissions() { return this.getAuthorities().stream().map(a -> a.getAuthority()).distinct().collect(Collectors.toSet()); }
  
  public CNamedOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken, OidcUserInfo userInfo, String UserName, boolean IsRoot) {
    super(authorities, idToken, userInfo, "email");
    this.m_UserName = UserName;
    this.m_IsRoot = IsRoot;
  }
  
  public String userName() { return this.m_UserName; }
  
  public boolean isRoot() { return this.m_IsRoot; }
  
  public boolean hasAllPermissions(FunctionalPermissions[] Permissions) {
    if (this.m_IsRoot) return true; 
    return this.m_GetPermissions().containsAll(Arrays.asList(Permissions).stream().map(p -> p.name()).collect(Collectors.toList()));
  }
  public Roles getRoles() {
    Roles result = new Roles();
    result.addAll(this.m_GetPermissions());
    return result;
  }
}
