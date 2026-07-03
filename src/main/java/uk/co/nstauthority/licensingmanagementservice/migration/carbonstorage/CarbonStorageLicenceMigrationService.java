package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;

@Service
public class CarbonStorageLicenceMigrationService {

  private final LicenceService licenceService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final LicenceScheduleService licenceScheduleService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceStartDateService licenceStartDateService;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;
  private final CarbonStorageLicenceMigrationExtractRepository carbonStorageLicenceMigrationExtractRepository;
  private final CarbonStorageLicenceOrgMappingRepository carbonStorageLicenceOrgMappingRepository;
  private final CarbonStorageStartDateMigrationRepository carbonStorageStartDateMigrationRepository;
  private final CarbonStorageTermMigrationRepository carbonStorageTermMigrationRepository;

  public CarbonStorageLicenceMigrationService(
      LicenceService licenceService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      LicenceScheduleService licenceScheduleService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceStartDateService licenceStartDateService,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      CarbonStorageLicenceMigrationExtractRepository carbonStorageLicenceMigrationExtractRepository,
      CarbonStorageLicenceOrgMappingRepository carbonStorageLicenceOrgMappingRepository,
      CarbonStorageStartDateMigrationRepository carbonStorageStartDateMigrationRepository,
      CarbonStorageTermMigrationRepository carbonStorageTermMigrationRepository
  ) {
    this.licenceService = licenceService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.licenceScheduleService = licenceScheduleService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceStartDateService = licenceStartDateService;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
    this.carbonStorageLicenceMigrationExtractRepository = carbonStorageLicenceMigrationExtractRepository;
    this.carbonStorageLicenceOrgMappingRepository = carbonStorageLicenceOrgMappingRepository;
    this.carbonStorageStartDateMigrationRepository = carbonStorageStartDateMigrationRepository;
    this.carbonStorageTermMigrationRepository = carbonStorageTermMigrationRepository;
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
    var csLicences = licenceService.getAllLicences().stream()
        .filter(licence -> LicenceType.CARBON_STORAGE.equals(licence.getType()))
        .toList();

    var licenceSchedules = new ArrayList<LicenceSchedule>();

    for (var licence : csLicences) {
      var licenceSchedule = new LicenceSchedule();
      licenceSchedule.setLicence(licence);
      licenceSchedules.add(licenceSchedule);
    }

    var savedLicenceSchedules = licenceScheduleService.saveLicenceSchedules(licenceSchedules);

    var licenceScheduleDetails = new ArrayList<LicenceScheduleDetail>();

    for (var licenceSchedule : savedLicenceSchedules) {
      var licenceScheduleDetail = new LicenceScheduleDetail();
      licenceScheduleDetail.setLicenceSchedule(licenceSchedule);
      licenceScheduleDetail.setStatus(LicenceScheduleDetailStatus.ACTIVE);
      licenceScheduleDetail.setCreatedInstant(Instant.now());
      licenceScheduleDetails.add(licenceScheduleDetail);
    }

    licenceScheduleDetailService.saveLicenceScheduleDetails(licenceScheduleDetails);

    var migrationStartDates = carbonStorageStartDateMigrationRepository.findAll();
    var licenceStartDates = new ArrayList<LicenceStartDate>();

    for (var migrationStartDate : migrationStartDates) {
      var licence = licenceService.findLicenceByReference(migrationStartDate.getLicenceRef());
      if (licence.isPresent()) {
        var licenceScheduleDetail = licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(
            licence.get(),
            LicenceScheduleDetailStatus.ACTIVE
        );
        var licenceStartDate = new LicenceStartDate();
        licenceStartDate.setLicenceScheduleDetail(licenceScheduleDetail);
        licenceStartDate.setStartDate(
            LocalDate.parse(migrationStartDate.getStartDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );
        licenceStartDates.add(licenceStartDate);
      }
    }

    licenceStartDateService.saveLicenceStartDates(licenceStartDates);

    var migrationTerms = carbonStorageTermMigrationRepository.findAll();
    var terms = new ArrayList<LicenceScheduleTerm>();

    for (var migrationTerm : migrationTerms) {
      var licence = licenceService.findLicenceByReference(migrationTerm.getLicenceRef());
      if (licence.isPresent()) {
        var licenceScheduleDetail = licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(
            licence.get(),
            LicenceScheduleDetailStatus.ACTIVE
        );
        var term = new LicenceScheduleTerm();
        term.setLicenceScheduleDetail(licenceScheduleDetail);
        term.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
        var termType = migrationTerm.getTerm().equals("Initial")
            ? TermType.INITIAL_CS
            : EnumUtils.getEnum(TermType.class, migrationTerm.getTerm().toUpperCase());
        term.setTermType(termType);
        term.setTermDuration(
            new ThreeFieldDuration(migrationTerm.getYears(), migrationTerm.getMonths(), migrationTerm.getDays())
        );
        terms.add(term);
      }
    }

    var savedTerms = licenceScheduleTermService.saveTerms(terms);

    for (var term : savedTerms) {
      var licenceScheduleDetail = term.getLicenceScheduleDetail();
      licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
    }
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
