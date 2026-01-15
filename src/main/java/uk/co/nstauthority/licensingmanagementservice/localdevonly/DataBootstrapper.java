package uk.co.nstauthority.licensingmanagementservice.localdevonly;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Service
@Profile("development")
public class DataBootstrapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataBootstrapper.class);
  private static final String INDUSTRY_TEAM_NAME = "BP EXPLORATION";

  private final EnergyPortalUserService energyPortalUserService;
  private final EntityManager entityManager;
  private final TeamQueryService teamQueryService;

  public DataBootstrapper(EnergyPortalUserService energyPortalUserService,
                          EntityManager entityManager,
                          TeamQueryService teamQueryService) {
    this.energyPortalUserService = energyPortalUserService;
    this.entityManager = entityManager;
    this.teamQueryService = teamQueryService;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void loadData() {
    List<TeamType> regulatorTeams = List.of(TeamType.LICENCE_MANAGEMENT);
    regulatorTeams.forEach(this::bootstrapRegulatorTeam);

    bootstrapIndustryTeam();
  }

  private void bootstrapIndustryTeam() {

    Integer industryTeamCount = (Integer) entityManager.createNativeQuery(
        "SELECT COUNT(*) FROM lms.teams t WHERE t.type = :type AND t.name = :name",
        Integer.class
    ).setParameter("type", TeamType.ORGANISATION.name()).setParameter("name", INDUSTRY_TEAM_NAME).getSingleResult();

    if (industryTeamCount == 0) {
      LOGGER.info("Bootstrapping industry team");
      var team = new Team();
      team.setName(INDUSTRY_TEAM_NAME);
      team.setTeamType(TeamType.ORGANISATION);
      team.setScopeType(TeamType.ORGANISATION.name());
      team.setScopeId("50");
      entityManager.persist(team);
      entityManager.flush();
    }

    Integer industryTeamUserCount = (Integer) entityManager.createNativeQuery(
        "SELECT COUNT(*) FROM lms.team_roles tr " +
            "JOIN lms.teams t ON t.id = tr.team_id " +
            "WHERE t.type = :type " +
            "AND t.name = :name",
        Integer.class
    ).setParameter("type", TeamType.ORGANISATION.name()).setParameter("name", INDUSTRY_TEAM_NAME).getSingleResult();

    if (industryTeamUserCount == 0) {
      LOGGER.info("Bootstrapping industry users");

      var industryAdmin = energyPortalUserService.findUsersByEmail(
              "bp.administrator@lms.co.uk",
              "Bootstrapping LMS BP admin for local dev")
          .getFirst();

      var teamRole = new TeamRole();

      Team industryTeam = (Team) entityManager.createNativeQuery(
          "SELECT * FROM lms.teams t WHERE type = :type AND t.name = :name",
          Team.class
      ).setParameter("type", TeamType.ORGANISATION.name()).setParameter("name", INDUSTRY_TEAM_NAME).getSingleResult();
      teamRole.setTeam(industryTeam);
      teamRole.setRole(Role.MANAGE_TEAM);
      teamRole.setWuaId(industryAdmin.webUserAccountId());
      entityManager.persist(teamRole);
      entityManager.flush();
    }
  }

  private void bootstrapRegulatorTeam(TeamType teamType) {

    Integer regulatorTeamCount = (Integer) entityManager.createNativeQuery(
        "SELECT COUNT(*) FROM lms.teams t WHERE t.type = :type",
        Integer.class
    ).setParameter("type", teamType.name()).getSingleResult();

    if (regulatorTeamCount == 0) {
      LOGGER.info("Bootstrapping regulator team: {}", teamType.getDisplayName());
      var team = new Team();
      team.setName(teamType.getDisplayName());
      team.setTeamType(teamType);
      entityManager.persist(team);
      entityManager.flush();
    }

    Integer regulatorTeamUserCount = (Integer) entityManager.createNativeQuery(
        "SELECT COUNT(*) FROM lms.team_roles tr " +
            "JOIN lms.teams t ON t.id = tr.team_id " +
            "WHERE t.type = :type",
        Integer.class
    ).setParameter("type", teamType.name()).getSingleResult();

    if (regulatorTeamUserCount == 0) {
      LOGGER.info("Bootstrapping regulator users");

      var regulatorAdmin = energyPortalUserService.findUsersByEmail(
          "administrator@lms.co.uk",
          "Bootstrapping LMS regulator admin for local dev")
          .getFirst();

      var teamRole = new TeamRole();
      var regulatorTeam = teamQueryService.getStaticTeam(teamType);
      teamRole.setTeam(regulatorTeam);
      teamRole.setRole(Role.MANAGE_TEAM);
      teamRole.setWuaId(regulatorAdmin.webUserAccountId());
      entityManager.persist(teamRole);
      entityManager.flush();
    }
  }
}