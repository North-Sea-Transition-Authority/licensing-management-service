package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class LicenceScheduleHistoryFormService {

  public LicenceScheduleHistoryForm getScheduleHistoryForm(LicenceScheduleDetail licenceScheduleDetail) {
    var form = new LicenceScheduleHistoryForm();
    form.setLicenceScheduleDetailId(licenceScheduleDetail.getId().toString());
    return form;
  }
}
