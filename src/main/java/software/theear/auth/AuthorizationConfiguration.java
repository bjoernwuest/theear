package software.theear.auth;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration of spring.security.oauth2.client subsection in application.yaml
 * 
 * @author bjoern.wuest@gmx.net
 */
@ConfigurationProperties(prefix = "spring.security.oauth2.client") public record RAuthorizationConfiguration(Map<String, Object> registration) {}
