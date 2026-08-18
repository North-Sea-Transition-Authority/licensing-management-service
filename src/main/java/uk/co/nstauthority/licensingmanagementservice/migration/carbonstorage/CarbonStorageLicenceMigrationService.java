package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationResult;

@Service
public class CarbonStorageLicenceMigrationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CarbonStorageLicenceMigrationService.class);

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

  /**
   * Creates a licence, status and set of responsible organisations for each row of the carbon storage licence extract.
   *
   * <p>Extract rows whose licence reference already belongs to a licence are skipped, so the migration can safely be
   * re-run — without that guard a second run would mint new licence ids and insert a duplicate of every licence, which
   * nothing in the schema would reject.
   *
   * @return how many licences were migrated and how many extract rows were skipped
   */
  @Transactional
  public MigrationResult migrateLicences() {
    var carbonStorageLicenceMigrationExtracts = carbonStorageLicenceMigrationExtractRepository.findAll();

    var extractLicenceRefs = carbonStorageLicenceMigrationExtracts.stream()
        .map(CarbonStorageLicenceMigrationExtract::getLicenceRef)
        .toList();

    if (extractLicenceRefs.isEmpty()) {
      LOGGER.info("No carbon storage licence extract rows found, no licences to migrate");
      return MigrationResult.nothingToMigrate();
    }

    // Refs already taken, either by an earlier run of this migration or by a licence from another source. Extract rows
    // are added as they are consumed so that a ref repeated within the extract is only migrated once.
    var takenLicenceRefs = new HashSet<>(licenceService.getExistingLicenceReferences(extractLicenceRefs));

    var currentLicenceId = licenceService.getNextLicenceId();

    var licences = new ArrayList<Licence>();
    var licenceStatusTypeByLicenceId = new HashMap<Integer, LicenceStatusType>();
    var licenceResponsibleOrganisations = new ArrayList<LicenceResponsibleOrganisation>();
    var skippedCount = 0;

    for (var migrationExtract : carbonStorageLicenceMigrationExtracts) {
      if (!takenLicenceRefs.add(migrationExtract.getLicenceRef())) {
        LOGGER.info("Licence {} already exists, skipping extract row", migrationExtract.getLicenceRef());
        skippedCount++;
        continue;
      }

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

    LOGGER.info("Migrated {} carbon storage licences, skipped {} of {} extract rows", savedLicences.size(),
        skippedCount, carbonStorageLicenceMigrationExtracts.size());
    return new MigrationResult(savedLicences.size(), skippedCount);
  }

  /**
   * Builds the schedule, schedule details, start dates, terms and work programme activities for each carbon storage
   * licence from the term and work programme extracts.
   *
   * <p>Licences that already have a schedule are skipped, so the migration can safely be re-run — a licence has at most
   * one schedule, and a second unguarded run would give each licence a second schedule with its own set of details,
   * leaving two ACTIVE details per licence for the rest of the application to choose between.
   *
   * @return how many licences were given a schedule and how many were skipped
   * @throws MigrationPreconditionException if the extract tables are empty, which would otherwise create an empty
   *     schedule for every carbon storage licence and block a subsequent correct run
   */
  @Transactional
  public MigrationResult migrateSchedules() {
    var casesByLicence = buildCasesByLicence();

    if (casesByLicence.isEmpty()) {
      throw new MigrationPreconditionException(
          "No carbon storage term or work programme extract rows found. Load the extract tables before migrating " +
              "schedules, otherwise every carbon storage licence is given an empty schedule."
      );
    }

    var csLicences = licenceService.getAllLicences().stream()
        .filter(licence -> LicenceType.CARBON_STORAGE.equals(licence.getType()))
        .toList();

    var alreadyScheduledLicenceIds = licenceScheduleService.getIdsOfLicencesWithASchedule(csLicences);

    var licencesToSchedule = csLicences.stream()
        .filter(licence -> !alreadyScheduledLicenceIds.contains(licence.getId()))
        .toList();

    if (licencesToSchedule.isEmpty()) {
      LOGGER.info("All {} carbon storage licences already have a schedule, nothing to migrate", csLicences.size());
      return new MigrationResult(0, csLicences.size());
    }

    var licenceSchedules = buildLicenceSchedules(licencesToSchedule);
    var savedSchedules = new ArrayList<LicenceSchedule>();
    licenceScheduleService.saveLicenceSchedules(licenceSchedules).forEach(savedSchedules::add);

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

    var skippedCount = csLicences.size() - licencesToSchedule.size();
    LOGGER.info("Migrated schedules for {} carbon storage licences, skipped {} that already had one",
        savedSchedules.size(), skippedCount);
    return new MigrationResult(savedSchedules.size(), skippedCount);
  }

  private List<LicenceSchedule> buildLicenceSchedules(List<Licence> licences) {
    var licenceSchedules = new ArrayList<LicenceSchedule>();
    for (var licence : licences) {
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
