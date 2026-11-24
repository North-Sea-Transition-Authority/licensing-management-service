package uk.co.nstauthority.licensingmanagementservice.internalonly;

import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.fivium.energyportal.starter.accounts.EnergyPortalServiceAccessService;
import uk.co.fivium.energyportal.starter.serviceproviders.EnergyPortalAccountsMessagePublishingService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Service
@Profile({"development", "test-account-bootstrap"})
public class DataBootstrapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataBootstrapper.class);

  private final EnergyPortalUserService energyPortalUserService;
  private final EntityManager entityManager;
  private final TeamQueryService teamQueryService;
  private final EnergyPortalAccountsMessagePublishingService energyPortalAccountsMessagePublishingService;
  private final EnergyPortalServiceAccessService energyPortalServiceAccessService;

  public DataBootstrapper(EnergyPortalUserService energyPortalUserService,
                          EntityManager entityManager,
                          TeamQueryService teamQueryService,
                          EnergyPortalAccountsMessagePublishingService energyPortalAccountsMessagePublishingService,
                          EnergyPortalServiceAccessService energyPortalServiceAccessService
  ) {
    this.energyPortalUserService = energyPortalUserService;
    this.entityManager = entityManager;
    this.teamQueryService = teamQueryService;
    this.energyPortalAccountsMessagePublishingService = energyPortalAccountsMessagePublishingService;
    this.energyPortalServiceAccessService = energyPortalServiceAccessService;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void loadData() {

    LOGGER.info("Bootstrapping data...");

    var licenceManagementUserMap = new HashMap<Role, String>();
    licenceManagementUserMap.put(Role.MANAGE_TEAM, "administrator@lms.co.uk");
    licenceManagementUserMap.put(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM, "industry.manager@lms.co.uk");
    licenceManagementUserMap.put(Role.OFFLINE_LICENCE_ADMINISTRATOR, "licence.administrator@lms.co.uk");
    licenceManagementUserMap.put(Role.SCHEDULE_ADMINISTRATOR, "schedule.administrator@lms.co.uk");
    licenceManagementUserMap.put(Role.WORK_PROGRAMME_ADMINISTRATOR, "wp.administrator@lms.co.uk");
    licenceManagementUserMap.put(Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR, "wp.status.administrator@lms.co.uk");
    licenceManagementUserMap.put(Role.LICENCE_SCHEDULE_WORK_PROGRAMME_VIEWER, "licence.viewer@lms.co.uk");
    licenceManagementUserMap.put(Role.LICENCE_CONTACTS_MANAGER, "licence.administrator@lms.co.uk");

    var regulatorTeamUsers = new HashMap<TeamType, Map<Role, String>>();
    regulatorTeamUsers.put(TeamType.LICENCE_MANAGEMENT, licenceManagementUserMap);

    var offshoreProductionLicensingUserMap = new HashMap<Role, String>();
    offshoreProductionLicensingUserMap.put(Role.MANAGE_TEAM, "administrator@lms.co.uk");
    offshoreProductionLicensingUserMap.put(Role.CASE_MANAGER_OFFSHORE, "casemanager.offshore@lms.co.uk");
    offshoreProductionLicensingUserMap.put(Role.STEWARD_OFFSHORE, "steward.offshore@lms.co.uk");
    offshoreProductionLicensingUserMap.put(Role.DECISION_ISSUER_OFFSHORE, "decision.issuer.offshore@lms.co.uk");
    offshoreProductionLicensingUserMap.put(Role.CONTINUATION_REVIEWER_OFFSHORE, "continuation.reviewer.offshore@lms.co.uk");
    regulatorTeamUsers.put(TeamType.OFFSHORE_PRODUCTION_LICENSING, offshoreProductionLicensingUserMap);

    var carbonStorageLicensingUserMap = new HashMap<Role, String>();
    carbonStorageLicensingUserMap.put(Role.MANAGE_TEAM, "administrator@lms.co.uk");
    carbonStorageLicensingUserMap.put(Role.CASE_MANAGER_CARBON_STORAGE, "casemanager.carbonstorage@lms.co.uk");
    carbonStorageLicensingUserMap.put(Role.STEWARD_CARBON_STORAGE, "steward.carbonstorage@lms.co.uk");
    carbonStorageLicensingUserMap.put(Role.DECISION_ISSUER_CARBON_STORAGE, "decision.issuer.carbonstorage@lms.co.uk");
    regulatorTeamUsers.put(TeamType.CARBON_STORAGE_LICENSING, carbonStorageLicensingUserMap);

    var onshoreProductionLicensingUserMap = new HashMap<Role, String>();
    onshoreProductionLicensingUserMap.put(Role.MANAGE_TEAM, "administrator@lms.co.uk");
    onshoreProductionLicensingUserMap.put(Role.CASE_MANAGER_ONSHORE, "casemanager.onshore@lms.co.uk");
    onshoreProductionLicensingUserMap.put(Role.STEWARD_ONSHORE, "steward.onshore@lms.co.uk");
    onshoreProductionLicensingUserMap.put(Role.DECISION_ISSUER_ONSHORE, "decision.issuer.onshore@lms.co.uk");
    onshoreProductionLicensingUserMap.put(Role.CONTINUATION_REVIEWER_ONSHORE, "continuation.reviewer.onshore@lms.co.uk");
    regulatorTeamUsers.put(TeamType.ONSHORE_PRODUCTION_LICENSING, onshoreProductionLicensingUserMap);

    var regulationsLicensingUserMap = new HashMap<Role, String>();
    regulationsLicensingUserMap.put(Role.MANAGE_TEAM, "administrator@lms.co.uk");
    regulationsLicensingUserMap.put(Role.CONTINUATION_ISSUER, "continuation.issuer@lms.co.uk");
    regulationsLicensingUserMap.put(Role.DECISION_EXECUTOR, "decision.executor@lms.co.uk");
    regulatorTeamUsers.put(TeamType.REGULATIONS_LICENSING, regulationsLicensingUserMap);

    var industryTeams = List.of(
        new IndustryTeam("BP EXPLORATION", "50", industryTeamUsers("bp")),
        new IndustryTeam("ITHACA ENERGY INC.", "1403", industryTeamUsers("ithaca"))
    );

    var emailAddresses = new HashSet<String>();
    regulatorTeamUsers.values().forEach(roleUserMap -> emailAddresses.addAll(roleUserMap.values()));
    industryTeams.forEach(industryTeam -> emailAddresses.addAll(industryTeam.roleUserMap().values()));
    var usersByEmail = findUsersByEmail(emailAddresses);

    bootstrapRegulatorTeams(regulatorTeamUsers, usersByEmail);
    industryTeams.forEach(industryTeam -> bootstrapIndustryTeam(industryTeam, usersByEmail));
  }

  private Map<Role, String> industryTeamUsers(String emailPrefix) {
    var industryTeamUsers = new HashMap<Role, String>();
    industryTeamUsers.put(Role.MANAGE_TEAM, emailPrefix + ".administrator@lms.co.uk");
    industryTeamUsers.put(Role.LICENSEE_CONTACTS_MANAGER, emailPrefix + ".contacts.manager@lms.co.uk");
    industryTeamUsers.put(Role.APPLICATION_EDITOR, emailPrefix + ".editor@lms.co.uk");
    industryTeamUsers.put(Role.APPLICATION_SUBMITTER, emailPrefix + ".submitter@lms.co.uk");
    industryTeamUsers.put(Role.VIEW_ORGANISATION_LICENCES, emailPrefix + ".viewer@lms.co.uk");
    return industryTeamUsers;
  }

  private Map<String, EnergyPortalUserJson> findUsersByEmail(Set<String> emailAddresses) {
    var usersByEmail = new HashMap<String, EnergyPortalUserJson>();

    for (String emailAddress : emailAddresses) {
      var users = energyPortalUserService.findUsersByEmail(emailAddress, "Bootstrapping LMS test accounts for test environment");

      if (users.isEmpty()) {
        LOGGER.warn("No Energy Portal user found for email {} while bootstrapping, skipping", emailAddress);
        continue;
      }

      usersByEmail.put(emailAddress, users.getFirst());
    }

    return usersByEmail;
  }

  private void bootstrapIndustryTeam(IndustryTeam industryTeamToBootstrap,
                                     Map<String, EnergyPortalUserJson> usersByEmail) {
    var teamName = industryTeamToBootstrap.name();
    var organisationGroupId = industryTeamToBootstrap.organisationGroupId();

    var industryTeamCount = (Integer) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM lms.teams t " +
                "WHERE t.type = :type AND t.scope_type = :scopeType AND t.scope_id = :scopeId",
            Integer.class
        )
        .setParameter("type", TeamType.ORGANISATION.name())
        .setParameter("scopeType", ScopeType.ORGANISATION_GROUP.name())
        .setParameter("scopeId", organisationGroupId)
        .getSingleResult();

    if (industryTeamCount == 0) {
      LOGGER.info("Bootstrapping industry team {}", teamName);
      var team = new Team();
      team.setName(teamName);
      team.setTeamType(TeamType.ORGANISATION);
      team.setScopeType(ScopeType.ORGANISATION_GROUP.name());
      team.setScopeId(organisationGroupId);
      entityManager.persist(team);
      entityManager.flush();
    }

    var industryTeam = (Team) entityManager.createNativeQuery(
            "SELECT * FROM lms.teams t " +
                "WHERE t.type = :type AND t.scope_type = :scopeType AND t.scope_id = :scopeId",
            Team.class
        ).setParameter("type", TeamType.ORGANISATION.name())
        .setParameter("scopeType", ScopeType.ORGANISATION_GROUP.name())
        .setParameter("scopeId", organisationGroupId)
        .getSingleResult();

    bootstrapTeamRoles(industryTeam, teamName, industryTeamToBootstrap.roleUserMap(), usersByEmail);
  }

  private void bootstrapRegulatorTeams(Map<TeamType, Map<Role, String>> regulatorTeamUsers,
                                       Map<String, EnergyPortalUserJson> usersByEmail) {
    for (TeamType teamType : regulatorTeamUsers.keySet()) {
      var regulatorTeam = teamQueryService.getStaticTeam(teamType);
      bootstrapTeamRoles(regulatorTeam, teamType.getDisplayName(), regulatorTeamUsers.get(teamType), usersByEmail);
    }
  }

  private void bootstrapTeamRoles(Team team, String teamName, Map<Role, String> roleUserMap,
                                  Map<String, EnergyPortalUserJson> usersByEmail) {
    for (Role role : roleUserMap.keySet()) {
      var emailAddress = roleUserMap.get(role);
      var user = usersByEmail.get(emailAddress);

      if (user == null) {
        continue;
      }

      if (userHasRoleInTeam(team, role, user.webUserAccountId())) {
        LOGGER.info("User {} with role {} in team {} exists", emailAddress, role.getName(), teamName);
        continue;
      }

      LOGGER.info("Bootstrapping {} {} for user {}", teamName, role.getName(), emailAddress);
      createTeamRole(role, team, user);
    }
  }

  private boolean userHasRoleInTeam(Team team, Role role, Long wuaId) {
    var count = (Integer) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM lms.team_roles tr " +
                "WHERE tr.team_id = :teamId " +
                "AND tr.role = :role " +
                "AND tr.wua_id = :wuaId",
            Integer.class
        )
        .setParameter("teamId", team.getId())
        .setParameter("role", role.name())
        .setParameter("wuaId", wuaId)
        .getSingleResult();

    return count > 0;
  }

  private void createTeamRole(Role role, Team team, EnergyPortalUserJson userJson) {
    var wuaId = userJson.webUserAccountId();

    var teamRole = new TeamRole();
    teamRole.setTeam(team);
    teamRole.setRole(role);
    teamRole.setWuaId(wuaId);
    entityManager.persist(teamRole);
    entityManager.flush();

    energyPortalAccountsMessagePublishingService.publishUsersRolesForTeam(
        wuaId,
        team.getId().toString(),
        team.getTeamType().name(),
        Set.of(role.name())
    );

    energyPortalServiceAccessService.addUser(wuaId);
  }

  private record IndustryTeam(String name, String organisationGroupId, Map<Role, String> roleUserMap) {
  }
}