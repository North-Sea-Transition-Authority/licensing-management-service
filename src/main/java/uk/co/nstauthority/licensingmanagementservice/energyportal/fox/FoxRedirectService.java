package uk.co.nstauthority.licensingmanagementservice.energyportal.fox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.configuration.FoxRedirectConfiguration;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Service
public class FoxRedirectService {

  public static final String FOX_MODULE_AND_PARAMS = "%s/view-licence?LICENCE_TYPE=%s&LICENCE_NO=%s";

  private final FoxRedirectConfiguration foxRedirectConfiguration;

  @Autowired
  public FoxRedirectService(FoxRedirectConfiguration foxRedirectConfiguration) {
    this.foxRedirectConfiguration = foxRedirectConfiguration;
  }

  public String getViewPearsLicenceUrl(Licence licence) {
    var pearsRedirectUrl = foxRedirectConfiguration.pearsRedirectUrl();
    return FOX_MODULE_AND_PARAMS.formatted(pearsRedirectUrl, licence.getPrefix(), licence.getLicenceNumber());
  }
}
