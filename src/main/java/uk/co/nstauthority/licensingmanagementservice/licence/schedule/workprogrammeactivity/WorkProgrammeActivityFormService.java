package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeFeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Service
public class WorkProgrammeActivityFormService {

  private final WorkProgrammeActivityRepository workProgrammeActivityRepository;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceTypeFeatureService licenceTypeFeatureService;

  public WorkProgrammeActivityFormService(
      WorkProgrammeActivityRepository workProgrammeActivityRepository,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceTypeFeatureService licenceTypeFeatureService
  ) {
    this.workProgrammeActivityRepository = workProgrammeActivityRepository;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceTypeFeatureService = licenceTypeFeatureService;
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
    return licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .sorted(Comparator.comparingInt(phase -> phase.getPhaseType().getDisplayOrder()))
        .collect(StreamUtil.toLinkedHashMap(
            phase -> phase.getId().toString(),
            phase -> phase.getPhaseType().getDisplayName())
        );
  }

  public Map<String, String> getDateOptions(LicenceScheduleDetail licenceScheduleDetail) {
    var options = new ArrayList<>(Arrays.asList(WorkProgrammeActivityDateOption.values()));

    var licenceType = licenceScheduleDetail.getLicenceSchedule().getLicence().getType();

    if (!licenceTypeFeatureService.arePhasesCaptured(licenceType) || getSchedulePhaseOptions(licenceScheduleDetail).isEmpty()) {
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

    if (dateOption.equals(WorkProgrammeActivityDateOption.FIXED_DATE)) {
      activity.setDueDate(form.getDueDateInput().getAsLocalDate().orElse(null));
    } else {
      activity.setDueDate(null);
    }

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

    activity.setComments(form.getComments());

    workProgrammeActivityRepository.save(activity);
  }
}
