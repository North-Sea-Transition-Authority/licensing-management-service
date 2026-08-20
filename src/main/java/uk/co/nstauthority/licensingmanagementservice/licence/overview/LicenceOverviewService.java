package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;

@Service
public class LicenceOverviewService {

  private static final String CS_REGISTER_URL =
      "https://www.nstauthority.co.uk/regulatory-information/carbon-storage/carbon-storage-public-register/?section=%s";

  public LicenceOverviewView getLicenceOverviewView(Licence licence) {
    return new LicenceOverviewView(
        licence.getLicenceReference(),
        licence.getType().getDisplayName(),
        getCsRegisterLink(licence)
    );
  }

  private String getCsRegisterLink(Licence licence) {
    if (!LicenceType.CARBON_STORAGE.equals(licence.getType())) {
      return null;
    }

    return CS_REGISTER_URL.formatted(licence.getLicenceReference());
  }
}
