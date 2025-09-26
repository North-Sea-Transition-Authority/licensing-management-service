package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class SelectLicenceAmendmentFormService {

  LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  public SelectLicenceAmendmentFormService(
      LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository) {
    this.licenceWorkProgrammeAmendmentRepository = licenceWorkProgrammeAmendmentRepository;
  }

  @Transactional
  public void saveAmendmentForm(SelectLicenceAmendmentForm licenceScheduleExtensionForm,
                                ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    var licenceWorkProgramAmendmentRequest = licenceWorkProgrammeAmendmentRepository
        .findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivityId(
            scheduleWorkProgrammeApplicationDetail,
            licenceScheduleExtensionForm
                .getSelectedWorkProgrammeActivityAmendmentId())
        .orElse(new LicenceWorkProgramAmendmentRequest());

    licenceWorkProgramAmendmentRequest.setScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
    licenceWorkProgramAmendmentRequest.setWorkProgrammeActivityId(
        licenceScheduleExtensionForm.getSelectedWorkProgrammeActivityAmendmentId());

    licenceWorkProgrammeAmendmentRepository.save(licenceWorkProgramAmendmentRequest);
  }

}