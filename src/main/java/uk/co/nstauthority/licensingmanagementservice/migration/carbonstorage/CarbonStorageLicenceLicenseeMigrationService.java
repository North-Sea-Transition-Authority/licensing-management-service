package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@Service
public class CarbonStorageLicenceLicenseeMigrationService {

  private final LicenceService licenceService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final CarbonStorageLicenceMigrationExtractRepository carbonStorageLicenceMigrationExtractRepository;
  private final CarbonStorageLicenceOrgMappingRepository carbonStorageLicenceOrgMappingRepository;

  public CarbonStorageLicenceLicenseeMigrationService(
      LicenceService licenceService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      CarbonStorageLicenceMigrationExtractRepository carbonStorageLicenceMigrationExtractRepository,
      CarbonStorageLicenceOrgMappingRepository carbonStorageLicenceOrgMappingRepository
  ) {
    this.licenceService = licenceService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.carbonStorageLicenceMigrationExtractRepository = carbonStorageLicenceMigrationExtractRepository;
    this.carbonStorageLicenceOrgMappingRepository = carbonStorageLicenceOrgMappingRepository;
  }

  @Transactional
  public void migrate() {
    var carbonStorageLicenceMigrationExtracts = carbonStorageLicenceMigrationExtractRepository.findAll();

    var currentLicenceId = 10000;

    var licences = new ArrayList<Licence>();
    var licenceResponsibleOrganisations = new ArrayList<LicenceResponsibleOrganisation>();

    for (var migrationExtract : carbonStorageLicenceMigrationExtracts) {
      var licence = new Licence();
      licence.setId(currentLicenceId++);
      licence.setType(LicenceType.CARBON_STORAGE);
      licence.setPrefix(migrationExtract.getPrefix());
      licence.setLicenceNumber(migrationExtract.getLicenceNumber());
      licence.setLicenceReference(migrationExtract.getLicenceRef());
      licences.add(licence);

      String[] organisations = migrationExtract.getResponsibleOrgs().split(",");
      for (var responsibleOrg : organisations) {

        var organisationUnitId = carbonStorageLicenceOrgMappingRepository.findByCsExtractResponsibleOrganisation(responsibleOrg)
            .getOrganisationUnitId();

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
}
