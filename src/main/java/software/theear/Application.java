package software.theear;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {
  public final static void main(String[] Args) {
    new SpringApplicationBuilder().sources(Application.class).run(Args);
  }
}
