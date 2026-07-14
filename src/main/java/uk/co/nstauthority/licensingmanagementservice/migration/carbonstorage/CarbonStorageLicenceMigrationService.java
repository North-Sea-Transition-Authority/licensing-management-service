package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;

@Service
public class CarbonStorageLicenceMigrationService {

  private static final DateTimeFormatter CASE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final LicenceService licenceService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final LicenceScheduleService licenceScheduleService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceStartDateService licenceStartDateService;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final WorkProgrammeActivityStatusRepository workProgrammeActivityStatusRepository;
  private final EventCommentRepository eventCommentRepository;
  private final CarbonStorageLicenceMigrationExtractRepository carbonStorageLicenceMigrationExtractRepository;
  private final CarbonStorageLicenceOrgMappingRepository carbonStorageLicenceOrgMappingRepository;
  private final CarbonStorageStartDateMigrationRepository carbonStorageStartDateMigrationRepository;
  private final CarbonStorageTermMigrationRepository carbonStorageTermMigrationRepository;
  private final CarbonStorageWorkProgrammeMigrationRepository carbonStorageWorkProgrammeMigrationRepository;

  public CarbonStorageLicenceMigrationService(
      LicenceService licenceService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      LicenceScheduleService licenceScheduleService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceStartDateService licenceStartDateService,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      WorkProgrammeActivityService workProgrammeActivityService,
      WorkProgrammeActivityStatusRepository workProgrammeActivityStatusRepository,
      EventCommentRepository eventCommentRepository,
      CarbonStorageLicenceMigrationExtractRepository carbonStorageLicenceMigrationExtractRepository,
      CarbonStorageLicenceOrgMappingRepository carbonStorageLicenceOrgMappingRepository,
      CarbonStorageStartDateMigrationRepository carbonStorageStartDateMigrationRepository,
      CarbonStorageTermMigrationRepository carbonStorageTermMigrationRepository,
      CarbonStorageWorkProgrammeMigrationRepository carbonStorageWorkProgrammeMigrationRepository
  ) {
    this.licenceService = licenceService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.licenceScheduleService = licenceScheduleService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceStartDateService = licenceStartDateService;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.workProgrammeActivityStatusRepository = workProgrammeActivityStatusRepository;
    this.eventCommentRepository = eventCommentRepository;
    this.carbonStorageLicenceMigrationExtractRepository = carbonStorageLicenceMigrationExtractRepository;
    this.carbonStorageLicenceOrgMappingRepository = carbonStorageLicenceOrgMappingRepository;
    this.carbonStorageStartDateMigrationRepository = carbonStorageStartDateMigrationRepository;
    this.carbonStorageTermMigrationRepository = carbonStorageTermMigrationRepository;
    this.carbonStorageWorkProgrammeMigrationRepository = carbonStorageWorkProgrammeMigrationRepository;
  }

  @Transactional
  public void migrateLicences() {
    var carbonStorageLicenceMigrationExtracts = carbonStorageLicenceMigrationExtractRepository.findAll();

    var currentLicenceId = licenceService.getNextLicenceId();

    var licences = new ArrayList<Licence>();
    var licenceResponsibleOrganisations = new ArrayList<LicenceResponsibleOrganisation>();

    for (var migrationExtract : carbonStorageLicenceMigrationExtracts) {
      var licence = new Licence();
      licence.setId(currentLicenceId++);
      licence.setType(LicenceType.CARBON_STORAGE);
      licence.setPrefix(LicenceType.CARBON_STORAGE.getPrefix());
      licence.setLicenceNumber(migrationExtract.getLicenceNumber());
      licence.setLicenceReference(migrationExtract.getLicenceRef());
      licence.setStatus(getLicenceStatusFromString(migrationExtract.getStatus()));
      licences.add(licence);

      String[] organisations = migrationExtract.getResponsibleOrgs().split(",");
      for (var responsibleOrg : organisations) {

        var organisationUnitId = carbonStorageLicenceOrgMappingRepository.findByCsExtractResponsibleOrganisation(
            responsibleOrg.trim()
        ).getOrganisationUnitId();

        if (organisationUnitId != null) {
          var licenceResponsibleOrganisation = new LicenceResponsibleOrganisation();
          licenceResponsibleOrganisation.setLicence(licence);
          licenceResponsibleOrganisation.setResponsibleOrganisationId(organisationUnitId);
          licenceResponsibleOrganisation.setManagedByLms(true);
          licenceResponsibleOrganisations.add(licenceResponsibleOrganisation);
        }
      }
    }

    licenceService.saveLicences(licences);
    licenceResponsibleOrganisationService.saveLicensees(licenceResponsibleOrganisations);
  }

  @Transactional
  public void migrateSchedules() {
    var licenceSchedules = buildLicenceSchedules();
    var savedSchedules = new ArrayList<LicenceSchedule>();
    licenceScheduleService.saveLicenceSchedules(licenceSchedules).forEach(savedSchedules::add);

    var casesByLicence = buildCasesByLicence();

    var detailsResult = buildScheduleDetails(savedSchedules, casesByLicence);
    licenceScheduleDetailService.saveLicenceScheduleDetails(detailsResult.details());

    var licenceStartDates = buildStartDates(detailsResult.details());
    licenceStartDateService.saveLicenceStartDates(licenceStartDates);

    var terms = buildTerms(savedSchedules, casesByLicence, detailsResult.detailsByCaseDate());
    licenceScheduleTermService.saveTerms(terms);
    var termsByDetailAndType = terms.stream()
        .collect(Collectors.groupingBy(
            LicenceScheduleTerm::getLicenceScheduleDetail,
            Collectors.toMap(LicenceScheduleTerm::getTermType, t -> t)
        ));

    var activitiesData = buildWorkProgrammeActivities(detailsResult.detailsByCaseDate(), termsByDetailAndType);
    workProgrammeActivityService.saveWorkProgrammeActivities(activitiesData.activities());
    workProgrammeActivityStatusRepository.saveAll(buildWorkProgrammeActivityStatuses(activitiesData.activityStatusData()));
    eventCommentRepository.saveAll(buildWorkProgrammeActivityComments(activitiesData.activityCommentData()));

    licenceStartDates.stream()
        .map(LicenceStartDate::getLicenceScheduleDetail)
        .distinct()
        .forEach(licenceScheduleCalculationService::calculateAndSaveLicenceScheduleDates);
  }

  private List<LicenceSchedule> buildLicenceSchedules() {
    var csLicences = licenceService.getAllLicences().stream()
        .filter(licence -> LicenceType.CARBON_STORAGE.equals(licence.getType()))
        .toList();
    var licenceSchedules = new ArrayList<LicenceSchedule>();
    for (var licence : csLicences) {
      var licenceSchedule = new LicenceSchedule();
      licenceSchedule.setLicence(licence);
      licenceSchedules.add(licenceSchedule);
    }
    return licenceSchedules;
  }

  private Map<String, List<CsLicenceCase>> buildCasesByLicence() {
    var allCases = Stream.concat(
        carbonStorageTermMigrationRepository.findDistinctCases().stream(),
        carbonStorageWorkProgrammeMigrationRepository.findDistinctCases().stream()
    ).collect(Collectors.toMap(
        c -> c.getLicenceRef() + "|" + c.getCaseDate(),
        c -> c,
        (a, b) -> a
    )).values();

    return allCases.stream()
        .collect(Collectors.groupingBy(
            CsLicenceCase::getLicenceRef,
            Collectors.collectingAndThen(
                Collectors.toList(),
                list -> list.stream()
                    .sorted(Comparator.comparing(c -> LocalDate.parse(c.getCaseDate(), CASE_DATE_FORMAT)))
                    .toList()
            )
        ));
  }

  private ScheduleDetailsResult buildScheduleDetails(
      List<LicenceSchedule> savedSchedules,
      Map<String, List<CsLicenceCase>> casesByLicence
  ) {
    var detailsByCaseDate = new HashMap<String, LicenceScheduleDetail>();
    var licenceScheduleDetails = new ArrayList<LicenceScheduleDetail>();

    for (var savedSchedule : savedSchedules) {
      var licenceRef = savedSchedule.getLicence().getLicenceReference();
      var cases = casesByLicence.getOrDefault(licenceRef, List.of());

      if (cases.isEmpty()) {
        var detail = new LicenceScheduleDetail();
        detail.setLicenceSchedule(savedSchedule);
        detail.setStatus(LicenceScheduleDetailStatus.ACTIVE);
        detail.setCreatedInstant(Instant.now());
        licenceScheduleDetails.add(detail);
      } else {
        for (int i = 0; i < cases.size(); i++) {
          var c = cases.get(i);
          var detail = new LicenceScheduleDetail();
          detail.setLicenceSchedule(savedSchedule);
          detail.setStatus(i == cases.size() - 1
              ? LicenceScheduleDetailStatus.ACTIVE
              : LicenceScheduleDetailStatus.REPLACED);
          detail.setCreatedInstant(LocalDate.parse(c.getCaseDate(), CASE_DATE_FORMAT)
              .atStartOfDay(ZoneOffset.UTC).toInstant());
          licenceScheduleDetails.add(detail);
          detailsByCaseDate.put(licenceRef + "|" + c.getCaseDate(), detail);
        }
      }
    }

    return new ScheduleDetailsResult(licenceScheduleDetails, detailsByCaseDate);
  }

  private List<LicenceStartDate> buildStartDates(List<LicenceScheduleDetail> allDetails) {
    var activeDetailByLicenceRef = allDetails.stream()
        .filter(d -> LicenceScheduleDetailStatus.ACTIVE.equals(d.getStatus()))
        .collect(Collectors.toMap(
            d -> d.getLicenceSchedule().getLicence().getLicenceReference(),
            d -> d
        ));

    var licenceStartDates = new ArrayList<LicenceStartDate>();
    for (var migrationStartDate : carbonStorageStartDateMigrationRepository.findAll()) {
      var detail = activeDetailByLicenceRef.get(migrationStartDate.getLicenceRef());
      if (detail != null) {
        var licenceStartDate = new LicenceStartDate();
        licenceStartDate.setLicenceScheduleDetail(detail);
        licenceStartDate.setStartDate(LocalDate.parse(migrationStartDate.getStartDate(), CASE_DATE_FORMAT));
        licenceStartDates.add(licenceStartDate);
      }
    }
    return licenceStartDates;
  }

  private List<LicenceScheduleTerm> buildTerms(
      List<LicenceSchedule> savedSchedules,
      Map<String, List<CsLicenceCase>> casesByLicence,
      Map<String, LicenceScheduleDetail> detailsByCaseDate
  ) {
    var migrationTermsList = new ArrayList<CarbonStorageTermMigrationExtract>();
    carbonStorageTermMigrationRepository.findAll().forEach(migrationTermsList::add);

    var termsByLicenceAndCase = migrationTermsList.stream()
        .collect(Collectors.groupingBy(
            CarbonStorageTermMigrationExtract::getLicenceRef,
            Collectors.groupingBy(CarbonStorageTermMigrationExtract::getCaseDate)
        ));

    var terms = new ArrayList<LicenceScheduleTerm>();

    for (var savedSchedule : savedSchedules) {
      var licenceRef = savedSchedule.getLicence().getLicenceReference();
      var cases = casesByLicence.getOrDefault(licenceRef, List.of());
      var termsByCase = termsByLicenceAndCase.getOrDefault(licenceRef, Map.of());
      List<CarbonStorageTermMigrationExtract> previousTerms = List.of();

      for (var c : cases) {
        var termsForCase = termsByCase.getOrDefault(c.getCaseDate(), List.of());
        if (!termsForCase.isEmpty()) {
          previousTerms = termsForCase;
        }
        var detail = detailsByCaseDate.get(licenceRef + "|" + c.getCaseDate());
        for (var migrationTerm : previousTerms) {
          var term = new LicenceScheduleTerm();
          term.setLicenceScheduleDetail(detail);
          term.setLicenceSchedule(detail.getLicenceSchedule());
          term.setTermType(migrationTerm.getTerm().equals("Initial")
              ? TermType.INITIAL_CS
              : EnumUtils.getEnum(TermType.class, migrationTerm.getTerm().toUpperCase()));
          term.setTermDuration(
              new ThreeFieldDuration(migrationTerm.getYears(), migrationTerm.getMonths(), migrationTerm.getDays())
          );
          terms.add(term);
        }
      }
    }

    return terms;
  }

  private WorkProgrammeActivityMigrationData buildWorkProgrammeActivities(
      Map<String, LicenceScheduleDetail> detailsByCaseDate,
      Map<LicenceScheduleDetail, Map<TermType, LicenceScheduleTerm>> termsByDetailAndType
  ) {
    var wpRowsList = new ArrayList<CarbonStorageWorkProgrammeMigrationExtract>();
    carbonStorageWorkProgrammeMigrationRepository.findAll().forEach(wpRowsList::add);
    var wpByLicenceAndCase = wpRowsList.stream()
        .collect(Collectors.groupingBy(
            CarbonStorageWorkProgrammeMigrationExtract::getLicenceRef,
            Collectors.groupingBy(CarbonStorageWorkProgrammeMigrationExtract::getCaseDate)
        ));

    var activities = new ArrayList<WorkProgrammeActivity>();
    var activityStatusData = new HashMap<WorkProgrammeActivity, ActivityStatusData>();
    var activityCommentData = new HashMap<WorkProgrammeActivity, ActivityCommentData>();

    for (var entry : wpByLicenceAndCase.entrySet()) {
      var licenceRef = entry.getKey();
      for (var caseEntry : entry.getValue().entrySet()) {
        var detail = detailsByCaseDate.get(licenceRef + "|" + caseEntry.getKey());
        if (detail == null) {
          continue;
        }
        var caseInstant = LocalDate.parse(caseEntry.getKey(), CASE_DATE_FORMAT)
            .atStartOfDay(ZoneOffset.UTC).toInstant();
        for (var wpRow : caseEntry.getValue()) {
          var activity = buildWorkProgrammeActivity(wpRow, detail, termsByDetailAndType);
          activities.add(activity);
          activityStatusData.put(activity, new ActivityStatusData(wpRow.getStatus(), caseInstant));
          activityCommentData.put(activity, new ActivityCommentData(wpRow.getComments(), caseInstant));
        }
      }
    }

    return new WorkProgrammeActivityMigrationData(activities, activityStatusData, activityCommentData);
  }

  private WorkProgrammeActivity buildWorkProgrammeActivity(
      CarbonStorageWorkProgrammeMigrationExtract wpRow,
      LicenceScheduleDetail detail,
      Map<LicenceScheduleDetail, Map<TermType, LicenceScheduleTerm>> termsByDetailAndType
  ) {
    var activity = new WorkProgrammeActivity();
    activity.setLicenceScheduleDetail(detail);
    activity.setLicenceSchedule(detail.getLicenceSchedule());
    activity.setDescription(wpRow.getDescription());
    activity.setOriginalEventId(wpRow.getUniqueEventId());
    WorkProgrammeActivityCategory.fromDisplayName(wpRow.getCategory())
        .ifPresent(category -> {
          activity.setCategory(category);
          if (category == WorkProgrammeActivityCategory.OTHER_ACTIVITY) {
            activity.setOtherCategoryName(wpRow.getOtherCategory());
          }
        });
    WorkProgrammeActivityCommitment.fromDisplayName(wpRow.getCommitment())
        .ifPresent(activity::setCommitment);
    TermType.fromDisplayName(wpRow.getTerm()).flatMap(termType -> Optional.ofNullable(
        termsByDetailAndType.getOrDefault(detail, Map.of()).get(termType)))
        .ifPresent(activity::setLicenceScheduleTerm);
    activity.setDateOption(wpRow.getDateOption());
    if (wpRow.getDateOption() == WorkProgrammeActivityDateOption.RELATIVE_DATE) {
      activity.setRelativeDuration(new ThreeFieldDuration(
          wpRow.getRelativeYears(), wpRow.getRelativeMonths(), wpRow.getRelativeDays()
      ));
    }
    return activity;
  }

  private List<WorkProgrammeActivityStatus> buildWorkProgrammeActivityStatuses(
      Map<WorkProgrammeActivity, ActivityStatusData> activityStatusData
  ) {
    var activityStatuses = new ArrayList<WorkProgrammeActivityStatus>();
    for (var entry : activityStatusData.entrySet()) {
      var data = entry.getValue();
      WorkProgrammeStatus.fromDisplayName(data.statusDisplayName())
          .ifPresent(workProgrammeStatus -> {
            var activityStatus = new WorkProgrammeActivityStatus();
            activityStatus.setScheduleEvent(entry.getKey());
            activityStatus.setStatus(workProgrammeStatus);
            activityStatus.setAppliedDatetime(data.caseInstant());
            activityStatuses.add(activityStatus);
          });
    }
    return activityStatuses;
  }

  private List<EventComment> buildWorkProgrammeActivityComments(
      Map<WorkProgrammeActivity, ActivityCommentData> activityCommentData
  ) {
    var eventComments = new ArrayList<EventComment>();
    for (var entry : activityCommentData.entrySet()) {
      var data = entry.getValue();
      if (data.comment() == null) {
        continue;
      }
      var eventComment = new EventComment();
      eventComment.setScheduleEvent(entry.getKey());
      eventComment.setComment(data.comment());
      eventComment.setStatus(EventCommentStatus.PUBLISHED);
      eventComment.setTimestamp(data.caseInstant());
      eventComments.add(eventComment);
    }
    return eventComments;
  }

  private LicenceStatus getLicenceStatusFromString(String status) {
    return switch (status) {
      case "Extant" -> LicenceStatus.EXTANT;
      case "Expired" -> LicenceStatus.EXPIRED;
      case "Revoked" -> LicenceStatus.REVOKED;
      case "Surrendered" -> LicenceStatus.SURRENDERED;
      case "Split" -> LicenceStatus.SPLIT_AND_TERMINATED;
      default -> null;
    };
  }
}
