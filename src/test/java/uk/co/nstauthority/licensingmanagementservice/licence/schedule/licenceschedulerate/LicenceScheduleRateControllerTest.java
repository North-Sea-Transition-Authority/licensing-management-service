package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.LicenceScheduleRelativeOptionsService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = LicenceScheduleRateController.class)
class LicenceScheduleRateControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleRateFormService licenceScheduleRateFormService;

  @MockitoBean
  private LicenceScheduleRateFormValidator licenceScheduleRateFormValidator;

  @MockitoBean
  private LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService;

  @MockitoBean
  private LicenceScheduleRateService licenceScheduleRateService;

  private Licence licence;
  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence)
    );

    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);
  }

  @Test
  void renderNewLicenceScheduleRateForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);

    var pageCaption = "P001";

    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRateFormService.getRateDefinitionOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceService.getLicencePageCaption(licence)).thenReturn(pageCaption);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleRateController.class).renderNewLicenceScheduleRateForm(licenceScheduleDetail.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createScheduleRate"))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("rateDefinitionOptions", Map.of()))
        .andExpect(model().attribute("relativeEventOptions", Map.of()))
        .andExpect(model().attribute("relativeDateOptions", RateRelativeDateOption.getRateRelativeDateOptions()))
        .andExpect(model().attribute("pageCaption", pageCaption))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()));
  }

  @Test
  void renderNewLicenceScheduleRateForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleRateController.class).renderNewLicenceScheduleRateForm(licenceScheduleDetail.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitNewLicenceScheduleRateForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleRateFormValidator.isValid(any(LicenceScheduleRateForm.class), any(Errors.class), any(LicenceScheduleDetail.class), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleRateController.class).submitNewLicenceScheduleRateForm(licenceScheduleDetail.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleRateFormService).saveRateFromForm(any(), eq(licenceScheduleDetail), any(), any());
  }

  @Test
  void submitNewLicenceScheduleRateForm_invalid() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleRateFormValidator.isValid(any(LicenceScheduleRateForm.class), any(Errors.class), any(LicenceScheduleDetail.class), any())).thenReturn(false);

    var pageCaption = "P001";

    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRateFormService.getRateDefinitionOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceService.getLicencePageCaption(licence)).thenReturn(pageCaption);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleRateController.class).submitNewLicenceScheduleRateForm(licenceScheduleDetail.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createScheduleRate"))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("rateDefinitionOptions", Map.of()))
        .andExpect(model().attribute("relativeEventOptions", Map.of()))
        .andExpect(model().attribute("relativeDateOptions", RateRelativeDateOption.getRateRelativeDateOptions()))
        .andExpect(model().attribute("pageCaption", pageCaption))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()));

    verify(licenceScheduleRateFormService, never()).saveRateFromForm(any(), any(), any(), any());
  }

  @Test
  void submitNewLicenceScheduleRateForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleRateController.class).submitNewLicenceScheduleRateForm(licenceScheduleDetail.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(licenceScheduleRateFormService, never()).saveRateFromForm(any(), any(), any(), any());
  }

  @Test
  void renderUpdateLicenceScheduleRateForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);

    var pageCaption = "P001";

    var rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setLicenceScheduleDetail(licenceScheduleDetail);
    when(licenceScheduleRateService.getRateByIdOrThrow(rate.getId())).thenReturn(rate);

    var form = new LicenceScheduleRateForm();
    when(licenceScheduleRateFormService.getFormFromRate(rate)).thenReturn(form);

    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRateFormService.getRateDefinitionOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceService.getLicencePageCaption(licence)).thenReturn(pageCaption);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleRateController.class).renderUpdateLicenceScheduleRateForm(rate.getId())))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createScheduleRate"))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("rateDefinitionOptions", Map.of()))
        .andExpect(model().attribute("relativeEventOptions", Map.of()))
        .andExpect(model().attribute("relativeDateOptions", RateRelativeDateOption.getRateRelativeDateOptions()))
        .andExpect(model().attribute("pageCaption", pageCaption))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()));
  }

  @Test
  void renderUpdateLicenceScheduleRateForm_noRoles() throws Exception {
    var rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setLicenceScheduleDetail(licenceScheduleDetail);

    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleRateController.class).renderUpdateLicenceScheduleRateForm(rate.getId())))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitUpdateLicenceScheduleRateForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    var rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setLicenceScheduleDetail(licenceScheduleDetail);

    when(licenceScheduleRateService.getRateByIdOrThrow(rate.getId())).thenReturn(rate);
    when(licenceScheduleRateFormValidator.isValid(any(LicenceScheduleRateForm.class), any(Errors.class), any(LicenceScheduleDetail.class), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleRateController.class).submitUpdateLicenceScheduleRateForm(rate.getId(), null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleRateFormService).saveRateFromForm(any(), eq(licenceScheduleDetail), eq(rate), any());
  }

  @Test
  void submitUpdateLicenceScheduleRateForm_invalid() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleRateFormValidator.isValid(any(LicenceScheduleRateForm.class), any(Errors.class), any(LicenceScheduleDetail.class), any())).thenReturn(false);

    var pageCaption = "P001";

    var rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setLicenceScheduleDetail(licenceScheduleDetail);
    when(licenceScheduleRateService.getRateByIdOrThrow(rate.getId())).thenReturn(rate);
    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRateFormService.getRateDefinitionOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceService.getLicencePageCaption(licence)).thenReturn(pageCaption);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleRateController.class).submitUpdateLicenceScheduleRateForm(rate.getId(), null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createScheduleRate"))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("rateDefinitionOptions", Map.of()))
        .andExpect(model().attribute("relativeEventOptions", Map.of()))
        .andExpect(model().attribute("relativeDateOptions", RateRelativeDateOption.getRateRelativeDateOptions()))
        .andExpect(model().attribute("pageCaption", pageCaption))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()));

    verify(licenceScheduleRateFormService, never()).saveRateFromForm(any(), any(), any(), any());
  }

  @Test
  void submitUpdateLicenceScheduleRateForm_noRoles() throws Exception {
    var rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setLicenceScheduleDetail(licenceScheduleDetail);

    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleRateController.class).submitUpdateLicenceScheduleRateForm(rate.getId(), null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(licenceScheduleRateFormService, never()).saveRateFromForm(any(), any(), any(), any());
  }

}