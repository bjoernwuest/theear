package software.theear.service.user;

import java.util.UUID;

import software.theear.util.SimpleEventbus;

/** Event bus triggered when new users are created in the system.
 * 
 * The signalled event is the ID of the user which can then be resolved using {@link UserprofileRepository}.
 * 
 * @author bjoern@liwuest.net
 */
public final class NewUserEventbus extends SimpleEventbus<UUID> {
  private NewUserEventbus() {}
  public final static NewUserEventbus GET = new NewUserEventbus();
}
