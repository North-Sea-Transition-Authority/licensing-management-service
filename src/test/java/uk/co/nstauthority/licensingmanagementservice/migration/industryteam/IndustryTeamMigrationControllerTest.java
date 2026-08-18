package uk.co.nstauthority.licensingmanagementservice.migration.industryteam;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationResult;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ActiveProfiles("migration")
@ContextConfiguration(classes = IndustryTeamMigrationController.class)
class IndustryTeamMigrationControllerTest extends AbstractControllerTest {

  private static final String MIGRATE_TEAMS_ROUTE =
      ReverseRouter.route(on(IndustryTeamMigrationController.class).migrateIndustryTeams());
  private static final String MIGRATE_TEAM_USERS_ROUTE =
      ReverseRouter.route(on(IndustryTeamMigrationController.class).migrateIndustryTeamUsers());

  @MockitoBean
  private IndustryTeamMigrationService industryTeamMigrationService;

  @Test
  void migrateIndustryTeams_whenGet_thenReportsTheNumberOfTeamsCreated() throws Exception {
    when(industryTeamMigrationService.migrateIndustryTeams()).thenReturn(new MigrationResult(3, 0));

    mockMvc.perform(get(MIGRATE_TEAMS_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().string("3 industry teams migrated, 0 skipped"));
  }

  @Test
  void migrateIndustryTeams_whenPost_thenReportsTheNumberOfTeamsCreated() throws Exception {
    when(industryTeamMigrationService.migrateIndustryTeams()).thenReturn(new MigrationResult(3, 0));

    mockMvc.perform(post(MIGRATE_TEAMS_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string("3 industry teams migrated, 0 skipped"));
  }

  @Test
  void migrateIndustryTeams_whenEverythingIsAlreadyMigrated_thenReportsThemAsSkipped() throws Exception {
    when(industryTeamMigrationService.migrateIndustryTeams()).thenReturn(new MigrationResult(0, 12));

    mockMvc.perform(get(MIGRATE_TEAMS_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().string("0 industry teams migrated, 12 skipped"));
  }

  @Test
  void migrateIndustryTeams_whenNotAuthenticated_thenRedirectsToLogin() throws Exception {
    mockMvc.perform(get(MIGRATE_TEAMS_ROUTE))
        .andExpect(redirectionToLoginUrl());

    verifyNoInteractions(industryTeamMigrationService);
  }

  @Test
  void migrateIndustryTeamUsers_whenGet_thenReportsTheNumberOfUsersMigrated() throws Exception {
    when(industryTeamMigrationService.migrateIndustryTeamUsers()).thenReturn(new MigrationResult(5, 2));

    mockMvc.perform(get(MIGRATE_TEAM_USERS_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().string("5 industry team users migrated, 2 skipped"));

    verify(industryTeamMigrationService).migrateIndustryTeamUsers();
  }

  @Test
  void migrateIndustryTeamUsers_whenPost_thenReportsTheNumberOfUsersMigrated() throws Exception {
    when(industryTeamMigrationService.migrateIndustryTeamUsers()).thenReturn(new MigrationResult(5, 2));

    mockMvc.perform(post(MIGRATE_TEAM_USERS_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string("5 industry team users migrated, 2 skipped"));
  }

  @Test
  void migrateIndustryTeamUsers_whenNotAuthenticated_thenRedirectsToLogin() throws Exception {
    mockMvc.perform(get(MIGRATE_TEAM_USERS_ROUTE))
        .andExpect(redirectionToLoginUrl());

    verifyNoInteractions(industryTeamMigrationService);
  }
}
