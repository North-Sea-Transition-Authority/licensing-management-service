package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class LicenceScheduleOverviewService {

  private static final String CS_REGISTER_URL = "https://www.nstauthority.co.uk/regulatory-information/carbon-storage/carbon-storage-public-register/?section=%s";


  public LicenceScheduleHistoryForm getScheduleHistoryForm(LicenceScheduleDetail licenceScheduleDetail) {
    var form = new LicenceScheduleHistoryForm();
    form.setLicenceScheduleDetailId(licenceScheduleDetail.getId().toString());
    return form;
  }

  public String getCsRegisterlink(Licence licence) {
    if (!licence.getType().equals(LicenceType.CARBON_STORAGE)) {
      return "";
    }

    return CS_REGISTER_URL.formatted(licence.getLicenceReference());
  }
}
