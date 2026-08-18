package uk.co.nstauthority.licensingmanagementservice.migration.industryteam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.fivium.energyportal.serviceproviders.epmq.messages.ServiceProviderTeamDto;
import uk.co.fivium.energyportal.starter.serviceproviders.EnergyPortalAccountsMessagePublishingService;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.generated.types.OrganisationGroup;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRepository;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class IndustryTeamMigrationServiceIntegrationTest {

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private EnergyPortalAccountsMessagePublishingService energyPortalAccountsMessagePublishingService;

  @Autowired
  private EntityManager em;

  @Autowired
  private TeamRepository teamRepository;

  @Autowired
  private IndustryTeamMigrationService industryTeamMigrationService;

  @Test
  void migrateIndustryTeams_createsOneTeamPerUniqueOrganisationGroup() {
    // two units resolve to the same group (500), a third to a different group (600)
    persistLicenceWithResponsibleOrganisation(20001, "IT001", 100);
    persistLicenceWithResponsibleOrganisation(20002, "IT002", 200);
    persistLicenceWithResponsibleOrganisation(20003, "IT003", 300);
    em.flush();

    when(organisationApi.getOrganisationUnitsByIds(any(), any(), any(), any()))
        .thenReturn(List.of(
            organisationUnitInGroup(100, 500),
            organisationUnitInGroup(200, 500),
            organisationUnitInGroup(300, 600)
        ));
    when(organisationApi.getAllOrganisationGroupsByIds(any(), any(), any()))
        .thenReturn(List.of(
            organisationGroup(500, "Group Alpha"),
            organisationGroup(600, "Group Beta")
        ));

    var result = industryTeamMigrationService.migrateIndustryTeams();

    assertThat(result.migrated()).isEqualTo(2);
    assertThat(result.skipped()).isZero();

    var teamAlpha = getOrganisationTeam("500").orElseThrow();
    assertThat(teamAlpha.getName()).isEqualTo("Group Alpha");
    assertThat(teamAlpha.getTeamType()).isEqualTo(TeamType.ORGANISATION);
    assertThat(teamAlpha.getScopeType()).isEqualTo(ScopeType.ORGANISATION_GROUP.name());

    var teamBeta = getOrganisationTeam("600").orElseThrow();
    assertThat(teamBeta.getName()).isEqualTo("Group Beta");
    assertThat(teamBeta.getScopeType()).isEqualTo(ScopeType.ORGANISATION_GROUP.name());

    verify(energyPortalAccountsMessagePublishingService, times(2))
        .publishTeam(any(ServiceProviderTeamDto.class));
  }

  @Test
  void migrateIndustryTeams_whenTeamAlreadyExistsForGroup_skipsThatGroup() {
    persistLicenceWithResponsibleOrganisation(20101, "IT101", 100);
    persistLicenceWithResponsibleOrganisation(20102, "IT102", 300);

    var existingTeam = new Team();
    existingTeam.setName("Existing Alpha");
    existingTeam.setTeamType(TeamType.ORGANISATION);
    existingTeam.setScopeType(ScopeType.ORGANISATION_GROUP.name());
    existingTeam.setScopeId("500");
    em.persist(existingTeam);
    em.flush();

    when(organisationApi.getOrganisationUnitsByIds(any(), any(), any(), any()))
        .thenReturn(List.of(
            organisationUnitInGroup(100, 500),
            organisationUnitInGroup(300, 600)
        ));
    when(organisationApi.getAllOrganisationGroupsByIds(any(), any(), any()))
        .thenReturn(List.of(
            organisationGroup(500, "Group Alpha"),
            organisationGroup(600, "Group Beta")
        ));

    var result = industryTeamMigrationService.migrateIndustryTeams();

    assertThat(result.migrated()).isEqualTo(1);
    assertThat(result.skipped()).isEqualTo(1);

    // existing team for group 500 is left untouched — not renamed or duplicated
    var teamAlpha = getOrganisationTeam("500").orElseThrow();
    assertThat(teamAlpha.getId()).isEqualTo(existingTeam.getId());
    assertThat(teamAlpha.getName()).isEqualTo("Existing Alpha");

    assertThat(getOrganisationTeam("600")).isPresent();

    verify(energyPortalAccountsMessagePublishingService, times(1))
        .publishTeam(any(ServiceProviderTeamDto.class));
  }

  @Test
  void migrateIndustryTeams_whenNoResponsibleOrganisations_createsNoTeamsAndReturnsZero() {
    var result = industryTeamMigrationService.migrateIndustryTeams();

    assertThat(result.migrated()).isZero();
    assertThat(result.skipped()).isZero();
    verify(organisationApi, never()).getOrganisationUnitsByIds(any(), any(), any(), any());
    verify(energyPortalAccountsMessagePublishingService, never()).publishTeam(any());
  }

  private void persistLicenceWithResponsibleOrganisation(int licenceId, String licenceRef, int organisationUnitId) {
    var licence = LicenceTestUtil.builder()
        .withId(licenceId)
        .withLicenceReference(licenceRef)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    em.persist(licence);

    var responsibleOrganisation = new LicenceResponsibleOrganisation();
    responsibleOrganisation.setLicence(licence);
    responsibleOrganisation.setResponsibleOrganisationId(organisationUnitId);
    responsibleOrganisation.setManagedByLms(true);
    em.persist(responsibleOrganisation);
  }

  private Optional<Team> getOrganisationTeam(String scopeId) {
    return teamRepository.findByTeamTypeAndScopeTypeAndScopeId(
        TeamType.ORGANISATION, ScopeType.ORGANISATION_GROUP.name(), scopeId);
  }

  private static OrganisationUnit organisationUnitInGroup(int organisationUnitId, int organisationGroupId) {
    var group = new OrganisationGroup();
    group.setOrganisationGroupId(organisationGroupId);

    var unit = new OrganisationUnit();
    unit.setOrganisationUnitId(organisationUnitId);
    unit.setOrganisationGroups(List.of(group));
    return unit;
  }

  private static OrganisationGroup organisationGroup(int organisationGroupId, String name) {
    var group = new OrganisationGroup();
    group.setOrganisationGroupId(organisationGroupId);
    group.setName(name);
    return group;
  }
}
