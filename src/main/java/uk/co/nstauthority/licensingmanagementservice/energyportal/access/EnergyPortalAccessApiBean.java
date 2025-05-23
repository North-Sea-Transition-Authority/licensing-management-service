package uk.co.nstauthority.licensingmanagementservice.energyportal.access;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.co.fivium.digital.energyportalteamaccesslibrary.team.EnergyPortalAccessService;

@Configuration
public class EnergyPortalAccessApiBean {

  @Bean
  EnergyPortalAccessService energyPortalAccessService(EnergyPortalAccessApiConfiguration configuration) {
    return new EnergyPortalAccessService(configuration.baseUrl(), configuration.token());
  }
}
