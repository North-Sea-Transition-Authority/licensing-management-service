package uk.co.nstauthority.licensingmanagementservice.migration.industryteam;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Profile("migration")
@Controller
@RequestMapping("/migration/industry-teams")
public class IndustryTeamMigrationController {

  private final IndustryTeamMigrationService industryTeamMigrationService;

  public IndustryTeamMigrationController(IndustryTeamMigrationService industryTeamMigrationService) {
    this.industryTeamMigrationService = industryTeamMigrationService;
  }

  @GetMapping("/teams")
  public ResponseEntity<String> migrateIndustryTeams() {
    var createdCount = industryTeamMigrationService.migrateIndustryTeams();
    return ResponseEntity.ok("%d industry teams migrated".formatted(createdCount));
  }
}
