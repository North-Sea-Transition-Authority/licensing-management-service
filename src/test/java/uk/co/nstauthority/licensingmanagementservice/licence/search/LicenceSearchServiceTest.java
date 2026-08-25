package uk.co.nstauthority.licensingmanagementservice.licence.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupDto;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.TabbedLicencePageService;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRoleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ExtendWith(MockitoExtension.class)
class LicenceSearchServiceTest {

  private static final int ORG_UNIT_ID_ALPHA = 100;
  private static final int ORG_UNIT_ID_BETA = 101;
  private static final String ORG_UNIT_NAME_ALPHA = "Org Alpha";
  private static final String ORG_UNIT_NAME_BETA = "Org Beta";
  private static final int ORG_GROUP_ID = 5;
  private static final long USER_WUA_ID = 999L;

  private static final Map<Integer, LicenceStatusType> LICENCE_STATUSES_BY_ID = new HashMap<>();

  private static final Licence SEAWARD_PRODUCTION_LICENCE = buildLicence(1, LicenceType.SEAWARD_PRODUCTION, "P1", LicenceStatusType.EXTANT);
  private static final Licence CARBON_STORAGE_LICENCE = buildLicence(2, LicenceType.CARBON_STORAGE, "CS1", LicenceStatusType.EXTANT);
  private static final Licence CARBON_STORAGE_LICENCE_2 = buildLicence(5, LicenceType.CARBON_STORAGE, "CS2", LicenceStatusType.EXTANT);
  private static final Licence GAS_STORAGE_LICENCE = buildLicence(3, LicenceType.GAS_STORAGE, "GS1", LicenceStatusType.EXPIRED);
  private static final Licence LANDWARD_PRODUCTION_LICENCE = buildLicence(4, LicenceType.LANDWARD_PRODUCTION, "PEDL1", LicenceStatusType.SURRENDERED);
  private static final Licence UNKNOWN_TYPE_LICENCE = buildLicence(6, LicenceType.XL, "XL1", LicenceStatusType.EXTANT);

  private static final ServiceUserDetail serviceUserDetail = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(USER_WUA_ID)
      .build();

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private OrganisationGroupQueryService organisationGroupQueryService;

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private LicenceStatusService licenceStatusService;

  @Mock
  private TabbedLicencePageService tabbedLicencePageService;

  @InjectMocks
  private LicenceSearchService licenceSearchService;

  @BeforeEach
  void setUp() {
    lenient().when(teamQueryService.userIsInRegulatorTeam(USER_WUA_ID)).thenReturn(true);
    lenient().when(tabbedLicencePageService.getDefaultTabUrl(any(Licence.class)))
        .thenAnswer(invocation -> defaultTabUrl(invocation.getArgument(0)));
  }

  @Test
  void getSearchResultItems_NoFilters_ReturnsAllResults() {
    var licences = List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE);
    when(licenceService.getAllLicences()).thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences)).thenReturn(statusMapFor(licences));
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(licences))
        .thenReturn(List.of());

    var result = licenceSearchService.getSearchResultItems(new LicenceSearchFilterForm(), serviceUserDetail);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(SEAWARD_PRODUCTION_LICENCE, List.of()),
            buildSearchResultItem(CARBON_STORAGE_LICENCE, List.of())
        ));
  }

  @Test
  void getSearchResultItems_FilterByReference_ReturnsFilteredResults() {
    var licences = List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE, CARBON_STORAGE_LICENCE_2);
    when(licenceService.getAllLicences())
        .thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences)).thenReturn(statusMapFor(licences));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenceReference("s");

    var result = licenceSearchService.getSearchResultItems(filterForm, serviceUserDetail);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(CARBON_STORAGE_LICENCE, List.of()),
            buildSearchResultItem(CARBON_STORAGE_LICENCE_2, List.of())
        ));
  }

  @Test
  void getSearchResultItems_FilterByLicenceType_ReturnsFilteredResults() {
    var licences = List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE, CARBON_STORAGE_LICENCE_2, LANDWARD_PRODUCTION_LICENCE);
    when(licenceService.getAllLicences())
        .thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences)).thenReturn(statusMapFor(licences));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenceTypes(List.of(LicenceType.CARBON_STORAGE.name(), LicenceType.LANDWARD_PRODUCTION.name()));

    var result = licenceSearchService.getSearchResultItems(filterForm, serviceUserDetail);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(CARBON_STORAGE_LICENCE, List.of()),
            buildSearchResultItem(CARBON_STORAGE_LICENCE_2, List.of()),
            buildSearchResultItem(LANDWARD_PRODUCTION_LICENCE, List.of())
        ));
  }

  @Test
  void getSearchResultItems_FilterByOtherLicenceType_ReturnsLicencesWithUnknownLicenceTypes() {
    var licences = List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE, UNKNOWN_TYPE_LICENCE);
    when(licenceService.getAllLicences())
        .thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences)).thenReturn(statusMapFor(licences));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenceTypes(List.of(LicenceTypeFilterUtil.OTHER_OPTION));

    var result = licenceSearchService.getSearchResultItems(filterForm, serviceUserDetail);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(UNKNOWN_TYPE_LICENCE, List.of())
        ));
  }

  @Test
  void getSearchResultItems_FilterByLicenceTypeAndOther_ReturnsBothKnownAndUnknownLicenceTypes() {
    var licences = List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE, UNKNOWN_TYPE_LICENCE);
    when(licenceService.getAllLicences())
        .thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences)).thenReturn(statusMapFor(licences));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenceTypes(List.of(LicenceType.CARBON_STORAGE.name(), LicenceTypeFilterUtil.OTHER_OPTION));

    var result = licenceSearchService.getSearchResultItems(filterForm, serviceUserDetail);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(CARBON_STORAGE_LICENCE, List.of()),
            buildSearchResultItem(UNKNOWN_TYPE_LICENCE, List.of())
        ));
  }

  @Test
  void getSearchResultItems_FilterByLicenceStatus_ReturnsFilteredResults() {
    var licences = List.of(SEAWARD_PRODUCTION_LICENCE, GAS_STORAGE_LICENCE, LANDWARD_PRODUCTION_LICENCE);
    when(licenceService.getAllLicences())
        .thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences)).thenReturn(statusMapFor(licences));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenceStatuses(List.of(LicenceStatusType.EXPIRED.name(), LicenceStatusType.SURRENDERED.name()));

    var result = licenceSearchService.getSearchResultItems(filterForm, serviceUserDetail);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(GAS_STORAGE_LICENCE, List.of()),
            buildSearchResultItem(LANDWARD_PRODUCTION_LICENCE, List.of())
        ));
  }

  @Test
  void getSearchResultItems_FilterByLicensee_ReturnsFilteredResults() {
    var licences = List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE, GAS_STORAGE_LICENCE, LANDWARD_PRODUCTION_LICENCE);

    when(licenceService.getAllLicences()).thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences)).thenReturn(statusMapFor(licences));
    var lro1 = buildLicenceResponsibleOrganisation(SEAWARD_PRODUCTION_LICENCE, ORG_UNIT_ID_ALPHA);
    var lro3 = buildLicenceResponsibleOrganisation(GAS_STORAGE_LICENCE, ORG_UNIT_ID_BETA);
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(licences))
        .thenReturn(List.of(lro1, lro3));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(ORG_UNIT_ID_ALPHA, ORG_UNIT_ID_BETA)))
        .thenReturn(Map.of(ORG_UNIT_ID_ALPHA, ORG_UNIT_NAME_ALPHA, ORG_UNIT_ID_BETA, ORG_UNIT_NAME_BETA));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenseeOrgUnitId(ORG_UNIT_ID_ALPHA);

    var result = licenceSearchService.getSearchResultItems(filterForm, serviceUserDetail);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(SEAWARD_PRODUCTION_LICENCE, List.of(ORG_UNIT_NAME_ALPHA))
        ));
  }

  @Test
  void getSearchResultItems_FilterByLicenseeGroup_ReturnsFilteredResults() {
    var licences = List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE, GAS_STORAGE_LICENCE);

    when(licenceService.getAllLicences()).thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences)).thenReturn(statusMapFor(licences));
    var lro1 = buildLicenceResponsibleOrganisation(SEAWARD_PRODUCTION_LICENCE, ORG_UNIT_ID_ALPHA);
    var lro2 = buildLicenceResponsibleOrganisation(CARBON_STORAGE_LICENCE, ORG_UNIT_ID_BETA);
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(licences))
        .thenReturn(List.of(lro1, lro2));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(ORG_UNIT_ID_ALPHA, ORG_UNIT_ID_BETA)))
        .thenReturn(Map.of(ORG_UNIT_ID_ALPHA, ORG_UNIT_NAME_ALPHA, ORG_UNIT_ID_BETA, ORG_UNIT_NAME_BETA));
    when(organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(List.of(ORG_GROUP_ID)))
        .thenReturn(List.of(new OrganisationUnitJson(ORG_UNIT_ID_ALPHA, ORG_UNIT_NAME_ALPHA)));

    var filterForm = new LicenceSearchFilterForm();
    filterForm.setLicenseeOrgGroupId(ORG_GROUP_ID);

    var result = licenceSearchService.getSearchResultItems(filterForm, serviceUserDetail);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(SEAWARD_PRODUCTION_LICENCE, List.of(ORG_UNIT_NAME_ALPHA))
        ));
  }

  @Test
  void getSearchResultItems_whenNonRegulatorUserWithOrganisationAccess_returnsOnlyLicencesForUsersOrgUnits() {
    var licences = List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE);

    when(teamQueryService.userIsInRegulatorTeam(USER_WUA_ID)).thenReturn(false);
    when(licenceService.getAllLicences()).thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences)).thenReturn(statusMapFor(licences));
    var lro1 = buildLicenceResponsibleOrganisation(SEAWARD_PRODUCTION_LICENCE, ORG_UNIT_ID_ALPHA);
    var lro2 = buildLicenceResponsibleOrganisation(CARBON_STORAGE_LICENCE, ORG_UNIT_ID_BETA);
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(licences))
        .thenReturn(List.of(lro1, lro2));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(ORG_UNIT_ID_ALPHA, ORG_UNIT_ID_BETA)))
        .thenReturn(Map.of(ORG_UNIT_ID_ALPHA, ORG_UNIT_NAME_ALPHA, ORG_UNIT_ID_BETA, ORG_UNIT_NAME_BETA));

    var team = TeamTestUtil.newBuilder()
        .withTeamType(TeamType.ORGANISATION)
        .withScopeType(ScopeType.ORGANISATION_GROUP.name())
        .withScopeId(String.valueOf(ORG_GROUP_ID))
        .build();
    var teamRole = TeamRoleTestUtil.newBuilder()
        .withWuaId(USER_WUA_ID)
        .withTeam(team)
        .withRole(Role.VIEW_ORGANISATION_LICENCES)
        .build();
    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of(teamRole));
    when(organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(List.of(ORG_GROUP_ID)))
        .thenReturn(List.of(new OrganisationUnitJson(ORG_UNIT_ID_ALPHA, ORG_UNIT_NAME_ALPHA)));

    var result = licenceSearchService.getSearchResultItems(new LicenceSearchFilterForm(), serviceUserDetail);

    assertThat(result)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(List.of(
            buildSearchResultItem(SEAWARD_PRODUCTION_LICENCE, List.of(ORG_UNIT_NAME_ALPHA))
        ));
  }

  @Test
  void getSearchResultItems_whenNonRegulatorUserWithNoOrganisationAccess_returnsNoResults() {
    var licences = List.of(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE);

    when(teamQueryService.userIsInRegulatorTeam(USER_WUA_ID)).thenReturn(false);
    when(licenceService.getAllLicences()).thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences)).thenReturn(statusMapFor(licences));
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(licences))
        .thenReturn(List.of());
    when(teamQueryService.getTeamRolesForUser(USER_WUA_ID)).thenReturn(Set.of());

    var result = licenceSearchService.getSearchResultItems(new LicenceSearchFilterForm(), serviceUserDetail);

    assertThat(result).isEmpty();
    verifyNoInteractions(organisationGroupQueryService);
  }

  @Test
  void getPreselectedOrganisationUnit_whenIdProvided_returnsNameMap() {
    var orgUnitJson = new OrganisationUnitJson(1, "org name");

    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(anyList()))
        .thenReturn(Map.of(orgUnitJson.organisationUnitId(), orgUnitJson.getName()));

    var result = licenceSearchService.getPreselectedOrganisationUnit(1);

    assertThat(result).isEqualTo(Map.of(orgUnitJson.getId(), orgUnitJson.getName()));
  }

  @Test
  void getPreselectedOrganisationUnit_whenNullId_returnsEmptyMap() {
    var result = licenceSearchService.getPreselectedOrganisationUnit(null);

    assertThat(result).isEmpty();
    verifyNoInteractions(organisationUnitQueryService);
  }

  @Test
  void getPreselectedOrganisationGroup_whenGroupExists() {
    var groupDto = new OrganisationGroupDto();
    groupDto.setOrganisationGroupId(ORG_GROUP_ID);
    groupDto.setOrganisationGroupName("Group Alpha");

    when(organisationGroupQueryService.getOrganisationGroupById(ORG_GROUP_ID))
        .thenReturn(Optional.of(groupDto));

    var result = licenceSearchService.getPreselectedOrganisationGroup(ORG_GROUP_ID);

    assertThat(result).isEqualTo(Map.of(String.valueOf(ORG_GROUP_ID), "Group Alpha"));
  }

  @Test
  void getPreselectedOrganisationGroup_whenNoneSelected() {
    var result = licenceSearchService.getPreselectedOrganisationGroup(null);

    assertThat(result).isEmpty();
    verifyNoInteractions(organisationGroupQueryService);
  }

  @Test
  void getResponsibleOrganisationNamesByLicences_ReturnsCorrectMapping() {
    var lro1 = buildLicenceResponsibleOrganisation(SEAWARD_PRODUCTION_LICENCE, ORG_UNIT_ID_ALPHA);
    var lro2 = buildLicenceResponsibleOrganisation(SEAWARD_PRODUCTION_LICENCE, ORG_UNIT_ID_BETA);
    var lro3 = buildLicenceResponsibleOrganisation(CARBON_STORAGE_LICENCE, ORG_UNIT_ID_ALPHA);
    var lroList = List.of(lro1, lro2, lro3);

    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(ORG_UNIT_ID_ALPHA, ORG_UNIT_ID_BETA)))
        .thenReturn(Map.of(ORG_UNIT_ID_ALPHA, "Org 100", ORG_UNIT_ID_BETA, "Org 101"));

    Map<Licence, List<String>> result = licenceSearchService.getResponsibleOrganisationNamesByLicences(lroList);

    assertThat(result).containsOnlyKeys(SEAWARD_PRODUCTION_LICENCE, CARBON_STORAGE_LICENCE);
    assertThat(result.get(SEAWARD_PRODUCTION_LICENCE)).containsExactlyInAnyOrder("Org 100", "Org 101");
    assertThat(result.get(CARBON_STORAGE_LICENCE)).containsExactly("Org 100");
  }

  @Test
  void getResponsibleOrganisationNamesByLicences_whenEmptyInput_returnsEmptyMap() {
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of()))
        .thenReturn(Map.of());

    Map<Licence, List<String>> result = licenceSearchService.getResponsibleOrganisationNamesByLicences(List.of());

    assertThat(result).isEmpty();
  }

  private static Licence buildLicence(Integer id, LicenceType licenceType, String ref, LicenceStatusType status) {
    var licence = LicenceTestUtil.builder()
        .withId(id)
        .withLicenceType(licenceType)
        .withLicenceReference(ref)
        .build();
    LICENCE_STATUSES_BY_ID.put(id, status);
    return licence;
  }

  private static Map<Integer, LicenceStatusType> statusMapFor(List<Licence> licences) {
    var statusMap = new HashMap<Integer, LicenceStatusType>();
    licences.forEach(licence -> statusMap.put(licence.getId(), LICENCE_STATUSES_BY_ID.get(licence.getId())));
    return statusMap;
  }

  private static LicenceResponsibleOrganisation buildLicenceResponsibleOrganisation(Licence licence,
                                                                                     int responsibleOrganisationId) {
    var lro = new LicenceResponsibleOrganisation();
    lro.setLicence(licence);
    lro.setResponsibleOrganisationId(responsibleOrganisationId);
    return lro;
  }

  /** Stands in for whichever tab is the default under the active release phase. */
  private static String defaultTabUrl(Licence licence) {
    return "/licences/%d/default-tab".formatted(licence.getId());
  }

  private static SearchResultItem buildSearchResultItem(Licence licence, List<String> licensees) {
    return SearchResultItem.newBuilder()
        .withId(licence.getId().toString())
        .withLinkHeadingUrl(defaultTabUrl(licence))
        .withLinkHeadingText(licence.getLicenceReference())
        .withCaptionText(licence.getType().getDisplayName())
        .withDataItemRow(SummaryDataView.newBuilder()
            .addStringValue("Licensee(s)", String.join(", ", licensees))
            .addStringValue("Status", LICENCE_STATUSES_BY_ID.get(licence.getId()).getDisplayName())
            .build())
        .build();
  }
}
