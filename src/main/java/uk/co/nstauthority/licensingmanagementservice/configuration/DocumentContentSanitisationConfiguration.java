package uk.co.nstauthority.licensingmanagementservice.configuration;

import org.jsoup.safety.Safelist;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentContentSanitisationConfiguration {

  @Bean
  public Safelist documentLibraryContentSanitisationSafelist() {
    return Safelist.basic()
        .addAttributes("p", "style");
  }
}
