package uk.co.nstauthority.licensingmanagementservice.internalonly;

import jakarta.persistence.EntityManager;
import java.util.HashMap;
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
  private static final String INDUSTRY_TEAM_NAME = "BP EXPLORATION";

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

    var regulatorTeamUsers = new HashMap<TeamType, Map<Role, String>>();

    var licenceManagementUserMap = new HashMap<Role, String>();
    licenceManagementUserMap.put(Role.MANAGE_TEAM, "administrator@lms.co.uk");
    licenceManagementUserMap.put(Role.CREATE_MANAGE_ANY_ORGANISATION_TEAM, "industry.manager@lms.co.uk");
    licenceManagementUserMap.put(Role.OFFLINE_LICENCE_ADMINISTRATOR, "licence.administrator@lms.co.uk");
    licenceManagementUserMap.put(Role.SCHEDULE_ADMINISTRATOR, "schedule.administrator@lms.co.uk");
    licenceManagementUserMap.put(Role.WORK_PROGRAMME_ADMINISTRATOR, "wp.administrator@lms.co.uk");
    licenceManagementUserMap.put(Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR, "wp.status.administrator@lms.co.uk");
    licenceManagementUserMap.put(Role.LICENCE_SCHEDULE_WORK_PROGRAMME_VIEWER, "licence.viewer@lms.co.uk");
    regulatorTeamUsers.put(TeamType.LICENCE_MANAGEMENT, licenceManagementUserMap);

    var offshoreProductionLicensingUserMap = new HashMap<Role, String>();
    offshoreProductionLicensingUserMap.put(Role.MANAGE_TEAM, "administrator@lms.co.uk");
    offshoreProductionLicensingUserMap.put(Role.CASE_MANAGER_NEW_VENTURES, "casemanager.nv@lms.co.uk");
    offshoreProductionLicensingUserMap.put(Role.CASE_MANAGER_OPERATIONS, "casemanager.ops@lms.co.uk");
    offshoreProductionLicensingUserMap.put(Role.STEWARD_NEW_VENTURES, "steward.nv@lms.co.uk");
    offshoreProductionLicensingUserMap.put(Role.STEWARD_OPERATIONS, "steward.ops@lms.co.uk");
    offshoreProductionLicensingUserMap.put(Role.DECISION_ISSUER_NEW_VENTURES, "decision.issuer.nv@lms.co.uk");
    offshoreProductionLicensingUserMap.put(Role.DECISION_ISSUER_OPERATIONS, "decision.issuer.ops@lms.co.uk");
    offshoreProductionLicensingUserMap.put(Role.CONTINUATION_REVIEWER_NEW_VENTURES, "continuation.reviewer.nv@lms.co.uk");
    offshoreProductionLicensingUserMap.put(Role.CONTINUATION_REVIEWER_OPERATIONS, "continuation.reviewer.ops@lms.co.uk");
    regulatorTeamUsers.put(TeamType.OFFSHORE_PRODUCTION_LICENSING, offshoreProductionLicensingUserMap);

    var carbonStorageLicensingUserMap = new HashMap<Role, String>();
    carbonStorageLicensingUserMap.put(Role.MANAGE_TEAM, "administrator@lms.co.uk");
    carbonStorageLicensingUserMap.put(Role.CASE_MANAGER_CS_NEW_VENTURES, "casemanager.cs.nv@lms.co.uk");
    carbonStorageLicensingUserMap.put(Role.STEWARD_CS_NEW_VENTURES, "steward.cs.nv@lms.co.uk");
    carbonStorageLicensingUserMap.put(Role.DECISION_ISSUER_CS_NEW_VENTURES, "decision.issuer.cs.nv@lms.co.uk");
    carbonStorageLicensingUserMap.put(Role.CASE_MANAGER_CS_CTS, "casemanager.cs.cts@lms.co.uk");
    carbonStorageLicensingUserMap.put(Role.STEWARD_CS_CTS, "steward.cs.cts@lms.co.uk");
    carbonStorageLicensingUserMap.put(Role.DECISION_ISSUER_CS_CTS, "decision.issuer.cs.cts@lms.co.uk");
    regulatorTeamUsers.put(TeamType.CARBON_STORAGE_LICENSING, carbonStorageLicensingUserMap);

    var onshoreProductionLicensingUserMap = new HashMap<Role, String>();
    onshoreProductionLicensingUserMap.put(Role.MANAGE_TEAM, "administrator@lms.co.uk");
    onshoreProductionLicensingUserMap.put(Role.CASE_MANAGER_ONSHORE, "casemanager.onshore@lms.co.uk");
    onshoreProductionLicensingUserMap.put(Role.STEWARD_ONSHORE, "steward.onshore@lms.co.uk");
    onshoreProductionLicensingUserMap.put(Role.DECISION_ISSUER_ONSHORE, "decision.issuer.onshore@lms.co.uk");
    regulatorTeamUsers.put(TeamType.ONSHORE_PRODUCTION_LICENSING, onshoreProductionLicensingUserMap);

    var regulationsLicensingUserMap = new HashMap<Role, String>();
    regulationsLicensingUserMap.put(Role.MANAGE_TEAM, "administrator@lms.co.uk");
    regulationsLicensingUserMap.put(Role.CONTINUATION_ISSUER, "continuation.issuer@lms.co.uk");
    regulationsLicensingUserMap.put(Role.DECISION_EXECUTOR, "decision.executor@lms.co.uk");
    regulatorTeamUsers.put(TeamType.REGULATIONS_LICENSING, regulationsLicensingUserMap);

    bootstrapRegulatorTeams(regulatorTeamUsers);

    var industryTeamUsers = new HashMap<Role, String>();
    industryTeamUsers.put(Role.MANAGE_TEAM, "bp.administrator@lms.co.uk");
    industryTeamUsers.put(Role.APPLICATION_EDITOR, "bp.editor@lms.co.uk");
    industryTeamUsers.put(Role.APPLICATION_SUBMITTER, "bp.submitter@lms.co.uk");
    industryTeamUsers.put(Role.VIEW_ORGANISATION_LICENCES, "bp.viewer@lms.co.uk");
    bootstrapIndustryTeam(industryTeamUsers);
  }

  private void bootstrapIndustryTeam(Map<Role, String> industryTeamUsers) {

    var industryTeamCount = (Integer) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM lms.teams t WHERE t.type = :type AND t.name = :name",
            Integer.class
        )
        .setParameter("type", TeamType.ORGANISATION.name())
        .setParameter("name", INDUSTRY_TEAM_NAME)
        .getSingleResult();

    if (industryTeamCount == 0) {
      LOGGER.info("Bootstrapping industry team");
      var team = new Team();
      team.setName(INDUSTRY_TEAM_NAME);
      team.setTeamType(TeamType.ORGANISATION);
      team.setScopeType(ScopeType.ORGANISATION_GROUP.name());
      team.setScopeId("50");
      entityManager.persist(team);
      entityManager.flush();
    }

    var industryTeam = (Team) entityManager.createNativeQuery(
            "SELECT * FROM lms.teams t WHERE type = :type AND t.name = :name",
            Team.class
        ).setParameter("type", TeamType.ORGANISATION.name())
        .setParameter("name", INDUSTRY_TEAM_NAME)
        .getSingleResult();

    var industryTeamUserRoles = entityManager.createNativeQuery(
            "SELECT tr.role FROM lms.team_roles tr " +
                "JOIN lms.teams t ON t.id = tr.team_id " +
                "WHERE t.type = :type " +
                "AND t.name = :name"
        )
        .setParameter("type", TeamType.ORGANISATION.name())
        .setParameter("name", INDUSTRY_TEAM_NAME)
        .getResultList();

    for (Role role : industryTeamUsers.keySet()) {
      if (industryTeamUserRoles.contains(role.name())) {
        LOGGER.info("Industry team member with role {} exists", role.getName());
        continue;
      }

      LOGGER.info("Bootstrapping industry {}", role.getName());
      var industryUser = findFirstUserByEmail(role, industryTeamUsers.get(role));

      createTeamRole(role, industryTeam, industryUser);
    }
  }

  private void bootstrapRegulatorTeams(Map<TeamType, Map<Role, String>> regulatorTeamUsers) {
    for (TeamType teamType : regulatorTeamUsers.keySet()) {
      var regulatorRoles = entityManager.createNativeQuery(
              "SELECT tr.role FROM lms.team_roles tr " +
                  "JOIN lms.teams t ON t.id = tr.team_id " +
                  "WHERE t.type = :type ")
          .setParameter("type", teamType.name())
          .getResultList();

      var roleUserMap = regulatorTeamUsers.get(teamType);
      for (Role role: roleUserMap.keySet()) {
        if (regulatorRoles.contains(role.name())) {
          LOGGER.info("Regulator team member with role {} in team {} exists", role.getName(), teamType.getDisplayName());
          continue;
        }

        LOGGER.info("Bootstrapping {} {}", teamType.getDisplayName(), role.getName());

        var regulatorUser = findFirstUserByEmail(role, roleUserMap.get(role));
        var regulatorTeam = teamQueryService.getStaticTeam(teamType);

        createTeamRole(role, regulatorTeam, regulatorUser);
      }
    }

  }

  private EnergyPortalUserJson findFirstUserByEmail(Role role, String emailAddress) {
    return energyPortalUserService.findUsersByEmail(
            emailAddress,
            String.format("Bootstrapping LMS %s for test environment", role.getName())
        )
        .getFirst();
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
}