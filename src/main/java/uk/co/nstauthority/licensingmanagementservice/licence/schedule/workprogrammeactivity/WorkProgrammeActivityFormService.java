package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.LicenceScheduleRelativeOptionsService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Service
public class WorkProgrammeActivityFormService {

  private final WorkProgrammeActivityRepository workProgrammeActivityRepository;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;
  private final LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;
  private final WorkProgrammeActivityStatusService workProgrammeActivityStatusService;

  public WorkProgrammeActivityFormService(
      WorkProgrammeActivityRepository workProgrammeActivityRepository,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceTypeRulesResolver licenceTypeRulesResolver,
      LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService,
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      WorkProgrammeActivityStatusService workProgrammeActivityStatusService
  ) {
    this.workProgrammeActivityRepository = workProgrammeActivityRepository;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
    this.licenceScheduleRelativeOptionsService = licenceScheduleRelativeOptionsService;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
    this.workProgrammeActivityStatusService = workProgrammeActivityStatusService;
  }

  public Map<String, String> getDateOptions(LicenceScheduleDetail licenceScheduleDetail) {
    var options = new ArrayList<>(Arrays.asList(WorkProgrammeActivityDateOption.values()));

    var licenceType = licenceScheduleDetail.getLicenceSchedule().getLicence().getType();

    if (!licenceTypeRulesResolver.arePhasesCaptured(licenceType)
        || licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail).isEmpty()) {
      options.remove(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);
    }

    return DisplayableEnumOptionUtil.getDisplayableOptions(options);
  }

  @Transactional
  public void saveActivityFromForm(
      WorkProgrammeActivityForm form,
      LicenceScheduleDetail licenceScheduleDetail,
      WorkProgrammeActivity activity
  ) {
    activity.setLicenceScheduleDetail(licenceScheduleDetail);
    activity.setCategory(form.getWorkProgrammeActivityCategory());
    activity.setOtherCategoryName(form.getOtherCategoryName());
    activity.setDescription(form.getDescription());
    activity.setCommitment(form.getWorkProgrammeActivityCommitment());

    var dateOption = form.getWorkProgrammeActivityDateOption();

    activity.setDateOption(dateOption);

    if (dateOption.equals(WorkProgrammeActivityDateOption.WITHIN_A_TERM)) {
      activity.setLicenceScheduleTerm(
          licenceScheduleTermService.getTermByIdOrThrow(UUID.fromString(form.getLicenceScheduleTermId()))
      );
    } else {
      activity.setLicenceScheduleTerm(null);
    }

    if (dateOption.equals(WorkProgrammeActivityDateOption.WITHIN_A_PHASE)) {
      activity.setLicenceSchedulePhase(
          licenceSchedulePhaseService.getPhaseByIdOrThrow(UUID.fromString(form.getLicenceSchedulePhaseId()))
      );
    } else {
      activity.setLicenceSchedulePhase(null);
    }

    if (dateOption.equals(WorkProgrammeActivityDateOption.RELATIVE_DATE)) {
      setRelativeEvent(form, licenceScheduleDetail, activity);
      activity.setRelativeDuration(form.getRelativeDuration().toThreeFieldDuration());
    } else {
      activity.setRelativeDuration(null);
    }

    activity.setComments(form.getComments());

    if (activity.getEventReference() == null) {
      activity.setEventReference(UUID.randomUUID());
    }

    workProgrammeActivityRepository.save(activity);
    workProgrammeActivityStatusService.createInitialStatusFor(activity);
    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  public WorkProgrammeActivityForm getActivityForm(WorkProgrammeActivity workProgrammeActivity) {
    var form = new WorkProgrammeActivityForm();
    form.setWorkProgrammeActivityCategory(workProgrammeActivity.getCategory());
    form.setOtherCategoryName(workProgrammeActivity.getOtherCategoryName());
    form.setDescription(workProgrammeActivity.getDescription());
    form.setWorkProgrammeActivityCommitment(workProgrammeActivity.getCommitment());
    form.setComments(workProgrammeActivity.getComments());

    var dateOption = workProgrammeActivity.getDateOption();

    form.setWorkProgrammeActivityDateOption(dateOption);

    var termIdString = workProgrammeActivity.getLicenceScheduleTerm() != null
        ? String.valueOf(workProgrammeActivity.getLicenceScheduleTerm().getId())
        : null;

    var phaseIdString = workProgrammeActivity.getLicenceSchedulePhase() != null
        ? String.valueOf(workProgrammeActivity.getLicenceSchedulePhase().getId())
        : null;

    if (dateOption.equals(WorkProgrammeActivityDateOption.WITHIN_A_TERM)) {
      form.setLicenceScheduleTermId(termIdString);
    }

    if (dateOption.equals(WorkProgrammeActivityDateOption.WITHIN_A_PHASE)) {
      form.setLicenceSchedulePhaseId(phaseIdString);
    }

    if (dateOption.equals(WorkProgrammeActivityDateOption.RELATIVE_DATE)) {
      form.getRelativeDuration().setFromThreeFieldDuration(workProgrammeActivity.getRelativeDuration());

      var relativeIdString = termIdString != null
          ? termIdString
          : phaseIdString;

      form.setRelativeEventId(relativeIdString);
    }

    return form;
  }

  private void setRelativeEvent(
      WorkProgrammeActivityForm form,
      LicenceScheduleDetail licenceScheduleDetail,
      WorkProgrammeActivity activity
  ) {
    var eventId = UUID.fromString(form.getRelativeEventId());

    var termMap = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .collect(StreamUtil.toLinkedHashMap(LicenceScheduleTerm::getId, Function.identity()));

    if (termMap.containsKey(eventId)) {
      activity.setLicenceScheduleTerm(termMap.get(eventId));
      activity.setLicenceSchedulePhase(null);
    } else {
      activity.setLicenceScheduleTerm(null);
      activity.setLicenceSchedulePhase(licenceSchedulePhaseService.getPhaseByIdOrThrow(eventId));
    }
  }
}
