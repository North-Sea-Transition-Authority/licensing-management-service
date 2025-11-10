package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceWorkProgrammeAmendmentService {

  private final LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;
  private final LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator;

  public LicenceWorkProgrammeAmendmentService(
      LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository,
      LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator
  ) {
    this.licenceWorkProgrammeAmendmentRepository = licenceWorkProgrammeAmendmentRepository;
    this.licenceWorkProgrammeAmendmentFormValidator = licenceWorkProgrammeAmendmentFormValidator;
  }

  public Optional<LicenceWorkProgrammeAmendmentRequest> getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail, UUID workProgrammeActivityId) {
    return licenceWorkProgrammeAmendmentRepository
        .findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivityId(
            scheduleWorkProgrammeApplicationDetail, workProgrammeActivityId);
  }

  public List<LicenceWorkProgrammeAmendmentRequest> getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return licenceWorkProgrammeAmendmentRepository
        .findAllByScheduleWorkProgrammeApplicationDetails(
            scheduleWorkProgrammeApplicationDetail);
  }

  public boolean hasAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return licenceWorkProgrammeAmendmentRepository
        .existsByScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
  }

  public boolean isAmendmentRequested(ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return licenceWorkProgrammeAmendmentRepository
        .existsByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeCompletionDateChangeRequestedTrue(
            scheduleWorkProgrammeApplicationDetail);
  }

  @Transactional
  public void deleteWorkProgrammeAmendment(LicenceWorkProgrammeAmendmentRequest licenceWorkProgrammeAmendmentRequest,
                                           ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    licenceWorkProgrammeAmendmentRequest.setScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
    licenceWorkProgrammeAmendmentRepository.delete(licenceWorkProgrammeAmendmentRequest);
  }

  @Transactional
  public void saveAmendmentForm(LicenceWorkProgrammeAmendmentForm licenceScheduleExtensionForm,
                                ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
                                UUID workProgrammeActivityId) {
    var licenceWorkProgrammeAmendmentRequest = licenceWorkProgrammeAmendmentRepository
        .findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivityId(
            scheduleWorkProgrammeApplicationDetail, workProgrammeActivityId)
        .orElse(new LicenceWorkProgrammeAmendmentRequest());

    licenceWorkProgrammeAmendmentRequest.setScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeCompletionDateChangeRequested(
        licenceScheduleExtensionForm.getDurationExtensionRequired());
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeChangeRequested(
        licenceScheduleExtensionForm.getAdditionalInfoRequired());

    if (BooleanUtils.isNotTrue(licenceScheduleExtensionForm.getDurationExtensionRequired())) {
      licenceWorkProgrammeAmendmentRequest.setWorkProgrammeExtensionDuration(null);
    } else {
      licenceWorkProgrammeAmendmentRequest.setWorkProgrammeExtensionDuration(
          licenceScheduleExtensionForm.getWorkProgrammeExtensionDuration().toThreeFieldDuration());
    }

    if (BooleanUtils.isNotTrue(licenceScheduleExtensionForm.getAdditionalInfoRequired())) {
      licenceWorkProgrammeAmendmentRequest.setWorkProgrammeAmendmentInformation(null);
    } else {
      licenceWorkProgrammeAmendmentRequest.setWorkProgrammeAmendmentInformation(
          licenceScheduleExtensionForm.getWorkProgrammeAmendmentInformation());
    }
    licenceWorkProgrammeAmendmentRepository.save(licenceWorkProgrammeAmendmentRequest);
  }

  public LicenceWorkProgrammeAmendmentForm getLicenceWorkProgrammeActivityAmendmentForm(
      UUID workProgrammeActivityId,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    return getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail, workProgrammeActivityId)
        .map(this::licenceWorkProgramAmendmentToForm)
        .orElse(new LicenceWorkProgrammeAmendmentForm());
  }

  public LicenceWorkProgrammeAmendmentRequest getAmendmentRequestByScheduleWorkProgrammeApplicationDetailElseThrow(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail, UUID workProgrammeActivityId) {
    return licenceWorkProgrammeAmendmentRepository
        .findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivityId(
            scheduleWorkProgrammeApplicationDetail, workProgrammeActivityId)
        .orElseThrow(() ->
            new LmsEntityNotFoundException(
                String.format("Licence work programme amendment %s request not found", workProgrammeActivityId.toString())));
  }

  private LicenceWorkProgrammeAmendmentForm licenceWorkProgramAmendmentToForm(
      LicenceWorkProgrammeAmendmentRequest licenceWorkProgrammeAmendmentRequest) {

    var form = new LicenceWorkProgrammeAmendmentForm();
    var formExtensionDuration = form.getWorkProgrammeExtensionDuration();
    var requestExtensionDuration = licenceWorkProgrammeAmendmentRequest.getWorkProgrammeExtensionDuration();

    if (requestExtensionDuration != null) {
      formExtensionDuration.setFromThreeFieldDuration(requestExtensionDuration);
    }

    form.setWorkProgrammeExtensionDuration(formExtensionDuration);
    form.setAdditionalInfoRequired(licenceWorkProgrammeAmendmentRequest.getWorkProgrammeChangeRequested());
    form.setDurationExtensionRequired(licenceWorkProgrammeAmendmentRequest.getWorkProgrammeCompletionDateChangeRequested());
    form.setWorkProgrammeAmendmentInformation(
        licenceWorkProgrammeAmendmentRequest.getWorkProgrammeAmendmentInformation());
    return form;
  }

  public boolean validateAllWorkProgrammeAmendments(
      List<LicenceWorkProgrammeAmendmentRequest> workProgrammeApplicationDetails) {

    return workProgrammeApplicationDetails
        .stream()
        .map(this::licenceWorkProgramAmendmentToForm)
        .allMatch(form -> {
          BindingResult bindingResult = new BeanPropertyBindingResult(
              form,
              "form"
          );
          return licenceWorkProgrammeAmendmentFormValidator.isValid(
              form,
              bindingResult
          );
        });
  }
}