package uk.co.nstauthority.licensingmanagementservice.migration.industryteam;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Profile("migration")
@RestController
@RequestMapping("/migration/industry-teams")
public class IndustryTeamMigrationController {

  private final IndustryTeamMigrationService industryTeamMigrationService;

  public IndustryTeamMigrationController(IndustryTeamMigrationService industryTeamMigrationService) {
    this.industryTeamMigrationService = industryTeamMigrationService;
  }

  /**
   * GET is supported alongside POST purely so the migration can be triggered by pasting the URL into a logged in
   * browser, which is the only practical way to reach this endpoint by hand given it sits behind SAML authentication.
   */
  @RequestMapping(value = "/teams", method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<String> migrateIndustryTeams() {
    var result = industryTeamMigrationService.migrateIndustryTeams();
    return ResponseEntity.ok(result.describe("industry teams"));
  }

  /**
   * GET is supported alongside POST for the same reason as {@link #migrateIndustryTeams()}.
   */
  @RequestMapping(value = "/team-users", method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<String> migrateIndustryTeamUsers() {
    var result = industryTeamMigrationService.migrateIndustryTeamUsers();
    return ResponseEntity.ok(result.describe("industry team users"));
  }
}
