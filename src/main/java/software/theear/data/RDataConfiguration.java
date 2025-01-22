package software.theear.data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.web.util.UriComponentsBuilder;

@ConfigurationProperties("database")
public record RDataConfiguration(
    @DefaultValue("localhost") String host,
    @DefaultValue("5432") int port,
    @DefaultValue("theear") String database,
    @DefaultValue("theear") String username,
    @DefaultValue("theear") String password,
    @DefaultValue("") String options,
    @DefaultValue("60") int connectionTimeout,
    @DefaultValue("60") int idleConnectionTimeout,
    @DefaultValue("60") int connectionKeepaliveInterval,
    @DefaultValue("1") int leakDetectionInterval,
    @DefaultValue("2") int maximumConnectionPoolSize,
    @DefaultValue("1") int minimumIdleConnections,
    @DefaultValue("60") int validConnectionDetectionTimeout,
    @DefaultValue("30") int maximumConnectionLifetime) {
	public RDataConfiguration {
		if (1 > port) { port = 5432; }
		options = UriComponentsBuilder.fromUriString(options).build().getQuery();
		if ((null != options) && !options.startsWith("?")) options = "?" + options;
	}
}
