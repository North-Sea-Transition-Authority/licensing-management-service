package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.energyportalapi.client.licence.licence.LicenceApi;
import uk.co.fivium.energyportalapi.generated.types.LicenceStatus;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationRepository;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class PearsLicenceRefreshServiceIntegrationTest {

  private static final Integer REFRESHED_LICENCE_ID = 1;
  private static final Integer OTHER_LICENCE_ID = 2;

  @MockitoBean
  private LicenceApi licenceApi;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;

  @Autowired
  private PearsLicenceRefreshService pearsLicenceRefreshService;

  @BeforeEach
  void setUp() {
    var refreshedLicence = persistLicence(REFRESHED_LICENCE_ID);
    var otherLicence = persistLicence(OTHER_LICENCE_ID);

    persistLicensee(refreshedLicence, 1);
    persistLicensee(refreshedLicence, 2);
    persistLicensee(otherLicence, 3);
  }

  @Test
  void refreshLicence_whenRefreshingSingleLicence_thenLeavesOtherLicencesResponsibleOrganisationsAlone() {
    when(licenceApi.searchLicencesById(eq(List.of(REFRESHED_LICENCE_ID)), any(), any(), any()))
        .thenReturn(List.of(portalLicence(REFRESHED_LICENCE_ID, List.of(1, 4))));

    pearsLicenceRefreshService.refreshLicence(REFRESHED_LICENCE_ID);

    assertThat(persistedLicensees()).containsExactlyInAnyOrder(
        tuple(REFRESHED_LICENCE_ID, 1),
        tuple(REFRESHED_LICENCE_ID, 4),
        tuple(OTHER_LICENCE_ID, 3)
    );
  }

  @Test
  void refreshLicence_whenEnergyPortalDoesNotHoldLicence_thenResponsibleOrganisationsRetained() {
    when(licenceApi.searchLicencesById(eq(List.of(REFRESHED_LICENCE_ID)), any(), any(), any())).thenReturn(List.of());

    var result = pearsLicenceRefreshService.refreshLicence(REFRESHED_LICENCE_ID);

    assertThat(result).isEmpty();
    assertThat(persistedLicensees()).containsExactlyInAnyOrder(
        tuple(REFRESHED_LICENCE_ID, 1),
        tuple(REFRESHED_LICENCE_ID, 2),
        tuple(OTHER_LICENCE_ID, 3)
    );
  }

  private List<org.assertj.core.groups.Tuple> persistedLicensees() {
    entityManager.flush();
    entityManager.clear();

    return licenceResponsibleOrganisationRepository.findAll().stream()
        .map(licensee -> tuple(licensee.getLicence().getId(), licensee.getResponsibleOrganisationId()))
        .toList();
  }

  private Licence persistLicence(Integer id) {
    var licence = new Licence();
    licence.setId(id);
    licence.setType(LicenceType.SEAWARD_PRODUCTION);
    licence.setLicenceNumber(id.toString());

    entityManager.persist(licence);
    return licence;
  }

  private void persistLicensee(Licence licence, Integer organisationUnitId) {
    var licensee = new LicenceResponsibleOrganisation();
    licensee.setLicence(licence);
    licensee.setResponsibleOrganisationId(organisationUnitId);
    licensee.setManagedByLms(false);

    entityManager.persist(licensee);
  }

  private uk.co.fivium.energyportalapi.generated.types.Licence portalLicence(
      Integer id,
      List<Integer> organisationUnitIds
  ) {
    var portalLicence = new uk.co.fivium.energyportalapi.generated.types.Licence();
    portalLicence.setId(id);
    portalLicence.setLicenceType("P");
    portalLicence.setLicenceNo(id);
    portalLicence.setLicenceStatus(LicenceStatus.EXTANT);
    portalLicence.setLicensees(organisationUnitIds.stream().map(organisationUnitId -> {
      var organisationUnit = new OrganisationUnit();
      organisationUnit.setOrganisationUnitId(organisationUnitId);
      return organisationUnit;
    }).toList());

    return portalLicence;
  }
}
