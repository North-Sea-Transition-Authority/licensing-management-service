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
  public void saveAmendmentForm(SelectLicenceAmendmentForm licenceScheduleAmendmentForm,
                                ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    var licenceWorkProgrammeAmendmentRequest = licenceWorkProgrammeAmendmentRepository
        .findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivityId(scheduleWorkProgrammeApplicationDetail,
            licenceScheduleAmendmentForm
                .getSelectedWorkProgrammeActivityAmendmentId())
        .orElse(new LicenceWorkProgrammeAmendmentRequest());

    licenceWorkProgrammeAmendmentRequest.setScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeActivityId(
        licenceScheduleAmendmentForm.getSelectedWorkProgrammeActivityAmendmentId());

    licenceWorkProgrammeAmendmentRepository.save(licenceWorkProgrammeAmendmentRequest);
  }

}