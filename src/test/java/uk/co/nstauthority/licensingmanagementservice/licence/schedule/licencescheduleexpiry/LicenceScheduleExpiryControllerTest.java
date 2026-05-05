package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = LicenceScheduleExpiryController.class)
class LicenceScheduleExpiryControllerTest extends AbstractControllerTest {

  @MockitoBean
  LicenceScheduleExpiryService licenceScheduleExpiryService;

  @MockitoBean
  LicenceScheduleExpiryFormValidator licenceScheduleExpiryFormValidator;

  private LicenceScheduleDetail licenceScheduleDetail;

  private Licence licence;
  private static final String PAGE_CAPTION = "page caption";

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        UUID.randomUUID(),
        LicenceScheduleTestUtil.createLicenceSchedule(licence)
    );

    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);
  }

  @Test
  void renderAddUpdateLicenceExpiryPage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);

    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);

    var expiry = new LicenceScheduleExpiry();

    when(licenceScheduleExpiryService.getOrCreateExpiry(licenceScheduleDetail)).thenReturn(expiry);
    when(licenceScheduleExpiryService.getExpiryForm(expiry)).thenReturn(new LicenceScheduleExpiryForm());

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleExpiryController.class).renderAddUpdateLicenceExpiryPage(licenceScheduleDetail.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createLicenceExpiry"))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));
  }

  @Test
  void renderAddUpdateLicenceExpiryPage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleExpiryController.class).renderAddUpdateLicenceExpiryPage(licenceScheduleDetail.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitAddUpdateLicenceExpiryPage_validForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);

    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licenceScheduleExpiryService.getOrCreateExpiry(licenceScheduleDetail)).thenReturn(new LicenceScheduleExpiry());
    when(licenceScheduleExpiryFormValidator.isValid(any(), any(), eq(licenceScheduleDetail))).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleExpiryController.class).submitAddUpdateLicenceExpiryPage(licenceScheduleDetail.getId(), null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleExpiryService).saveExpiryFromForm(any(), eq(licenceScheduleDetail), any());
  }

  @Test
  void submitAddUpdateLicenceExpiryPage_invalidForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);

    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licenceScheduleExpiryService.getOrCreateExpiry(licenceScheduleDetail)).thenReturn(new LicenceScheduleExpiry());
    when(licenceScheduleExpiryFormValidator.isValid(any(), any(), eq(licenceScheduleDetail))).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleExpiryController.class).submitAddUpdateLicenceExpiryPage(licenceScheduleDetail.getId(), null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createLicenceExpiry"))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));

    verify(licenceScheduleExpiryService, never()).saveExpiryFromForm(any(), any(), any());
  }

  @Test
  void submitAddUpdateLicenceExpiryPage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleExpiryController.class).submitAddUpdateLicenceExpiryPage(licenceScheduleDetail.getId(), null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(licenceScheduleExpiryService, never()).saveExpiryFromForm(any(), any(), any());
  }
}