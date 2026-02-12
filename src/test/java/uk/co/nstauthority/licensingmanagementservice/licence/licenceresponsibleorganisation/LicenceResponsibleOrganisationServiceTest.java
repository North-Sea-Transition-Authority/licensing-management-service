package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;

@ExtendWith(MockitoExtension.class)
class LicenceResponsibleOrganisationServiceTest {

  @Captor
  private ArgumentCaptor<List<LicenceResponsibleOrganisation>> organisationCaptor;

  @Mock
  private LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;

  @Mock
  private PearsResponsibleOrganisationRefreshService pearsResponsibleOrganisationRefreshService;

  @Mock
  private ApplicationAccessService applicationAccessService;

  @Mock
  private LicenceOrganisationService licenceOrganisationService;

  @InjectMocks
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  private ServiceUserDetail organisationUser;

  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  @Test
  void getAllByLicence() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var licence = new Licence();

    licenceResponsibleOrganisationService.getAllByLicence(licence);

    verify(licenceResponsibleOrganisationRepository).findAllByLicence(licence);
  }

  @Test
  void refreshPearsResponsibleOrganisations() {
    var licence = new Licence();
    licence.setId(1);

    var licence2 = new Licence();
    licence2.setId(2);

    var licenceList = List.of(licence, licence2);

    var licenceMap = Map.of(
        1, List.of(1, 2),
        2, List.of(1, 3)
    );

    licenceResponsibleOrganisationService.refreshPearsResponsibleOrganisations(licenceList, licenceMap);

    verify(pearsResponsibleOrganisationRefreshService).saveResponsibleOrganisationsForLicences(licenceList, licenceMap);
    verify(pearsResponsibleOrganisationRefreshService).deleteRemovedResponsibleOrganisationsForLicences(licenceMap);

  }

  @Test
  void saveLicenseesFromForm() {
    var licence = new Licence();

    var orgs = List.of("1", "2");

    var responsibleOrganisation = new LicenceResponsibleOrganisation();
    responsibleOrganisation.setResponsibleOrganisationId(1);
    responsibleOrganisation.setLicence(licence);
    responsibleOrganisation.setManagedByLms(true);

    var responsibleOrganisation2 = new LicenceResponsibleOrganisation();
    responsibleOrganisation2.setResponsibleOrganisationId(2);
    responsibleOrganisation2.setLicence(licence);
    responsibleOrganisation2.setManagedByLms(true);

    var expectedResult = List.of(responsibleOrganisation, responsibleOrganisation2);

    licenceResponsibleOrganisationService.saveLicenseesFromForm(licence, orgs);

    verify(licenceResponsibleOrganisationRepository).saveAll(organisationCaptor.capture());

    assertThat(organisationCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(expectedResult);
  }

  @Test
  void saveLicenseesFromForm_addAndDelete() {
    var licence = new Licence();

    var orgs = List.of("1", "3");

    var responsibleOrganisation = new LicenceResponsibleOrganisation();
    responsibleOrganisation.setResponsibleOrganisationId(1);
    responsibleOrganisation.setLicence(licence);
    responsibleOrganisation.setManagedByLms(true);

    var responsibleOrganisation2 = new LicenceResponsibleOrganisation();
    responsibleOrganisation2.setResponsibleOrganisationId(2);
    responsibleOrganisation2.setLicence(licence);
    responsibleOrganisation2.setManagedByLms(true);

    var responsibleOrganisation3 = new LicenceResponsibleOrganisation();
    responsibleOrganisation3.setResponsibleOrganisationId(3);
    responsibleOrganisation3.setLicence(licence);
    responsibleOrganisation3.setManagedByLms(true);

    when(licenceResponsibleOrganisationRepository.findAllByLicence(licence)).thenReturn(List.of(responsibleOrganisation, responsibleOrganisation2));

    var expectedDelete = List.of(responsibleOrganisation2);

    var expectedSave = List.of(responsibleOrganisation3);

    licenceResponsibleOrganisationService.saveLicenseesFromForm(licence, orgs);

    verify(licenceResponsibleOrganisationRepository).deleteAll(expectedDelete);

    verify(licenceResponsibleOrganisationRepository).saveAll(organisationCaptor.capture());

    assertThat(organisationCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(expectedSave);
  }

  @Test
  void getResponsibleOrgUnitOptions_returnsOnlyMatchingOrgUnitsWithValidRoles() {
    Licence licence = new Licence();

    var responsibleOrganisation = new LicenceResponsibleOrganisation();
    responsibleOrganisation.setResponsibleOrganisationId(1);
    responsibleOrganisation.setLicence(licence);
    responsibleOrganisation.setManagedByLms(true);

    var responsibleOrganisation2 = new LicenceResponsibleOrganisation();
    responsibleOrganisation2.setResponsibleOrganisationId(2);
    responsibleOrganisation2.setLicence(licence);
    responsibleOrganisation2.setManagedByLms(true);

    when(licenceResponsibleOrganisationRepository.findAllByLicence(licence)).thenReturn(List.of(responsibleOrganisation, responsibleOrganisation2));

    OrganisationUnitJson ou1 = new OrganisationUnitJson(2, "Org Two");
    OrganisationUnitJson ou2 = new OrganisationUnitJson(3, "Org Three");
    when(applicationAccessService.userHasEditorOrSubmitterRoleInOrganisationGroup(organisationUser)).thenReturn(true);
    when(licenceOrganisationService.getUsersOrgUnits(organisationUser)).thenReturn(List.of(ou1, ou2));

    var result = licenceResponsibleOrganisationService.getResponsibleOrgUnitOptionsWithValidRoles(licence, organisationUser);

    assertThat(result)
        .hasSize(1)
        .containsEntry("2", "Org Two");
  }

  @Test
  void getResponsibleOrgUnitOptions_WithValidRoles_returnsEmptyWhenNoMatches() {
    Licence licence = new Licence();

    OrganisationUnitJson ou1 = new OrganisationUnitJson(2, "Org Two");
    OrganisationUnitJson ou2 = new OrganisationUnitJson(3, "Org Three");
    when(licenceOrganisationService.getUsersOrgUnits(organisationUser)).thenReturn(List.of(ou1, ou2));

    var result = licenceResponsibleOrganisationService.getResponsibleOrgUnitOptionsWithValidRoles(licence, organisationUser);

    assertThat(result).isEmpty();
  }
}