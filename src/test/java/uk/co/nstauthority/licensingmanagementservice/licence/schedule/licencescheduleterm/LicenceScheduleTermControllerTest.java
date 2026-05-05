package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

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
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = LicenceScheduleTermController.class)
class LicenceScheduleTermControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleTermFormService licenceScheduleTermFormService;

  @MockitoBean
  private LicenceScheduleTermFormValidator licenceScheduleTermFormValidator;

  @MockitoBean
  private LicenceScheduleTermService licenceScheduleTermService;

  private LicenceScheduleDetail licenceScheduleDetail;
  private static final UUID LICENCE_SCHEDULE_DETAIL_ID = UUID.randomUUID();

  private LicenceScheduleTerm licenceScheduleTerm;
  private static final UUID LICENCE_SCHEDULE_TERM_ID = UUID.randomUUID();

  private Licence licence;
  private static final String PAGE_CAPTION = "page caption";

  @BeforeEach
  void setUp() {
    licence = new Licence();
    licence.setType(LicenceType.SEAWARD_PRODUCTION);

    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);

    licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setId(LICENCE_SCHEDULE_DETAIL_ID);
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);

    licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setId(LICENCE_SCHEDULE_TERM_ID);
    licenceScheduleTerm.setTermType(TermType.INITIAL);
    licenceScheduleTerm.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    licenceScheduleTerm.setLicenceScheduleDetail(licenceScheduleDetail);
  }

  @Test
  void renderAddNewTermForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licenceScheduleDetailService.getByIdOrThrow(LICENCE_SCHEDULE_DETAIL_ID)).thenReturn(licenceScheduleDetail);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTermController.class).renderAddNewTermForm(LICENCE_SCHEDULE_DETAIL_ID, null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createScheduleTerm"))
        .andExpect(model().attribute("radioOptions", TermType.getTermRadioOptionsFor(LicenceType.SEAWARD_PRODUCTION)))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));
  }

  @Test
  void renderAddNewTermForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTermController.class).renderAddNewTermForm(LICENCE_SCHEDULE_DETAIL_ID, null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitAddNewTermForm_validForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleDetailService.getByIdOrThrow(LICENCE_SCHEDULE_DETAIL_ID)).thenReturn(licenceScheduleDetail);
    when(licenceScheduleTermFormValidator.isValid(any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleTermController.class).submitAddNewTermForm(LICENCE_SCHEDULE_DETAIL_ID, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleTermFormService).saveTermFromForm(any(), eq(licenceScheduleDetail), any());
  }

  @Test
  void submitAddNewTermForm_invalidForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licenceScheduleDetailService.getByIdOrThrow(LICENCE_SCHEDULE_DETAIL_ID)).thenReturn(licenceScheduleDetail);
    when(licenceScheduleTermFormValidator.isValid(any(), any(), any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleTermController.class).submitAddNewTermForm(LICENCE_SCHEDULE_DETAIL_ID, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createScheduleTerm"))
        .andExpect(model().attribute("radioOptions", TermType.getTermRadioOptionsFor(LicenceType.SEAWARD_PRODUCTION)))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));

    verify(licenceScheduleTermFormService, never()).saveTermFromForm(any(), any(), any());
  }

  @Test
  void submitAddNewTermForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleTermController.class).submitAddNewTermForm(LICENCE_SCHEDULE_DETAIL_ID, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(licenceScheduleTermFormService, never()).saveTermFromForm(any(), any(), any());
  }

  @Test
  void renderUpdateTermForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licenceScheduleTermService.getTermByIdOrThrow(LICENCE_SCHEDULE_TERM_ID)).thenReturn(licenceScheduleTerm);
    when(licenceScheduleTermFormService.getTermForm(licenceScheduleTerm)).thenReturn(new LicenceScheduleTermForm());

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(LICENCE_SCHEDULE_TERM_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createScheduleTerm"))
        .andExpect(model().attribute("radioOptions", TermType.getTermRadioOptionsFor(LicenceType.SEAWARD_PRODUCTION)))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));
  }

  @Test
  void renderUpdateTermForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(LICENCE_SCHEDULE_TERM_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitUpdateTermForm_validForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleTermService.getTermByIdOrThrow(LICENCE_SCHEDULE_TERM_ID)).thenReturn(licenceScheduleTerm);
    when(licenceScheduleTermFormValidator.isValidUpdate(any(), any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleTermController.class).submitUpdateTermForm(LICENCE_SCHEDULE_TERM_ID, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleTermFormService).saveTermFromForm(any(), eq(licenceScheduleDetail), eq(licenceScheduleTerm));
  }

  @Test
  void submitUpdateTermForm_invalidForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licenceScheduleTermService.getTermByIdOrThrow(LICENCE_SCHEDULE_TERM_ID)).thenReturn(licenceScheduleTerm);
    when(licenceScheduleTermFormValidator.isValidUpdate(any(), any(), any(), any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleTermController.class).submitUpdateTermForm(LICENCE_SCHEDULE_TERM_ID, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createScheduleTerm"))
        .andExpect(model().attribute("radioOptions", TermType.getTermRadioOptionsFor(LicenceType.SEAWARD_PRODUCTION)))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));

    verify(licenceScheduleTermFormService, never()).saveTermFromForm(any(), any(), any());
  }

  @Test
  void submitUpdateTermForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleTermController.class).submitUpdateTermForm(LICENCE_SCHEDULE_TERM_ID, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(licenceScheduleTermFormService, never()).saveTermFromForm(any(), any(), any());
  }
}