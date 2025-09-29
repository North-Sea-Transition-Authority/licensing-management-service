package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.DurationInputMapper;
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
  public void saveAmendmentForm(LicenceWorkProgrammeAmendmentForm licenceScheduleExtensionForm,
                                ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
                                UUID workProgrammeActivityId
  ) {
    var licenceWorkProgrammeAmendmentRequest = licenceWorkProgrammeAmendmentRepository
        .findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivityId(
            scheduleWorkProgrammeApplicationDetail, workProgrammeActivityId)
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
      LicenceWorkProgrammeAmendmentRequest licenceWorkProgrammeAmendmentRequest) {

    var form = new LicenceWorkProgrammeAmendmentForm();
    var formExtensionDuration = form.getWorkProgrammeExtensionDuration();
    var requestExtensionDuration = licenceWorkProgrammeAmendmentRequest.getWorkProgrammeExtensionDuration();

    DurationInputMapper.mapToFormDuration(formExtensionDuration, requestExtensionDuration);

    form.setWorkProgrammeAmendmentInformation(
        licenceWorkProgrammeAmendmentRequest.getWorkProgrammeAmendmentInformation());
    return form;

  }

  public LicenceWorkProgrammeAmendmentForm getLicenceWorkProgramAmendmentForm(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail, UUID workProgrammeActivityId) {
    return licenceWorkProgrammeAmendmentService.getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail, workProgrammeActivityId).map(
        this::licenceWorkProgramAmendmentToForm).orElse(new LicenceWorkProgrammeAmendmentForm());
  }
}