package uk.co.nstauthority.licensingmanagementservice.configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceLogoutSuccessHandler;
import uk.co.nstauthority.licensingmanagementservice.authentication.saml.SamlResponseParser;
import uk.co.nstauthority.licensingmanagementservice.mvc.PostAuthenticationRequestMdcFilter;
import uk.co.nstauthority.licensingmanagementservice.mvc.RequestLogFilter;

@Configuration
public class WebSecurityConfiguration {

  public static final String IDP_ACCESS_GRANTED_AUTHORITY_NAME = "LMS_ACCESS_PRIVILEGE";

  private final SamlProperties samlProperties;
  private final SamlResponseParser samlResponseParser;
  private final ServiceLogoutSuccessHandler serviceLogoutSuccessHandler;
  private final RequestLogFilter requestLogFilter;
  private final PostAuthenticationRequestMdcFilter postAuthenticationRequestMdcFilter;

  @Autowired
  public WebSecurityConfiguration(
      SamlProperties samlProperties,
      SamlResponseParser samlResponseParser,
      ServiceLogoutSuccessHandler serviceLogoutSuccessHandler,
      RequestLogFilter requestLogFilter,
      PostAuthenticationRequestMdcFilter postAuthenticationRequestMdcFilter
  ) {
    this.samlProperties = samlProperties;
    this.samlResponseParser = samlResponseParser;
    this.serviceLogoutSuccessHandler = serviceLogoutSuccessHandler;
    this.requestLogFilter = requestLogFilter;
    this.postAuthenticationRequestMdcFilter = postAuthenticationRequestMdcFilter;
  }

  @Bean
  protected SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    var authenticationProvider = new OpenSaml4AuthenticationProvider();
    authenticationProvider.setResponseAuthenticationConverter(r -> samlResponseParser.parseSamlResponse(r.getResponse()));

    return httpSecurity
      .authorizeHttpRequests(http -> http
        .requestMatchers("/assets/**", "/error", "/actuator/health", "/api/v1/logout/*")
          .permitAll()
        //TODO xyz - add in GA filter once Fox team has been created
        //.requestMatchers("/**")
        //  .hasAuthority(IDP_ACCESS_GRANTED_AUTHORITY_NAME)
        .anyRequest()
          .authenticated()
      )
      .saml2Login(saml2 -> saml2.authenticationManager(new ProviderManager(authenticationProvider)))
      .logout(logout -> logout.logoutSuccessHandler(serviceLogoutSuccessHandler))
      .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/logout/*"))
      .addFilterBefore(requestLogFilter, SecurityContextHolderFilter.class)
      .addFilterAfter(postAuthenticationRequestMdcFilter, SecurityContextHolderFilter.class)
      .build();
  }

  @Bean
  protected RelyingPartyRegistrationRepository relyingPartyRegistrations(RelyingPartyRegistration registration) {
    return new InMemoryRelyingPartyRegistrationRepository(registration);
  }

  @Bean
  public RelyingPartyRegistration getRelyingPartyRegistration() throws CertificateException {

    var certificateStream = new ByteArrayInputStream(samlProperties.certificate().getBytes(StandardCharsets.UTF_8));

    X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
        .generateCertificate(certificateStream);

    Saml2X509Credential credential = Saml2X509Credential.verification(Objects.requireNonNull(certificate));

    return RelyingPartyRegistration
      .withRegistrationId(samlProperties.registrationId())
      .assertingPartyMetadata(party -> party
        .entityId(samlProperties.entityId())
        .singleSignOnServiceLocation(samlProperties.loginUrl())
        .singleSignOnServiceBinding(Saml2MessageBinding.POST)
        .wantAuthnRequestsSigned(false)
        .verificationX509Credentials(c -> c.add(credential))
      )
      .assertionConsumerServiceLocation(samlProperties.consumerServiceLocation())
      .build();
  }

}
