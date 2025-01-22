package software.theear.user;

import org.springframework.stereotype.Component;

import software.theear.event.SimpleEventbus;

/** Event bus triggered when new users are created in the system.
 * 
 * @author bjoern@liwuest.net
 */
@Component public final class NewUserEventbus extends SimpleEventbus<UserProfile> {}
