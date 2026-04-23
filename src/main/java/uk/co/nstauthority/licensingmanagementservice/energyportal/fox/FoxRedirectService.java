package uk.co.nstauthority.licensingmanagementservice.energyportal.fox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.configuration.FoxRedirectConfiguration;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Service
public class FoxRedirectService {

  public static final String VIEW_LICENCE_URL = "%s/view-licence?LICENCE_REF=%s";

  private final FoxRedirectConfiguration foxRedirectConfiguration;

  @Autowired
  public FoxRedirectService(FoxRedirectConfiguration foxRedirectConfiguration) {
    this.foxRedirectConfiguration = foxRedirectConfiguration;
  }

  public String getViewPearsLicenceUrl(Licence licence) {
    var pearsRedirectUrl = foxRedirectConfiguration.pearsRedirectUrl();
    var viewLicenceUrl = VIEW_LICENCE_URL.formatted(pearsRedirectUrl, licence.getLicenceReference());

    return foxRedirectConfiguration.epasRedirectUrl() + viewLicenceUrl;
  }
}
