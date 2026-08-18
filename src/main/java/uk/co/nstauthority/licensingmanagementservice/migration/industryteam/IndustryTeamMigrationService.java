package uk.co.nstauthority.licensingmanagementservice.migration.industryteam;

import jakarta.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.fivium.energyportal.serviceproviders.epmq.messages.ServiceProviderTeamDto;
import uk.co.fivium.energyportal.starter.serviceproviders.EnergyPortalAccountsMessagePublishingService;
import uk.co.fivium.energyportalapi.generated.types.OrganisationGroup;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationRepository;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationResult;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;

@Service
public class IndustryTeamMigrationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(IndustryTeamMigrationService.class);

  static final String CONTACT_USER_LOOKUP_PURPOSE = "Validate PEARS contact being migrated into an industry team";
  static final List<Role> MIGRATED_CONTACT_ROLES = List.of(Role.MANAGE_TEAM);

  private final LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final OrganisationGroupQueryService organisationGroupQueryService;
  private final TeamManagementService teamManagementService;
  private final EnergyPortalAccountsMessagePublishingService energyPortalAccountsMessagePublishingService;
  private final PearsContactsMigrationExtractRepository pearsContactsMigrationExtractRepository;
  private final EnergyPortalUserService energyPortalUserService;
  private final UserDetailService userDetailService;

  public IndustryTeamMigrationService(
      LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository,
      OrganisationUnitQueryService organisationUnitQueryService,
      OrganisationGroupQueryService organisationGroupQueryService,
      TeamManagementService teamManagementService,
      EnergyPortalAccountsMessagePublishingService energyPortalAccountsMessagePublishingService,
      PearsContactsMigrationExtractRepository pearsContactsMigrationExtractRepository,
      EnergyPortalUserService energyPortalUserService,
      UserDetailService userDetailService
  ) {
    this.licenceResponsibleOrganisationRepository = licenceResponsibleOrganisationRepository;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.organisationGroupQueryService = organisationGroupQueryService;
    this.teamManagementService = teamManagementService;
    this.energyPortalAccountsMessagePublishingService = energyPortalAccountsMessagePublishingService;
    this.pearsContactsMigrationExtractRepository = pearsContactsMigrationExtractRepository;
    this.energyPortalUserService = energyPortalUserService;
    this.userDetailService = userDetailService;
  }

  /**
   * Creates an organisation (industry) team for each unique organisation group that owns a responsible organisation
   * on a licence. Responsible organisation ids are organisation unit ids; each is resolved via EPA to its parent
   * organisation group, and one team is created per unique group. Teams that already exist for a group are skipped, so
   * the migration can safely be re-run.
   *
   * @return how many industry teams were created and how many organisation groups already had one
   */
  @Transactional
  public MigrationResult migrateIndustryTeams() {
    var organisationUnitIds = licenceResponsibleOrganisationRepository.findAll().stream()
        .map(LicenceResponsibleOrganisation::getResponsibleOrganisationId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    if (organisationUnitIds.isEmpty()) {
      LOGGER.info("No responsible organisations found, no industry teams to migrate");
      return MigrationResult.nothingToMigrate();
    }

    var organisationGroupIds = organisationUnitQueryService.findOrganisationGroupIdsByUnitIds(organisationUnitIds)
        .stream()
        .distinct()
        .toList();

    var organisationGroups = organisationGroupQueryService.getOrganisationGroupsByIds(organisationGroupIds);

    var createdCount = 0;
    var skippedCount = 0;
    for (var organisationGroup : organisationGroups) {
      if (createIndustryTeamIfNotExists(organisationGroup)) {
        createdCount++;
      } else {
        skippedCount++;
      }
    }

    LOGGER.info("Created {} industry teams from {} responsible organisation units, skipped {} groups that already " +
        "had a team", createdCount, organisationUnitIds.size(), skippedCount);
    return new MigrationResult(createdCount, skippedCount);
  }

  /**
   * Adds the PEARS contacts held in {@code pears_contacts_migration_extract} to the industry team of the organisation
   * group they were extracted against, each with the roles in {@link #MIGRATED_CONTACT_ROLES}. Extract rows for an
   * organisation group that has no industry team are skipped, as are users who are already a member of the team (their
   * existing roles are left untouched) and users who no longer have a usable Energy Portal account.
   *
   * <p>{@link #migrateIndustryTeams()} must have been run first, as it is what creates the teams the contacts are
   * added to.
   *
   * @return how many users were added to industry teams and how many extracted contacts were skipped
   */
  @Transactional
  public MigrationResult migrateIndustryTeamUsers() {
    var wuaIdsByOrganisationGroupId = getExtractedWuaIdsByOrganisationGroupId();

    if (wuaIdsByOrganisationGroupId.isEmpty()) {
      LOGGER.info("No PEARS contacts found in the migration extract, no industry team users to migrate");
      return MigrationResult.nothingToMigrate();
    }

    var industryTeamsByOrganisationGroupId = getIndustryTeamsByOrganisationGroupId(
        wuaIdsByOrganisationGroupId.keySet());

    var contactUsers = energyPortalUserService.getEnergyPortalUserMap(
        wuaIdsByOrganisationGroupId.values().stream()
            .flatMap(Set::stream)
            .distinct()
            .map(WebUserAccountId::from)
            .toList(),
        CONTACT_USER_LOOKUP_PURPOSE
    );

    var instigatingUser = userDetailService.getUserDetail();

    var migratedCount = 0;
    var skippedCount = 0;
    for (var extractedGroup : wuaIdsByOrganisationGroupId.entrySet()) {
      var organisationGroupId = extractedGroup.getKey();
      var team = industryTeamsByOrganisationGroupId.get(organisationGroupId);

      if (team == null) {
        LOGGER.warn("No industry team exists for organisation group {}, skipping its {} extracted contacts",
            organisationGroupId, extractedGroup.getValue().size());
        skippedCount += extractedGroup.getValue().size();
        continue;
      }

      for (var wuaId : extractedGroup.getValue()) {
        if (!canMigrateContact(wuaId, organisationGroupId, contactUsers)) {
          skippedCount++;
          continue;
        }

        if (teamManagementService.isMemberOfTeam(team, wuaId)) {
          LOGGER.info("User {} is already a member of industry team {}, leaving their existing roles unchanged",
              wuaId, team.getId());
          skippedCount++;
          continue;
        }

        teamManagementService.setUserTeamRoles(wuaId, team, MIGRATED_CONTACT_ROLES, instigatingUser);
        migratedCount++;
      }
    }

    LOGGER.info("Added {} users to industry teams from {} extracted organisation groups, skipped {} contacts",
        migratedCount, wuaIdsByOrganisationGroupId.size(), skippedCount);
    return new MigrationResult(migratedCount, skippedCount);
  }

  /**
   * Groups the extracted contacts by organisation group id, preserving extract order and dropping duplicate and
   * incomplete rows.
   */
  private Map<Integer, Set<Long>> getExtractedWuaIdsByOrganisationGroupId() {
    return pearsContactsMigrationExtractRepository.findAll().stream()
        .filter(extract -> extract.getOrganisationGroupId() != null && extract.getWuaId() != null)
        .collect(Collectors.groupingBy(
            PearsContactsMigrationExtract::getOrganisationGroupId,
            LinkedHashMap::new,
            Collectors.mapping(
                extract -> extract.getWuaId().longValue(),
                Collectors.toCollection(LinkedHashSet::new)
            )
        ));
  }

  private Map<Integer, Team> getIndustryTeamsByOrganisationGroupId(Set<Integer> organisationGroupIds) {
    var scopeIds = organisationGroupIds.stream()
        .map(String::valueOf)
        .toList();

    return teamManagementService
        .getScopedTeams(TeamType.ORGANISATION, ScopeType.ORGANISATION_GROUP.name(), scopeIds)
        .stream()
        .collect(Collectors.toMap(team -> Integer.valueOf(team.getScopeId()), Function.identity()));
  }

  /**
   * Checks the contact up front rather than relying on {@link TeamManagementService#setUserTeamRoles} to reject them,
   * so that one unusable account is logged and skipped instead of rolling back the whole migration.
   */
  private boolean canMigrateContact(Long wuaId, Integer organisationGroupId,
                                    Map<WebUserAccountId, EnergyPortalUserJson> contactUsers) {
    var user = contactUsers.get(WebUserAccountId.from(wuaId));

    if (user == null) {
      LOGGER.warn("Extracted contact {} for organisation group {} does not exist in EPA, skipping", wuaId,
          organisationGroupId);
      return false;
    }

    if (user.sharedAccount()) {
      LOGGER.warn("Extracted contact {} for organisation group {} is a shared account, skipping", wuaId,
          organisationGroupId);
      return false;
    }

    if (!user.canLogin()) {
      LOGGER.warn("Extracted contact {} for organisation group {} is not an active account, skipping", wuaId,
          organisationGroupId);
      return false;
    }

    return true;
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
