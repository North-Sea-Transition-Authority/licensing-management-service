package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.contact.LicenceContact;
import uk.co.nstauthority.licensingmanagementservice.licence.contact.LicenceContactRepository;

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

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private LicenceContactRepository licenceContactRepository;

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
  void getAllByResponsibleOrganisationIdIn() {
    var responsibleOrganisationIds = List.of(10, 20);

    licenceResponsibleOrganisationService.getAllByResponsibleOrganisationIdIn(responsibleOrganisationIds);

    verify(licenceResponsibleOrganisationRepository).findAllByResponsibleOrganisationIdIn(responsibleOrganisationIds);
  }

  @Test
  void getByLicenceIdAndResponsibleOrganisationIdOrThrow_whenFound_returnsIt() {
    var licensee = new LicenceResponsibleOrganisation();
    when(licenceResponsibleOrganisationRepository.findByLicence_IdAndResponsibleOrganisationId(1, 10))
        .thenReturn(Optional.of(licensee));

    assertThat(licenceResponsibleOrganisationService.getByLicenceIdAndResponsibleOrganisationIdOrThrow(1, 10))
        .isEqualTo(licensee);
  }

  @Test
  void getByLicenceIdAndResponsibleOrganisationIdOrThrow_whenNotFound_throws() {
    when(licenceResponsibleOrganisationRepository.findByLicence_IdAndResponsibleOrganisationId(1, 10))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> licenceResponsibleOrganisationService.getByLicenceIdAndResponsibleOrganisationIdOrThrow(1, 10))
        .isInstanceOf(LmsEntityNotFoundException.class);
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
  void saveLicenseesFromForm_deletesContactsForRemovedLicensees() {
    var licence = new Licence();

    var keptLicensee = new LicenceResponsibleOrganisation();
    keptLicensee.setResponsibleOrganisationId(1);
    keptLicensee.setLicence(licence);
    keptLicensee.setManagedByLms(true);

    var removedLicensee = new LicenceResponsibleOrganisation();
    removedLicensee.setResponsibleOrganisationId(2);
    removedLicensee.setLicence(licence);
    removedLicensee.setManagedByLms(true);

    when(licenceResponsibleOrganisationRepository.findAllByLicence(licence))
        .thenReturn(List.of(keptLicensee, removedLicensee));

    var removedContact = new LicenceContact();
    removedContact.setLicensee(removedLicensee);
    when(licenceContactRepository.findAllByLicenseeIn(List.of(removedLicensee)))
        .thenReturn(List.of(removedContact));

    licenceResponsibleOrganisationService.saveLicenseesFromForm(licence, List.of("1"));

    verify(licenceContactRepository).deleteAll(List.of(removedContact));
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

  @Test
  void getResponsibleOrganisationsByLicences_returnsMappedOrganisationUnitsPerLicence() {
    var licence = new Licence();
    licence.setId(1);

    var lro = new LicenceResponsibleOrganisation();
    lro.setLicence(licence);
    lro.setResponsibleOrganisationId(100);

    when(licenceResponsibleOrganisationRepository.findAllByLicenceIn(List.of(licence))).thenReturn(List.of(lro));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(100))).thenReturn(Map.of(100, "Org Name"));

    var result = licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(List.of(licence));

    assertThat(result.get(licence))
        .usingRecursiveComparison()
        .isEqualTo(List.of(new OrganisationUnit(100, "Org Name")));
  }

  @Test
  void getOrganisationUnitIdsFromLicenceOrgUnitMap_returnsIdsForMatchingLicence() {
    var licence = new Licence();
    licence.setId(1);
    var otherLicence = new Licence();
    otherLicence.setId(2);
    var map = Map.of(
        licence, List.of(new OrganisationUnit(100, "Org A")),
        otherLicence, List.of(new OrganisationUnit(200, "Org B"))
    );

    assertThat(licenceResponsibleOrganisationService.getOrganisationUnitIdsFromLicenceOrgUnitMap(map, licence))
        .containsExactly(100);
  }

  @Test
  void getOrgUnitToGroupIdMap_whenNullLicence_returnsEmptyMap() {
    var result = licenceResponsibleOrganisationService.getOrgUnitToGroupIdMap((Licence) null);
    assertThat(result).isEmpty();
  }

  @Test
  void getOrgUnitToGroupIdMap_whenLicenceHasResponsibleOrganisations_returnsGroupIdMap() {
    var licence = new Licence();
    licence.setId(1);

    var lro = new LicenceResponsibleOrganisation();
    lro.setLicence(licence);
    lro.setResponsibleOrganisationId(100);

    when(licenceResponsibleOrganisationRepository.findAllByLicenceIn(List.of(licence))).thenReturn(List.of(lro));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(100))).thenReturn(Map.of(100, "Org Name"));
    when(organisationUnitQueryService.findOrganisationGroupIdMapByUnitIds(List.of(100))).thenReturn(Map.of(100, 200));

    var result = licenceResponsibleOrganisationService.getOrgUnitToGroupIdMap(licence);

    assertThat(result).containsEntry(100, 200);
  }

  @Test
  void getOrgUnitToGroupIdMap_whenMapAndApplicationDetailsProvided_combinesOrgUnitIds() {
    var licence = new Licence();
    licence.setId(1);

    var responsibleOrganisations = Map.of(licence, List.of(new OrganisationUnit(100, "Org A")));

    var appDetail = mock(LicenceApplicationDetail.class);
    when(appDetail.getResponsibleOrganisationUnitId()).thenReturn(200);

    when(organisationUnitQueryService.findOrganisationGroupIdMapByUnitIds(List.of(100, 200)))
        .thenReturn(Map.of(100, 10, 200, 20));

    var result = licenceResponsibleOrganisationService.getOrgUnitToGroupIdMap(responsibleOrganisations, List.of(appDetail));

    assertThat(result).containsEntry(100, 10).containsEntry(200, 20);
  }

  @Test
  void getOrgUnitToGroupIdMap_whenApplicationDetailHasNullOrgUnitId_excludesFromIds() {
    var licence = new Licence();
    licence.setId(1);

    var responsibleOrganisations = Map.of(licence, List.of(new OrganisationUnit(100, "Org A")));

    var appDetail = mock(LicenceApplicationDetail.class);
    when(appDetail.getResponsibleOrganisationUnitId()).thenReturn(null);

    when(organisationUnitQueryService.findOrganisationGroupIdMapByUnitIds(List.of(100)))
        .thenReturn(Map.of(100, 10));

    var result = licenceResponsibleOrganisationService.getOrgUnitToGroupIdMap(responsibleOrganisations, List.of(appDetail));

    assertThat(result).containsOnlyKeys(100);
  }

  @Test
  void getOrgUnitToGroupIdMap_whenSameOrgUnitIdInBothSources_deduplicates() {
    var licence = new Licence();
    licence.setId(1);

    var responsibleOrganisations = Map.of(licence, List.of(new OrganisationUnit(100, "Org A")));

    var appDetail = mock(LicenceApplicationDetail.class);
    when(appDetail.getResponsibleOrganisationUnitId()).thenReturn(100);

    when(organisationUnitQueryService.findOrganisationGroupIdMapByUnitIds(List.of(100)))
        .thenReturn(Map.of(100, 10));

    var result = licenceResponsibleOrganisationService.getOrgUnitToGroupIdMap(responsibleOrganisations, List.of(appDetail));

    assertThat(result).containsOnlyKeys(100);
  }
}