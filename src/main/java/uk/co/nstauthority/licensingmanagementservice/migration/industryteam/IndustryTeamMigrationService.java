package uk.co.nstauthority.licensingmanagementservice.migration.industryteam;

import jakarta.transaction.Transactional;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.fivium.energyportal.serviceproviders.epmq.messages.ServiceProviderTeamDto;
import uk.co.fivium.energyportal.starter.serviceproviders.EnergyPortalAccountsMessagePublishingService;
import uk.co.fivium.energyportalapi.generated.types.OrganisationGroup;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationRepository;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@Service
public class IndustryTeamMigrationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(IndustryTeamMigrationService.class);

  private final LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final OrganisationGroupQueryService organisationGroupQueryService;
  private final TeamManagementService teamManagementService;
  private final EnergyPortalAccountsMessagePublishingService energyPortalAccountsMessagePublishingService;

  public IndustryTeamMigrationService(
      LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository,
      OrganisationUnitQueryService organisationUnitQueryService,
      OrganisationGroupQueryService organisationGroupQueryService,
      TeamManagementService teamManagementService,
      EnergyPortalAccountsMessagePublishingService energyPortalAccountsMessagePublishingService
  ) {
    this.licenceResponsibleOrganisationRepository = licenceResponsibleOrganisationRepository;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.organisationGroupQueryService = organisationGroupQueryService;
    this.teamManagementService = teamManagementService;
    this.energyPortalAccountsMessagePublishingService = energyPortalAccountsMessagePublishingService;
  }

  /**
   * Creates an organisation (industry) team for each unique organisation group that owns a responsible organisation
   * on a licence. Responsible organisation ids are organisation unit ids; each is resolved via EPA to its parent
   * organisation group, and one team is created per unique group. Teams that already exist for a group are skipped.
   *
   * @return the number of industry teams created
   */
  @Transactional
  public int migrateIndustryTeams() {
    var organisationUnitIds = licenceResponsibleOrganisationRepository.findAll().stream()
        .map(LicenceResponsibleOrganisation::getResponsibleOrganisationId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    if (organisationUnitIds.isEmpty()) {
      LOGGER.info("No responsible organisations found, no industry teams to migrate");
      return 0;
    }

    var organisationGroupIds = organisationUnitQueryService.findOrganisationGroupIdsByUnitIds(organisationUnitIds)
        .stream()
        .distinct()
        .toList();

    var organisationGroups = organisationGroupQueryService.getOrganisationGroupsByIds(organisationGroupIds);

    var createdCount = 0;
    for (var organisationGroup : organisationGroups) {
      if (createIndustryTeamIfNotExists(organisationGroup)) {
        createdCount++;
      }
    }

    LOGGER.info("Created {} industry teams from {} responsible organisation units", createdCount,
        organisationUnitIds.size());
    return createdCount;
  }

  private boolean createIndustryTeamIfNotExists(OrganisationGroup organisationGroup) {
    var scopeRef = TeamScopeReference.from(
        organisationGroup.getOrganisationGroupId().toString(),
        ScopeType.ORGANISATION_GROUP.name()
    );

    if (teamManagementService.doesScopedTeamWithReferenceExist(TeamType.ORGANISATION, scopeRef)) {
      return false;
    }

    var team = teamManagementService.createScopedTeam(
        organisationGroup.getName(),
        TeamType.ORGANISATION,
        scopeRef
    );

    var serviceProviderTeam = new ServiceProviderTeamDto(
        team.getId().toString(),
        team.getScopeId(),
        ScopeType.ORGANISATION_GROUP,
        team.getTeamType().name()
    );
    energyPortalAccountsMessagePublishingService.publishTeam(serviceProviderTeam);

    return true;
  }
}
