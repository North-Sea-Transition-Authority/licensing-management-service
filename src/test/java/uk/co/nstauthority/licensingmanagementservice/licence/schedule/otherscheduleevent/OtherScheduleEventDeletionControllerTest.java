package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

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

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = OtherScheduleEventDeletionController.class)
class OtherScheduleEventDeletionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private OtherScheduleEventService otherScheduleEventService;

  @MockitoBean
  private EventCommentService eventCommentService;

  private Licence licence;
  private LicenceScheduleDetail licenceScheduleDetail;

  private OtherScheduleEvent otherScheduleEvent;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder().build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    otherScheduleEvent = new OtherScheduleEvent();
    otherScheduleEvent.setId(UUID.randomUUID());
    otherScheduleEvent.setLicenceScheduleDetail(licenceScheduleDetail);
    otherScheduleEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    otherScheduleEvent.setDescription("description");
    otherScheduleEvent.setEventDate(LocalDate.of(2025, 1, 1));
  }

  @Test
  void renderDeleteEventPage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(otherScheduleEventService.getOtherScheduleEventByIdOrThrow(otherScheduleEvent.getId())).thenReturn(otherScheduleEvent);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");

    mockMvc.perform(
            get(ReverseRouter.route(on(OtherScheduleEventDeletionController.class).renderDeleteEventPage(otherScheduleEvent.getId())))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/deleteOtherScheduleEvent"))
        .andExpect(model().attribute("pageTitle", "Do you want to delete the %s event?".formatted(otherScheduleEvent.getCategoryString())))
        .andExpect(model().attribute("summaryView", OtherScheduleEventSummaryView.fromOtherScheduleEvent(otherScheduleEvent)))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", "caption"))
        .andExpect(model().attribute("pendingComment", ""));
  }

  @Test
  void renderDeleteEventPage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(OtherScheduleEventDeletionController.class).renderDeleteEventPage(otherScheduleEvent.getId())))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitDeleteEventPage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(otherScheduleEventService.getOtherScheduleEventByIdOrThrow(otherScheduleEvent.getId())).thenReturn(otherScheduleEvent);

    mockMvc.perform(
            post(ReverseRouter.route(on(OtherScheduleEventDeletionController.class).submitDeleteEventPage(otherScheduleEvent.getId(), null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(otherScheduleEventService).deleteOtherScheduleEvent(otherScheduleEvent);
  }

  @Test
  void submitDeleteEventPage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(OtherScheduleEventDeletionController.class).submitDeleteEventPage(otherScheduleEvent.getId(), null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(otherScheduleEventService, never()).deleteOtherScheduleEvent(otherScheduleEvent);
  }

  @Test
  void renderDeleteEventPage_whenEventHasEventReferenceAndPendingCommentExists_showsPendingComment() throws Exception {
    var eventReference = new EventReference();
    otherScheduleEvent.setEventReference(eventReference);

    var eventComment = new EventComment();
    eventComment.setComment("a pending comment");

    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(otherScheduleEventService.getOtherScheduleEventByIdOrThrow(otherScheduleEvent.getId())).thenReturn(otherScheduleEvent);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");
    when(eventCommentService.findPendingCommentForEventReference(eventReference)).thenReturn(Optional.of(eventComment));

    mockMvc.perform(
            get(ReverseRouter.route(on(OtherScheduleEventDeletionController.class).renderDeleteEventPage(otherScheduleEvent.getId())))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("pendingComment", "a pending comment"));
  }

  @Test
  void renderDeleteEventPage_whenEventHasEventReferenceAndNoPendingComment_showsEmptyPendingComment() throws Exception {
    var eventReference = new EventReference();
    otherScheduleEvent.setEventReference(eventReference);

    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(otherScheduleEventService.getOtherScheduleEventByIdOrThrow(otherScheduleEvent.getId())).thenReturn(otherScheduleEvent);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");
    when(eventCommentService.findPendingCommentForEventReference(eventReference)).thenReturn(Optional.empty());

    mockMvc.perform(
            get(ReverseRouter.route(on(OtherScheduleEventDeletionController.class).renderDeleteEventPage(otherScheduleEvent.getId())))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("pendingComment", ""));
  }

}