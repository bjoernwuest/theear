package software.theear.user;

import org.springframework.stereotype.Component;

import software.theear.event.SimpleEventbus;

@Component public final class NewUserEventbus extends SimpleEventbus<UserProfile> {}
