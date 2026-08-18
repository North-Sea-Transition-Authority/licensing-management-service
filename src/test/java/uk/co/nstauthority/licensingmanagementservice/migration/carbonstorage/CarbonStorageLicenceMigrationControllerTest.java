package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ActiveProfiles("migration")
@ContextConfiguration(classes = CarbonStorageLicenceMigrationController.class)
class CarbonStorageLicenceMigrationControllerTest extends AbstractControllerTest {

  private static final String MIGRATE_LICENCES_ROUTE =
      ReverseRouter.route(on(CarbonStorageLicenceMigrationController.class).migrateLicences());
  private static final String MIGRATE_SCHEDULES_ROUTE =
      ReverseRouter.route(on(CarbonStorageLicenceMigrationController.class).migrateSchedules());

  @MockitoBean
  private CarbonStorageLicenceMigrationService carbonStorageLicenceMigrationService;

  @Test
  void migrateLicences_whenGet_thenMigratesLicences() throws Exception {
    mockMvc.perform(get(MIGRATE_LICENCES_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().string("licences migrated"));

    verify(carbonStorageLicenceMigrationService).migrateLicences();
  }

  @Test
  void migrateLicences_whenPost_thenMigratesLicences() throws Exception {
    mockMvc.perform(post(MIGRATE_LICENCES_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string("licences migrated"));

    verify(carbonStorageLicenceMigrationService).migrateLicences();
  }

  @Test
  void migrateLicences_whenNotAuthenticated_thenRedirectsToLogin() throws Exception {
    mockMvc.perform(get(MIGRATE_LICENCES_ROUTE))
        .andExpect(redirectionToLoginUrl());

    verifyNoInteractions(carbonStorageLicenceMigrationService);
  }

  @Test
  void migrateSchedules_whenGet_thenMigratesSchedules() throws Exception {
    mockMvc.perform(get(MIGRATE_SCHEDULES_ROUTE).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().string("schedules migrated"));

    verify(carbonStorageLicenceMigrationService).migrateSchedules();
  }

  @Test
  void migrateSchedules_whenPost_thenMigratesSchedules() throws Exception {
    mockMvc.perform(post(MIGRATE_SCHEDULES_ROUTE).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string("schedules migrated"));

    verify(carbonStorageLicenceMigrationService).migrateSchedules();
  }

  @Test
  void migrateSchedules_whenNotAuthenticated_thenRedirectsToLogin() throws Exception {
    mockMvc.perform(get(MIGRATE_SCHEDULES_ROUTE))
        .andExpect(redirectionToLoginUrl());

    verifyNoInteractions(carbonStorageLicenceMigrationService);
  }
}
