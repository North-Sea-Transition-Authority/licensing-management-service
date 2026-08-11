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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
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
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusRepository;

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
  private final LicenceStatusRepository licenceStatusRepository;
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
      LicenceStatusRepository licenceStatusRepository,
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
    this.licenceStatusRepository = licenceStatusRepository;
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
    var licenceStatusTypeByLicenceId = new HashMap<Integer, LicenceStatusType>();
    var licenceResponsibleOrganisations = new ArrayList<LicenceResponsibleOrganisation>();

    for (var migrationExtract : carbonStorageLicenceMigrationExtracts) {
      var licence = new Licence();
      licence.setId(currentLicenceId++);
      licence.setType(LicenceType.CARBON_STORAGE);
      licence.setPrefix(LicenceType.CARBON_STORAGE.getPrefix());
      licence.setLicenceNumber(migrationExtract.getLicenceNumber());
      licence.setLicenceReference(migrationExtract.getLicenceRef());
      licences.add(licence);

      licenceStatusTypeByLicenceId.put(licence.getId(), getLicenceStatusFromString(migrationExtract.getStatus()));

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

    var savedLicences = (List<Licence>) licenceService.saveLicences(licences);

    var licenceStatuses = savedLicences.stream()
        .map(savedLicence -> {
          var licenceStatus = new LicenceStatus();
          licenceStatus.setLicence(savedLicence);
          licenceStatus.setStatus(licenceStatusTypeByLicenceId.get(savedLicence.getId()));
          licenceStatus.setStatusDate(LocalDate.now());
          return licenceStatus;
        })
        .toList();

    licenceStatusRepository.saveAll(licenceStatuses);
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

    var migratedActivities = buildWorkProgrammeActivities(detailsResult.detailsByCaseDate(), termsByDetailAndType);
    workProgrammeActivityService.saveWorkProgrammeActivities(
        migratedActivities.stream().map(MigratedActivity::activity).toList());
    workProgrammeActivityStatusRepository.saveAll(buildWorkProgrammeActivityStatuses(migratedActivities));
    eventCommentRepository.saveAll(buildWorkProgrammeActivityComments(migratedActivities));

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
        c -> caseKey(c.getLicenceRef(), c.getCaseDate()),
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
          detailsByCaseDate.put(caseKey(licenceRef, c.getCaseDate()), detail);
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
    var termsByLicenceAndCase = carbonStorageTermMigrationRepository.findAll().stream()
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
        var detail = detailsByCaseDate.get(caseKey(licenceRef, c.getCaseDate()));
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

  private List<MigratedActivity> buildWorkProgrammeActivities(
      Map<String, LicenceScheduleDetail> detailsByCaseDate,
      Map<LicenceScheduleDetail, Map<TermType, LicenceScheduleTerm>> termsByDetailAndType
  ) {
    var wpByLicenceAndCase = carbonStorageWorkProgrammeMigrationRepository.findAll().stream()
        .collect(Collectors.groupingBy(
            CarbonStorageWorkProgrammeMigrationExtract::getLicenceRef,
            Collectors.groupingBy(CarbonStorageWorkProgrammeMigrationExtract::getCaseDate)
        ));

    var migratedActivities = new ArrayList<MigratedActivity>();

    for (var entry : wpByLicenceAndCase.entrySet()) {
      var licenceRef = entry.getKey();
      for (var caseEntry : entry.getValue().entrySet()) {
        var detail = detailsByCaseDate.get(caseKey(licenceRef, caseEntry.getKey()));
        if (detail == null) {
          continue;
        }
        var caseInstant = LocalDate.parse(caseEntry.getKey(), CASE_DATE_FORMAT)
            .atStartOfDay(ZoneOffset.UTC).toInstant();
        for (var wpRow : caseEntry.getValue()) {
          var activity = buildWorkProgrammeActivity(wpRow, detail, termsByDetailAndType);
          migratedActivities.add(new MigratedActivity(activity, wpRow.getStatus(), wpRow.getComments(), caseInstant));
        }
      }
    }

    return migratedActivities;
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
      List<MigratedActivity> migratedActivities
  ) {
    var activityStatuses = new ArrayList<WorkProgrammeActivityStatus>();
    for (var migratedActivity : migratedActivities) {
      WorkProgrammeStatus.fromDisplayName(migratedActivity.statusDisplayName())
          .ifPresent(workProgrammeStatus -> {
            var activityStatus = new WorkProgrammeActivityStatus();
            activityStatus.setScheduleEvent(migratedActivity.activity());
            activityStatus.setStatus(workProgrammeStatus);
            activityStatus.setAppliedDatetime(migratedActivity.caseInstant());
            activityStatuses.add(activityStatus);
          });
    }
    return activityStatuses;
  }

  private List<EventComment> buildWorkProgrammeActivityComments(
      List<MigratedActivity> migratedActivities
  ) {
    var eventComments = new ArrayList<EventComment>();
    for (var migratedActivity : migratedActivities) {
      if (migratedActivity.comment() == null) {
        continue;
      }
      var eventComment = new EventComment();
      eventComment.setScheduleEvent(migratedActivity.activity());
      eventComment.setComment(migratedActivity.comment());
      eventComment.setStatus(EventCommentStatus.PUBLISHED);
      eventComment.setTimestamp(migratedActivity.caseInstant());
      eventComments.add(eventComment);
    }
    return eventComments;
  }

  private static String caseKey(String licenceRef, String caseDate) {
    return licenceRef + "|" + caseDate;
  }

  private LicenceStatusType getLicenceStatusFromString(String status) {
    return switch (status) {
      case "Extant" -> LicenceStatusType.EXTANT;
      case "Expired" -> LicenceStatusType.EXPIRED;
      case "Revoked" -> LicenceStatusType.REVOKED;
      case "Surrendered" -> LicenceStatusType.SURRENDERED;
      case "Split" -> LicenceStatusType.SPLIT_AND_TERMINATED;
      default -> throw new IllegalArgumentException("%s does not map to a valid status".formatted(status));
    };
  }
}
