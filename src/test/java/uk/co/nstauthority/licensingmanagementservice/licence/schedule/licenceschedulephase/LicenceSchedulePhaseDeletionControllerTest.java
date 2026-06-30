package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

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
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = LicenceSchedulePhaseDeletionController.class)
class LicenceSchedulePhaseDeletionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @MockitoBean
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @MockitoBean
  private EventCommentService eventCommentService;

  private Licence licence;
  private LicenceScheduleDetail licenceScheduleDetail;

  private LicenceSchedulePhase licenceSchedulePhase;
  private static final UUID LICENCE_SCHEDULE_PHASE_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder().build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setId(LICENCE_SCHEDULE_PHASE_ID);
    licenceSchedulePhase.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceSchedulePhase.setPhaseType(PhaseType.PHASE_A);
    licenceSchedulePhase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    licenceSchedulePhase.setStartDate(LocalDate.of(2025, 1, 1));
    licenceSchedulePhase.setEndDate(LocalDate.of(2025, 12, 31));
  }

  @Test
  void renderDeletePhasePage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(LICENCE_SCHEDULE_PHASE_ID)).thenReturn(licenceSchedulePhase);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).renderDeletePhasePage(LICENCE_SCHEDULE_PHASE_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/deleteSchedulePhase"))
        .andExpect(model().attribute("pageTitle", "Do you want to delete the %s?".formatted(licenceSchedulePhase.getPhaseType().getDisplayName())))
        .andExpect(model().attribute("licenceSchedulePhaseSummaryView", LicenceSchedulePhaseSummaryView.fromPhase(licenceSchedulePhase)))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", "caption"))
        .andExpect(model().attribute("pendingComment", ""));
  }

  @Test
  void renderDeletePhasePage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).renderDeletePhasePage(LICENCE_SCHEDULE_PHASE_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitDeletePhasePage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(LICENCE_SCHEDULE_PHASE_ID)).thenReturn(licenceSchedulePhase);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).submitDeletePhasePage(LICENCE_SCHEDULE_PHASE_ID, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceSchedulePhaseService).deletePhase(licenceSchedulePhase);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void submitDeletePhasePage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).submitDeletePhasePage(LICENCE_SCHEDULE_PHASE_ID, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(licenceSchedulePhaseService, never()).deletePhase(licenceSchedulePhase);
    verify(licenceScheduleCalculationService, never()).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void renderDeletePhasePage_whenPhaseHasEventReferenceAndPendingCommentExists_showsPendingComment() throws Exception {
    var eventReference = new EventReference();
    licenceSchedulePhase.setEventReference(eventReference);

    var eventComment = new EventComment();
    eventComment.setComment("a pending comment");

    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(LICENCE_SCHEDULE_PHASE_ID)).thenReturn(licenceSchedulePhase);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");
    when(eventCommentService.findPendingCommentForEventReference(eventReference)).thenReturn(Optional.of(eventComment));

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).renderDeletePhasePage(LICENCE_SCHEDULE_PHASE_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("pendingComment", "a pending comment"));
  }

  @Test
  void renderDeletePhasePage_whenPhaseHasEventReferenceAndNoPendingComment_showsEmptyPendingComment() throws Exception {
    var eventReference = new EventReference();
    licenceSchedulePhase.setEventReference(eventReference);

    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(LICENCE_SCHEDULE_PHASE_ID)).thenReturn(licenceSchedulePhase);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");
    when(eventCommentService.findPendingCommentForEventReference(eventReference)).thenReturn(Optional.empty());

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).renderDeletePhasePage(LICENCE_SCHEDULE_PHASE_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("pendingComment", ""));
  }
}