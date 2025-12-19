package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationService;

@Service
public class LicenceWorkProgrammeAmendmentService {

  private final LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;
  private final LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator;
  private final WorkProgrammeActivityService  workProgrammeActivityService;
  private final LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService;

  public LicenceWorkProgrammeAmendmentService(
      LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository,
      LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator,
      WorkProgrammeActivityService workProgrammeActivityService,
      LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService
  ) {
    this.licenceWorkProgrammeAmendmentRepository = licenceWorkProgrammeAmendmentRepository;
    this.licenceWorkProgrammeAmendmentFormValidator = licenceWorkProgrammeAmendmentFormValidator;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.licenceScheduleSupportingInformationService = licenceScheduleSupportingInformationService;
  }

  public Optional<LicenceWorkProgrammeAmendmentRequest> getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      WorkProgrammeActivity workProgrammeActivity
  ) {
    return licenceWorkProgrammeAmendmentRepository
        .findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivity(
            scheduleWorkProgrammeApplicationDetail, workProgrammeActivity);
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

  @Transactional
  public void deleteWorkProgrammeAmendment(LicenceWorkProgrammeAmendmentRequest licenceWorkProgrammeAmendmentRequest,
                                           ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    licenceWorkProgrammeAmendmentRequest.setScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
    licenceWorkProgrammeAmendmentRepository.delete(licenceWorkProgrammeAmendmentRequest);
  }

  @Transactional
  public void saveAmendmentForm(LicenceWorkProgrammeAmendmentForm licenceScheduleExtensionForm,
                                ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
                                WorkProgrammeActivity workProgrammeActivity) {
    var licenceWorkProgrammeAmendmentRequest = licenceWorkProgrammeAmendmentRepository
        .findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivity(
            scheduleWorkProgrammeApplicationDetail, workProgrammeActivity)
        .orElse(new LicenceWorkProgrammeAmendmentRequest());

    licenceWorkProgrammeAmendmentRequest.setScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeCompletionDateChangeRequested(
        licenceScheduleExtensionForm.getDurationExtensionRequired());
    licenceWorkProgrammeAmendmentRequest.setWorkProgrammeChangeRequested(
        licenceScheduleExtensionForm.getAdditionalInfoRequired());

    if (BooleanUtils.isNotTrue(licenceScheduleExtensionForm.getDurationExtensionRequired())) {
      licenceWorkProgrammeAmendmentRequest.setWorkProgrammeExtensionDuration(null);
      licenceScheduleSupportingInformationService
          .handleSupportingInformationExtensionRemoval(scheduleWorkProgrammeApplicationDetail);
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
      WorkProgrammeActivity workProgrammeActivity,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    return getAmendmentRequestByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail, workProgrammeActivity)
        .map(this::licenceWorkProgramAmendmentToForm)
        .orElse(new LicenceWorkProgrammeAmendmentForm());
  }

  public LicenceWorkProgrammeAmendmentRequest getAmendmentRequestByScheduleWorkProgrammeApplicationDetailElseThrow(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      WorkProgrammeActivity workProgrammeActivity
  ) {
    return licenceWorkProgrammeAmendmentRepository
        .findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivity(
            scheduleWorkProgrammeApplicationDetail, workProgrammeActivity)
        .orElseThrow(() ->
                         new LmsEntityNotFoundException(
                             String.format(
                                 "Licence work programme amendment %s request not found",
                                 workProgrammeActivity.getId().toString()
                             )));
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

  public List<WorkProgrammeActivityAmendmentView> getLicenceWorkProgramAmendmentViews(
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    List<WorkProgrammeActivity> workProgrammeActivities = workProgrammeActivityService.getActiveWorkProgrammeActivities(
        licenceScheduleDetail
    );

    return workProgrammeActivities
        .stream()
        .map(this::createWorkProgrammeActivityAmendmentView)
        .toList();
  }

  public WorkProgrammeActivityAmendmentView getLicenceWorkProgramAmendmentView(
      WorkProgrammeActivity workProgrammeActivity
  ) {
    return createWorkProgrammeActivityAmendmentView(workProgrammeActivity);
  }

  private WorkProgrammeActivityAmendmentView createWorkProgrammeActivityAmendmentView(
      WorkProgrammeActivity workProgrammeActivity
  ) {
    LocalDate dueDate = resolveWorkProgrammeActivityDueDate(workProgrammeActivity);

    return new WorkProgrammeActivityAmendmentView(
        workProgrammeActivity.getId().toString(),
        DateFormatUtil.convertToDisplayText(dueDate),
        resolveCategory(workProgrammeActivity),
        workProgrammeActivity.getDescription(),
        getCategoryWithDueDate(workProgrammeActivity, dueDate)
    );
  }

  public String getCategoryWithDueDate(WorkProgrammeActivity activity, LocalDate dueDate) {
    return resolveCategory(activity) + " " + DateFormatUtil.convertToDisplayTextWithDueDateLabel(dueDate);
  }

  public LocalDate resolveWorkProgrammeActivityDueDate(WorkProgrammeActivity activity) {
    WorkProgrammeActivityDateOption dateOption = activity.getDateOption();

    if (dateOption.equals(WorkProgrammeActivityDateOption.WITHIN_A_PHASE)) {
      return activity.getLicenceSchedulePhase().getEndDate();
    }
    if (dateOption.equals(WorkProgrammeActivityDateOption.WITHIN_A_TERM)) {
      return activity.getLicenceScheduleTerm().getEndDate();
    }
    return activity.getDueDate();
  }

  public String resolveCategory(WorkProgrammeActivity workProgrammeActivity) {
    if (workProgrammeActivity.getOtherCategoryName() == null) {
      return workProgrammeActivity.getCategory().getDisplayName();
    }
    return workProgrammeActivity.getOtherCategoryName();
  }
}