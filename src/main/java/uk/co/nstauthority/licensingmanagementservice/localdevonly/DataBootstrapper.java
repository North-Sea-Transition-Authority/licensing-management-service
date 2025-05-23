package uk.co.nstauthority.licensingmanagementservice.localdevonly;

import jakarta.persistence.EntityManager;
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


  private final EnergyPortalUserService energyPortalUserService;
  private final EntityManager entityManager;
  private final TeamQueryService teamQueryService;

  public DataBootstrapper(EnergyPortalUserService energyPortalUserService, EntityManager entityManager,
                          TeamQueryService teamQueryService) {
    this.energyPortalUserService = energyPortalUserService;
    this.entityManager = entityManager;
    this.teamQueryService = teamQueryService;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void loadData() {

    //TODO replace dev set up once team structure is determined
    Integer regulatorTeamCount = (Integer) entityManager.createNativeQuery(
        "SELECT COUNT(*) FROM lms.teams t WHERE t.type = 'REGULATOR'", Integer.class).getSingleResult();

    if (regulatorTeamCount == 0) {
      LOGGER.info("Bootstrapping regulator team");
      var team = new Team();
      team.setName("Regulator");
      team.setTeamType(TeamType.REGULATOR);
      entityManager.persist(team);
      entityManager.flush();
    }

    Integer regulatorTeamUserCount = (Integer) entityManager.createNativeQuery(
        "SELECT COUNT(*) FROM lms.team_roles tr " +
            "JOIN lms.teams t ON t.id = tr.team_id " +
            "WHERE t.type = 'REGULATOR'",
        Integer.class).getSingleResult();

    if (regulatorTeamUserCount == 0) {
      LOGGER.info("Bootstrapping regulator users");

      var regulatorAdmin = energyPortalUserService.findUsersByEmail(
          "administrator@lms.co.uk",
          "Bootstrapping LMS admin for local dev")
          .getFirst();

      var teamRole = new TeamRole();
      var regulatorTeam = teamQueryService.getStaticTeam(TeamType.REGULATOR);
      teamRole.setTeam(regulatorTeam);
      teamRole.setRole(Role.MANAGE_TEAM);
      teamRole.setWuaId(regulatorAdmin.webUserAccountId());
      entityManager.persist(teamRole);
      entityManager.flush();
    }

  }
}
