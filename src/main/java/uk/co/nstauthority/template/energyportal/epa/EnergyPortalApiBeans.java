package uk.co.nstauthority.template.energyportal.epa;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.co.fivium.energyportalapi.client.EnergyPortal;
import uk.co.fivium.energyportalapi.client.field.FieldApi;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.nstauthority.template.correlationid.CorrelationIdUtil;

@Configuration
public class EnergyPortalApiBeans {

  @Bean
  EnergyPortal energyPortal(
      EnergyPortalApiConfig energyPortalApiConfig,
      EpaRequestHandler epaRequestHandler
  ) {
    return EnergyPortal.customConfiguration(
        energyPortalApiConfig.url(),
        energyPortalApiConfig.preSharedKey(),
        EnergyPortal.DEFAULT_REQUEST_TIMEOUT_SECONDS,
        CorrelationIdUtil::getLogCorrelationId,
        epaRequestHandler
    );
  }

  @Bean
  public FieldApi fieldApi(EnergyPortal energyPortal) {
    return new FieldApi(energyPortal);
  }

  @Bean
  public UserApi userApi(EnergyPortal energyPortal) {
    return new UserApi(energyPortal);
  }

  @Bean
  public OrganisationApi organisationApi(EnergyPortal energyPortal) {
    return new OrganisationApi(energyPortal);
  }
}
