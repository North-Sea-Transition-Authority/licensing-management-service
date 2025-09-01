package migration.carbonstorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.CarbonStorageLicenceLicenseeMigrationService;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.CarbonStorageLicenceMigrationExtract;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.CarbonStorageLicenceMigrationExtractRepository;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.CarbonStorageLicenceOrgMapping;
import uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage.CarbonStorageLicenceOrgMappingRepository;

@ExtendWith(MockitoExtension.class)
class CarbonStorageLicenceLicenseeMigrationServiceTest {

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private CarbonStorageLicenceMigrationExtractRepository carbonStorageLicenceMigrationExtractRepository;

  @Mock
  private CarbonStorageLicenceOrgMappingRepository carbonStorageLicenceOrgMappingRepository;

  @Captor
  private ArgumentCaptor<Collection<Licence>> licenceArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<LicenceResponsibleOrganisation>> licenceResponsibleOrganisationArgumentCaptor;

  @InjectMocks
  private CarbonStorageLicenceLicenseeMigrationService carbonStorageLicenceLicenseeMigrationService;

  @Test
  void testMigrate_savesLicencesAndLicensees() {
    CarbonStorageLicenceMigrationExtract extract = new CarbonStorageLicenceMigrationExtract();
    extract.setResponsibleOrgs("org1,org2");
    extract.setLicenceNumber("123");
    extract.setPrefix("CS");
    extract.setLicenceRef("CS123");

    when(carbonStorageLicenceMigrationExtractRepository.findAll()).thenReturn(List.of(extract));

    CarbonStorageLicenceOrgMapping mapping1 = new CarbonStorageLicenceOrgMapping();
    mapping1.setOrganisationUnitId(1);
    CarbonStorageLicenceOrgMapping mapping2 = new CarbonStorageLicenceOrgMapping();
    mapping2.setOrganisationUnitId(2);

    when(carbonStorageLicenceOrgMappingRepository.findByCsExtractResponsibleOrganisation("org1"))
        .thenReturn(mapping1);
    when(carbonStorageLicenceOrgMappingRepository.findByCsExtractResponsibleOrganisation("org2"))
        .thenReturn(mapping2);

    carbonStorageLicenceLicenseeMigrationService.migrate();

    verify(licenceService, times(1)).saveLicences(licenceArgumentCaptor.capture());
    verify(licenceResponsibleOrganisationService, times(1))
        .saveLicensees(licenceResponsibleOrganisationArgumentCaptor.capture());

    Collection<Licence> licences = licenceArgumentCaptor.getValue();
    List<LicenceResponsibleOrganisation> responsibleOrgs = licenceResponsibleOrganisationArgumentCaptor.getValue();

    assertEquals(1, licences.size());
    var licence1 = licences.iterator().next();
    assertEquals("CS", licence1.getPrefix());
    assertEquals("123", licence1.getLicenceNumber());
    assertEquals("CS123", licence1.getLicenceReference());
    assertEquals(LicenceType.CARBON_STORAGE, licence1.getType());

    assertEquals(2, responsibleOrgs.size());
    assertThat(responsibleOrgs)
        .extracting(
            LicenceResponsibleOrganisation::getResponsibleOrganisationId,
            LicenceResponsibleOrganisation::getLicence,
            LicenceResponsibleOrganisation::getManagedByLms
        ).containsExactlyInAnyOrder(
            tuple(1, licence1, true),
            tuple(2, licence1, true)
        );


  }
}
