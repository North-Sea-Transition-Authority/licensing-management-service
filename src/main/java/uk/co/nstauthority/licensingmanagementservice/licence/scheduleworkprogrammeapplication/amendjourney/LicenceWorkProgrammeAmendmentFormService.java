package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceWorkProgrammeAmendmentFormService {
  private final LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;
  private final LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  public LicenceWorkProgrammeAmendmentFormService(
      LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository,
      LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService
  ) {
    this.licenceWorkProgrammeAmendmentRepository = licenceWorkProgrammeAmendmentRepository;
    this.licenceWorkProgrammeAmendmentService = licenceWorkProgrammeAmendmentService;
  }

  @Transactional
  public void saveAmendmentForm(
      LicenceWorkProgrammeAmendmentForm licenceScheduleExtensionForm,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var licenceWorkProgrammeAmendmentRequest = licenceWorkProgrammeAmendmentRepository
        .findByScheduleWorkProgrammeApplicationDetails(
            scheduleWorkProgrammeApplicationDetail)
        .orElse(new LicenceWorkProgrammeAmendmentRequest());

    licenceWorkProgrammeAmendmentRequest.setScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
    licenceWorkProgrammeAmendmentRequest.setDurationExtensionRequired(
        licenceScheduleExtensionForm.isDurationExtensionRequired());
    licenceWorkProgrammeAmendmentRequest.setAdditionalInfoRequired(
        licenceScheduleExtensionForm.isAdditionalInfoRequired());

    if (!licenceScheduleExtensionForm.isDurationExtensionRequired()) {
      licenceWorkProgrammeAmendmentRequest.setWorkProgrammeExtensionDuration(null);
    } else {
      licenceWorkProgrammeAmendmentRequest.setWorkProgrammeExtensionDuration(
          licenceScheduleExtensionForm.getWorkProgrammeExtensionDuration().toThreeFieldDuration());
    }

    if (!licenceScheduleExtensionForm.isAdditionalInfoRequired()) {
      licenceWorkProgrammeAmendmentRequest.setWorkProgrammeAmendmentInformation(null);
    } else {
      licenceWorkProgrammeAmendmentRequest.setWorkProgrammeAmendmentInformation(
          licenceScheduleExtensionForm.getWorkProgrammeAmendmentInformation());
    }

    licenceWorkProgrammeAmendmentRepository.save(licenceWorkProgrammeAmendmentRequest);
  }

  private LicenceWorkProgrammeAmendmentForm licenceWorkProgramAmendmentToForm(
      LicenceWorkProgrammeAmendmentRequest licenceWorkProgrammeAmendmentRequest
  ) {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.getWorkProgrammeExtensionDuration().setFromThreeFieldDuration(
        licenceWorkProgrammeAmendmentRequest.getWorkProgrammeExtensionDuration());
    form.setAdditionalInfoRequired(licenceWorkProgrammeAmendmentRequest.isAdditionalInfoRequired());
    form.setDurationExtensionRequired(licenceWorkProgrammeAmendmentRequest.isDurationExtensionRequired());
    form.setWorkProgrammeAmendmentInformation(
        licenceWorkProgrammeAmendmentRequest.getWorkProgrammeAmendmentInformation());
    return form;
  }

  public LicenceWorkProgrammeAmendmentForm getLicenceWorkProgrammeActivityAmendmentForm(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return licenceWorkProgrammeAmendmentService
        .getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail)
        .map(this::licenceWorkProgramAmendmentToForm)
        .orElse(new LicenceWorkProgrammeAmendmentForm());
  }
}