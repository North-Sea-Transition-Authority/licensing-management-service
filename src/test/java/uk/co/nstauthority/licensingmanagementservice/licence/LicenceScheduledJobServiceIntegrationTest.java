package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.energyportalapi.client.licence.licence.LicenceApi;
import uk.co.fivium.energyportalapi.client.licence.licence.LicenceSearchFilter;
import uk.co.fivium.energyportalapi.generated.types.LicenceStatus;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusRepository;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class LicenceScheduledJobServiceIntegrationTest {

  @MockitoBean
  private LicenceApi licenceApi;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private LicenceRepository licenceRepository;

  @Autowired
  private LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;

  @Autowired
  private LicenceStatusRepository licenceStatusRepository;

  @Autowired
  private LicenceScheduledJobService licenceScheduledJobService;

  private Licence licence;
  private Licence licence2;

  private LicenceResponsibleOrganisation licenceResponsibleOrganisation;
  private LicenceResponsibleOrganisation licenceResponsibleOrganisation2;

  @Test
  void retrieveAndSavePearsLicences() {
    createDBBaseline();

    var orgUnit1 = new OrganisationUnit();
    orgUnit1.setOrganisationUnitId(1);

    var orgUnit2 = new OrganisationUnit();
    orgUnit2.setOrganisationUnitId(2);

    var orgUnit3 = new OrganisationUnit();
    orgUnit3.setOrganisationUnitId(3);

    var portalLicence = new uk.co.fivium.energyportalapi.generated.types.Licence();
    portalLicence.setId(1);
    portalLicence.setLicenceType("P");
    portalLicence.setLicenceSubType("Frontier");
    portalLicence.setLicenceNo(1);
    portalLicence.setLicensees(List.of(orgUnit1, orgUnit2));
    // licence 1's previously-recorded status is REVOKED (see createDBBaseline) - this run reports a changed status
    portalLicence.setLicenceStatus(LicenceStatus.EXTANT);

    var portalLicence2 = new uk.co.fivium.energyportalapi.generated.types.Licence();
    portalLicence2.setId(2);
    portalLicence2.setLicenceType("PEDL");
    portalLicence2.setLicenceSubType(null);
    portalLicence2.setLicenceNo(2);
    portalLicence2.setLicensees(List.of(orgUnit3));
    // licence 2's previously-recorded status is already EXTANT (see createDBBaseline) - this run reports no change
    portalLicence2.setLicenceStatus(LicenceStatus.EXTANT);

    var portalLicence3 = new uk.co.fivium.energyportalapi.generated.types.Licence();
    portalLicence3.setId(3);
    portalLicence3.setLicenceType("EXL");
    portalLicence3.setLicenceSubType(null);
    portalLicence3.setLicenceNo(3);
    portalLicence3.setLicensees(List.of(orgUnit3));
    portalLicence3.setLicenceStatus(LicenceStatus.EXTANT);

    var epaResult = List.of(portalLicence, portalLicence2, portalLicence3);

    when(licenceApi.searchLicences(any(LicenceSearchFilter.class), any(), any(), any())).thenReturn(epaResult);

    licenceScheduledJobService.retrieveAndSavePearsLicences();

    var licence3 = new Licence();
    licence3.setId(3);
    licence3.setType(LicenceType.LANDWARD_PRODUCTION);
    licence3.setSubtype(null);
    licence3.setLicenceNumber("3");
    licence3.setPrefix("EXL");

    var licencesResult = licenceRepository.findAll();

    assertThat(licencesResult)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(licence, licence2, licence3));

    // licence 3 is genuinely new (did not exist before this sync run) so a status history row should be recorded for it
    assertThat(licenceStatusRepository.findAllByLicence_IdIn(List.of(3)))
        .extracting(uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatus::getStatus)
        .containsExactly(uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType.EXTANT);

    // licence 1 already existed with a recorded status of REVOKED, and this run's portal status (EXTANT) differs,
    // so a new status row should be recorded in addition to the original one
    assertThat(licenceStatusRepository.findAllByLicence_IdIn(List.of(1)))
        .extracting(uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatus::getStatus)
        .containsExactlyInAnyOrder(
            uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType.REVOKED,
            uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType.EXTANT
        );

    // licence 2 already existed with a recorded status of EXTANT, and this run's portal status is unchanged (EXTANT),
    // so no new status row should be recorded
    assertThat(licenceStatusRepository.findAllByLicence_IdIn(List.of(2)))
        .extracting(uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatus::getStatus)
        .containsExactly(uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType.EXTANT);

    var responsibleOrganisation4 = new LicenceResponsibleOrganisation();
    responsibleOrganisation4.setResponsibleOrganisationId(3);
    responsibleOrganisation4.setLicence(licence2);
    responsibleOrganisation4.setManagedByLms(false);

    var responsibleOrganisation5 = new LicenceResponsibleOrganisation();
    responsibleOrganisation5.setResponsibleOrganisationId(3);
    responsibleOrganisation5.setLicence(licence3);
    responsibleOrganisation5.setManagedByLms(false);

    var responsibleOrganisationsResult = licenceResponsibleOrganisationRepository.findAll();

    assertThat(responsibleOrganisationsResult)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            licenceResponsibleOrganisation,
            licenceResponsibleOrganisation2,
            responsibleOrganisation4,
            responsibleOrganisation5
            )
        );
  }

  private void createDBBaseline() {
    licence = new Licence();
    licence.setId(1);
    licence.setType(LicenceType.SEAWARD_PRODUCTION);
    licence.setSubtype(LicenceSubtype.FRONTIER);
    licence.setLicenceNumber("1");

    entityManager.persist(licence);

    licence2 = new Licence();
    licence2.setId(2);
    licence2.setType(LicenceType.LANDWARD_PRODUCTION);
    licence2.setSubtype(null);
    licence2.setLicenceNumber("2");

    entityManager.persist(licence2);

    var licenceStatus = new uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatus();
    licenceStatus.setLicence(licence);
    licenceStatus.setStatus(LicenceStatusType.REVOKED);
    licenceStatus.setStatusDate(LocalDate.now());

    entityManager.persist(licenceStatus);

    var licenceStatus2 = new uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatus();
    licenceStatus2.setLicence(licence2);
    licenceStatus2.setStatus(LicenceStatusType.EXTANT);
    licenceStatus2.setStatusDate(LocalDate.now());

    entityManager.persist(licenceStatus2);

    // exists in pears and will be removed from lms and re-added
    licenceResponsibleOrganisation = new LicenceResponsibleOrganisation();
    licenceResponsibleOrganisation.setResponsibleOrganisationId(1);
    licenceResponsibleOrganisation.setLicence(licence);
    licenceResponsibleOrganisation.setManagedByLms(false);

    entityManager.persist(licenceResponsibleOrganisation);

    // exists in pears and will be removed from lms and re-added
    licenceResponsibleOrganisation2 = new LicenceResponsibleOrganisation();
    licenceResponsibleOrganisation2.setResponsibleOrganisationId(2);
    licenceResponsibleOrganisation2.setLicence(licence);
    licenceResponsibleOrganisation2.setManagedByLms(false);

    entityManager.persist(licenceResponsibleOrganisation2);

    // no longer exists in pears and will be removed from lms
    LicenceResponsibleOrganisation licenceResponsibleOrganisation3 = new LicenceResponsibleOrganisation();
    licenceResponsibleOrganisation3.setResponsibleOrganisationId(1);
    licenceResponsibleOrganisation3.setLicence(licence2);
    licenceResponsibleOrganisation3.setManagedByLms(false);

    entityManager.persist(licenceResponsibleOrganisation3);
  }
}