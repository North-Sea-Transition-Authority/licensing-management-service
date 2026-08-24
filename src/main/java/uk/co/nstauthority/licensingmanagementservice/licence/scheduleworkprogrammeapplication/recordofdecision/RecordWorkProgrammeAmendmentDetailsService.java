package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceJson;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;

@Service
public class RecordWorkProgrammeAmendmentDetailsService {

  private final RecordOfDecisionWorkProgrammeRepository recordOfDecisionWorkProgrammeRepository;
  private final RecordOfDecisionWorkProgrammeLicenceRepository recordOfDecisionWorkProgrammeLicenceRepository;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final LicenceService licenceService;

  public RecordWorkProgrammeAmendmentDetailsService(
      RecordOfDecisionWorkProgrammeRepository recordOfDecisionWorkProgrammeRepository,
      RecordOfDecisionWorkProgrammeLicenceRepository recordOfDecisionWorkProgrammeLicenceRepository,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      WorkProgrammeActivityService workProgrammeActivityService,
      LicenceService licenceService
  ) {
    this.recordOfDecisionWorkProgrammeRepository = recordOfDecisionWorkProgrammeRepository;
    this.recordOfDecisionWorkProgrammeLicenceRepository = recordOfDecisionWorkProgrammeLicenceRepository;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.licenceService = licenceService;
  }

  public boolean hasAmendmentDetails(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return recordOfDecisionWorkProgrammeRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail);
  }

  public boolean isActivityAlreadyDecided(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      String workProgrammeActivityId
  ) {
    if (workProgrammeActivityId == null) {
      return false;
    }

    try {
      return recordOfDecisionWorkProgrammeRepository
          .existsByScheduleWorkProgrammeApplicationDetailAndWorkProgrammeActivityId(
              applicationDetail, UUID.fromString(workProgrammeActivityId));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public List<WorkProgrammeActivityView> getSelectableActivityViews(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var alreadyRecordedIds = recordOfDecisionWorkProgrammeRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail)
        .stream()
        .map(workProgramme -> workProgramme.getWorkProgrammeActivity().getId().toString())
        .collect(Collectors.toSet());

    var licenceScheduleDetail = scheduleWorkProgrammeApplicationService
        .getCurrentScheduleDetailFromApplicationDetail(applicationDetail);

    return workProgrammeActivityService.getLicenceWorkProgramActivitiesViews(licenceScheduleDetail)
        .stream()
        .filter(view -> !alreadyRecordedIds.contains(view.id()))
        .toList();
  }

  public RecordWorkProgrammeAmendmentDetailsForm getFilledForm(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      WorkProgrammeActivity workProgrammeActivity
  ) {
    var form = new RecordWorkProgrammeAmendmentDetailsForm();

    findWorkProgramme(applicationDetail, workProgrammeActivity).ifPresent(workProgramme -> {
      form.setDecision(workProgramme.getDecision());
      form.setAmendDuration(workProgramme.getAmendDuration());
      form.setAmendText(workProgramme.getAmendText());
      form.setAmendedText(workProgramme.getAmendedText());

      if (workProgramme.getAmendedDuration() != null) {
        form.getAmendedDuration().setFromThreeFieldDuration(workProgramme.getAmendedDuration());
      }

      form.setTargetLicenceIds(recordOfDecisionWorkProgrammeLicenceRepository
          .findAllByRecordOfDecisionWorkProgramme(workProgramme)
          .stream()
          .map(workProgrammeLicence -> workProgrammeLicence.getLicence().getId().toString())
          .toList());
    });

    return form;
  }

  public List<LicenceJson> getTargetLicenceSelections(List<String> targetLicenceIds) {
    return getTargetLicences(targetLicenceIds)
        .stream()
        .map(licence -> new LicenceJson(licence.getId(), licence.getLicenceReference()))
        .toList();
  }

  @Transactional
  public void saveAmendmentDetails(
      RecordWorkProgrammeAmendmentDetailsForm form,
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      WorkProgrammeActivity workProgrammeActivity
  ) {
    var workProgramme = findWorkProgramme(applicationDetail, workProgrammeActivity)
        .orElseGet(RecordOfDecisionWorkProgramme::new);

    workProgramme.setScheduleWorkProgrammeApplicationDetail(applicationDetail);
    workProgramme.setWorkProgrammeActivity(workProgrammeActivity);
    workProgramme.setDecision(form.getDecision());

    setAmendmentAnswers(workProgramme, form);

    recordOfDecisionWorkProgrammeRepository.save(workProgramme);

    saveTargetLicences(workProgramme, form);
  }

  private void setAmendmentAnswers(
      RecordOfDecisionWorkProgramme workProgramme,
      RecordWorkProgrammeAmendmentDetailsForm form
  ) {
    if (form.getDecision() != WorkProgrammeAmendmentDecision.AMEND) {
      workProgramme.setAmendDuration(null);
      workProgramme.setAmendText(null);
      workProgramme.setAmendedDuration(null);
      workProgramme.setAmendedText(null);
      return;
    }

    var amendDuration = BooleanUtils.isTrue(form.getAmendDuration());
    var amendText = BooleanUtils.isTrue(form.getAmendText());

    workProgramme.setAmendDuration(amendDuration);
    workProgramme.setAmendText(amendText);
    workProgramme.setAmendedDuration(amendDuration ? form.getAmendedDuration().toThreeFieldDuration() : null);
    workProgramme.setAmendedText(amendText ? form.getAmendedText() : null);
  }

  private void saveTargetLicences(
      RecordOfDecisionWorkProgramme workProgramme,
      RecordWorkProgrammeAmendmentDetailsForm form
  ) {
    var targetLicences = form.getDecision() == WorkProgrammeAmendmentDecision.COMPLETE_ON_ANOTHER_LICENCE
        ? getTargetLicences(form.getTargetLicenceIds())
        : List.<Licence>of();

    var targetLicenceIds = targetLicences.stream()
        .map(Licence::getId)
        .collect(Collectors.toSet());

    var savedWorkProgrammeLicences = recordOfDecisionWorkProgrammeLicenceRepository
        .findAllByRecordOfDecisionWorkProgramme(workProgramme);

    savedWorkProgrammeLicences.stream()
        .filter(workProgrammeLicence -> !targetLicenceIds.contains(workProgrammeLicence.getLicence().getId()))
        .forEach(recordOfDecisionWorkProgrammeLicenceRepository::delete);

    var savedLicenceIds = savedWorkProgrammeLicences.stream()
        .map(workProgrammeLicence -> workProgrammeLicence.getLicence().getId())
        .collect(Collectors.toSet());

    var newWorkProgrammeLicences = targetLicences.stream()
        .filter(licence -> !savedLicenceIds.contains(licence.getId()))
        .map(licence -> {
          var workProgrammeLicence = new RecordOfDecisionWorkProgrammeLicence();
          workProgrammeLicence.setRecordOfDecisionWorkProgramme(workProgramme);
          workProgrammeLicence.setLicence(licence);
          return workProgrammeLicence;
        })
        .toList();

    recordOfDecisionWorkProgrammeLicenceRepository.saveAll(newWorkProgrammeLicences);
  }

  private List<Licence> getTargetLicences(List<String> targetLicenceIds) {
    if (targetLicenceIds == null || targetLicenceIds.isEmpty()) {
      return List.of();
    }

    var licenceIds = targetLicenceIds.stream()
        .map(Integer::valueOf)
        .toList();

    return licenceService.getLicencesByIds(licenceIds);
  }

  private Optional<RecordOfDecisionWorkProgramme> findWorkProgramme(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      WorkProgrammeActivity workProgrammeActivity
  ) {
    return recordOfDecisionWorkProgrammeRepository
        .findByScheduleWorkProgrammeApplicationDetailAndWorkProgrammeActivity(applicationDetail, workProgrammeActivity);
  }
}
