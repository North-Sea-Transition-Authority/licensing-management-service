package uk.co.nstauthority.licensingmanagementservice.localdevonly;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@Profile({"development"})
public class DevelopmentConfiguration {

  /**
   * Disable the SameSite cookie policy when running in development mode, as the SAML IdP (Fox) will most likely be running
   * on a different domain to the application (i.e. localhost and itportal.dev.fivium.co.uk)
   * This overrides the default 'lax' setting using in production.
   * @return CookieSerializer bean
   */
  @Bean
  public CookieSerializer cookieSerializer() {
    DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
    cookieSerializer.setSameSite(null);
    return cookieSerializer;
  }
}
