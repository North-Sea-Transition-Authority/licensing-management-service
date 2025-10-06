package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class SelectLicenceAmendmentFormService {

  LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;
  LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  public SelectLicenceAmendmentFormService(
      LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository,
      LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService) {
    this.licenceWorkProgrammeAmendmentRepository = licenceWorkProgrammeAmendmentRepository;
    this.licenceWorkProgrammeAmendmentService = licenceWorkProgrammeAmendmentService;
  }

  @Transactional
  public void saveAmendmentForm(SelectLicenceAmendmentForm licenceScheduleAmendmentForm,
                                ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    var licenceWorkProgrammeAmendmentRequest = licenceWorkProgrammeAmendmentRepository
        .findByScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail)
        .orElse(new LicenceWorkProgrammeAmendmentRequest());

    licenceWorkProgrammeAmendmentRequest.setScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeActivityId(
        licenceScheduleAmendmentForm.getSelectedWorkProgrammeActivityAmendmentId());

    licenceWorkProgrammeAmendmentRepository.save(licenceWorkProgrammeAmendmentRequest);
  }

  private SelectLicenceAmendmentForm licenceWorkProgramAmendmentToForm(
      LicenceWorkProgrammeAmendmentRequest licenceWorkProgrammeAmendmentRequest) {
    var form = new SelectLicenceAmendmentForm();
    form.setSelectedWorkProgrammeActivityAmendmentId(licenceWorkProgrammeAmendmentRequest.getWorkProgrammeActivityId());
    return form;
  }

  public SelectLicenceAmendmentForm getLicenceSelectWorkProgramAmendmentForm(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    return licenceWorkProgrammeAmendmentService
        .getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail)
        .map(this::licenceWorkProgramAmendmentToForm)
        .orElse(new SelectLicenceAmendmentForm());
  }

}