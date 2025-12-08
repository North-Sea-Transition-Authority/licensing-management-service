package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Service
public class WorkProgrammeActivityFormService {

  private final WorkProgrammeActivityRepository workProgrammeActivityRepository;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;

  public WorkProgrammeActivityFormService(
      WorkProgrammeActivityRepository workProgrammeActivityRepository,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceTypeRulesResolver licenceTypeRulesResolver
  ) {
    this.workProgrammeActivityRepository = workProgrammeActivityRepository;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
  }

  public Map<String, String> getScheduleTermOptions(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .sorted(Comparator.comparingInt(term -> term.getTermType().getDisplayOrder()))
        .collect(StreamUtil.toLinkedHashMap(
            term -> term.getId().toString(),
            term -> term.getTermType().getDisplayName())
        );
  }

  public Map<String, String> getSchedulePhaseOptions(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .sorted(Comparator.comparingInt(phase -> phase.getPhaseType().getDisplayOrder()))
        .collect(StreamUtil.toLinkedHashMap(
            phase -> phase.getId().toString(),
            phase -> phase.getPhaseType().getDisplayName())
        );
  }

  public Map<String, String> getRelativeDateOptions(LicenceScheduleDetail licenceScheduleDetail) {
    var termPhaseMap = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .sorted(Comparator.comparingInt(term -> term.getTermType().getDisplayOrder()))
        .collect(StreamUtil.toLinkedHashMap(Function.identity(), this::getPhaseMap));

    HashMap<String, String> combinedOptions = new HashMap<>();

    for (var termPhase : termPhaseMap.entrySet()) {
      var phases = termPhase.getValue();

      if (phases.isEmpty()) {
        combinedOptions.put(
            termPhase.getKey().getId().toString(),
            "Start of %s".formatted(termPhase.getKey().getTermType().getDisplayName())
        );
      } else {
        combinedOptions.putAll(phases);
      }
    }

    return combinedOptions;
  }

  private Map<String, String> getPhaseMap(LicenceScheduleTerm term) {
    return licenceSchedulePhaseService.getActivePhasesByTerm(term).stream()
        .sorted(Comparator.comparingInt(phase -> phase.getPhaseType().getDisplayOrder()))
        .collect(StreamUtil.toLinkedHashMap(
            phase -> phase.getId().toString(),
            phase -> "Start of %s".formatted(phase.getPhaseType().getDisplayName()))
        );
  }

  public Map<String, String> getDateOptions(LicenceScheduleDetail licenceScheduleDetail) {
    var options = new ArrayList<>(Arrays.asList(WorkProgrammeActivityDateOption.values()));

    var licenceType = licenceScheduleDetail.getLicenceSchedule().getLicence().getType();

    if (!licenceTypeRulesResolver.arePhasesCaptured(licenceType) || getSchedulePhaseOptions(licenceScheduleDetail).isEmpty()) {
      options.remove(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);
    }

    return DisplayableEnumOptionUtil.getDisplayableOptions(options);
  }

  @Transactional
  public void saveActivityFromForm(
      WorkProgrammeActivityForm form,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var activity = new WorkProgrammeActivity();
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

    workProgrammeActivityRepository.save(activity);
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
