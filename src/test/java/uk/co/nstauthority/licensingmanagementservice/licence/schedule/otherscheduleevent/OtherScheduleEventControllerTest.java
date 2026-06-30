package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

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

@ContextConfiguration(classes = OtherScheduleEventController.class)
class OtherScheduleEventControllerTest extends AbstractControllerTest {

  @MockitoBean
  private OtherScheduleEventService otherScheduleEventService;

  @MockitoBean
  private OtherScheduleEventFormService otherScheduleEventFormService;

  @MockitoBean
  private OtherScheduleEventFormValidator otherScheduleEventFormValidator;

  @MockitoBean
  private LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService;

  private Licence licence;
  private static final String PAGE_CAPTION = "page caption";

  private LicenceScheduleDetail licenceScheduleDetail;
  private OtherScheduleEvent otherScheduleEvent;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    otherScheduleEvent = new OtherScheduleEvent();
    otherScheduleEvent.setId(UUID.randomUUID());
    otherScheduleEvent.setLicenceScheduleDetail(licenceScheduleDetail);
  }

  @Test
  void renderAddNewEventForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(otherScheduleEventFormService.getDateOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).thenReturn(Map.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(OtherScheduleEventController.class)
                .renderAddNewEventForm(licenceScheduleDetail.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createOtherScheduleEvent"))
        .andExpect(model().attribute("categoryRadioOptions", OtherScheduleEventCategory.getCategoriesForLicenceType(licence.getType())))
        .andExpect(model().attribute("eventDateRadioOptions", Map.of()))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("relativeOptions", Map.of()))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));
  }

  @Test
  void renderAddNewEventForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(OtherScheduleEventController.class)
                .renderAddNewEventForm(licenceScheduleDetail.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitAddNewEventForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);
    when(otherScheduleEventFormValidator.isValid(any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(OtherScheduleEventController.class)
                .submitAddNewEventForm(licenceScheduleDetail.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(otherScheduleEventFormService).saveEventFromForm(any(), eq(licenceScheduleDetail), any(), any());
  }

  @Test
  void submitAddNewEventForm_invalid() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(otherScheduleEventFormValidator.isValid(any(), any())).thenReturn(false);
    when(otherScheduleEventFormService.getDateOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).thenReturn(Map.of());

    mockMvc.perform(
            post(ReverseRouter.route(on(OtherScheduleEventController.class)
                .submitAddNewEventForm(licenceScheduleDetail.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createOtherScheduleEvent"))
        .andExpect(model().attribute("categoryRadioOptions", OtherScheduleEventCategory.getCategoriesForLicenceType(licence.getType())))
        .andExpect(model().attribute("eventDateRadioOptions", Map.of()))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("relativeOptions", Map.of()))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));

    verify(otherScheduleEventFormService, never()).saveEventFromForm(any(), any(), any(), any());
  }

  @Test
  void submitAddNewEventForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(OtherScheduleEventController.class)
                .submitAddNewEventForm(licenceScheduleDetail.getId(), null, null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(otherScheduleEventFormService, never()).saveEventFromForm(any(), any(), any(), any());
  }

  @Test
  void renderUpdateEventForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(otherScheduleEventService.getOtherScheduleEventByIdOrThrow(otherScheduleEvent.getId())).thenReturn(otherScheduleEvent);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(otherScheduleEventFormService.getDateOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(otherScheduleEventFormService.getEventForm(otherScheduleEvent)).thenReturn(new OtherScheduleEventForm());

    mockMvc.perform(
            get(ReverseRouter.route(on(OtherScheduleEventController.class)
                .renderUpdateEventForm(otherScheduleEvent.getId())))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createOtherScheduleEvent"))
        .andExpect(model().attribute("categoryRadioOptions", OtherScheduleEventCategory.getCategoriesForLicenceType(licence.getType())))
        .andExpect(model().attribute("eventDateRadioOptions", Map.of()))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("relativeOptions", Map.of()))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));
  }

  @Test
  void renderUpdateEventForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(OtherScheduleEventController.class)
                .renderUpdateEventForm(otherScheduleEvent.getId())))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitUpdateEventForm() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(otherScheduleEventService.getOtherScheduleEventByIdOrThrow(otherScheduleEvent.getId())).thenReturn(otherScheduleEvent);
    when(otherScheduleEventFormValidator.isValid(any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(OtherScheduleEventController.class)
                .submitUpdateEventForm(otherScheduleEvent.getId(), null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(otherScheduleEventFormService).saveEventFromForm(any(), eq(licenceScheduleDetail), eq(otherScheduleEvent), any());
  }

  @Test
  void submitUpdateEventForm_invalid() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(otherScheduleEventService.getOtherScheduleEventByIdOrThrow(otherScheduleEvent.getId())).thenReturn(otherScheduleEvent);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(otherScheduleEventFormValidator.isValid(any(), any())).thenReturn(false);
    when(otherScheduleEventFormService.getDateOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(otherScheduleEventFormService.getEventForm(otherScheduleEvent)).thenReturn(new OtherScheduleEventForm());

    mockMvc.perform(
            post(ReverseRouter.route(on(OtherScheduleEventController.class)
                .submitUpdateEventForm(otherScheduleEvent.getId(), null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createOtherScheduleEvent"))
        .andExpect(model().attribute("categoryRadioOptions", OtherScheduleEventCategory.getCategoriesForLicenceType(licence.getType())))
        .andExpect(model().attribute("eventDateRadioOptions", Map.of()))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("relativeOptions", Map.of()))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));

    verify(otherScheduleEventFormService, never()).saveEventFromForm(any(), any(), any(), any());
  }

  @Test
  void submitUpdateEventForm_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(OtherScheduleEventController.class)
                .submitUpdateEventForm(otherScheduleEvent.getId(), null, null, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(otherScheduleEventFormService, never()).saveEventFromForm(any(), any(), any(), any());
  }
}