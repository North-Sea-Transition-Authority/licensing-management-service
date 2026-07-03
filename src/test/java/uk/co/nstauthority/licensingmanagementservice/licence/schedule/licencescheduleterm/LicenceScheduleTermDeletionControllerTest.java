package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

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
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = LicenceScheduleTermDeletionController.class)
class LicenceScheduleTermDeletionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleTermService licenceScheduleTermService;

  @MockitoBean
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @MockitoBean
  private EventCommentService eventCommentService;

  private Licence licence;
  private LicenceScheduleDetail licenceScheduleDetail;

  private LicenceScheduleTerm licenceScheduleTerm;
  private static final UUID LICENCE_SCHEDULE_TERM_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder().build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setId(LICENCE_SCHEDULE_TERM_ID);
    licenceScheduleTerm.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleTerm.setTermType(TermType.INITIAL);
    licenceScheduleTerm.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    licenceScheduleTerm.setStartDate(LocalDate.of(2025, 1, 1));
    licenceScheduleTerm.setEndDate(LocalDate.of(2025, 12, 31));
  }

  @Test
  void renderDeleteTermPage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleTermService.getTermByIdOrThrow(LICENCE_SCHEDULE_TERM_ID)).thenReturn(licenceScheduleTerm);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(LICENCE_SCHEDULE_TERM_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/deleteScheduleTerm"))
        .andExpect(model().attribute("pageTitle", "Do you want to delete the %s?".formatted(licenceScheduleTerm.getTermType().getDisplayName())))
        .andExpect(model().attribute("licenceScheduleTermSummaryView", LicenceScheduleTermSummaryView.fromTerm(licenceScheduleTerm)))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", "caption"))
        .andExpect(model().attribute("pendingComment", ""));
  }

  @Test
  void renderDeleteTermPage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(LICENCE_SCHEDULE_TERM_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitDeleteTermPage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleTermService.getTermByIdOrThrow(LICENCE_SCHEDULE_TERM_ID)).thenReturn(licenceScheduleTerm);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).submitDeleteTermPage(LICENCE_SCHEDULE_TERM_ID, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleTermService).deleteTerm(licenceScheduleTerm);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void submitDeleteTermPage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).submitDeleteTermPage(LICENCE_SCHEDULE_TERM_ID, null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(licenceScheduleTermService, never()).deleteTerm(licenceScheduleTerm);
    verify(licenceScheduleCalculationService, never()).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void renderDeleteTermPage_whenTermHasLicenceScheduleAndPendingCommentExists_showsPendingComment() throws Exception {
    licenceScheduleTerm.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());

    var eventComment = new EventComment();
    eventComment.setComment("a pending comment");

    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleTermService.getTermByIdOrThrow(LICENCE_SCHEDULE_TERM_ID)).thenReturn(licenceScheduleTerm);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");
    when(eventCommentService.findPendingCommentForScheduleEvent(licenceScheduleTerm)).thenReturn(Optional.of(eventComment));

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(LICENCE_SCHEDULE_TERM_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("pendingComment", "a pending comment"));
  }

  @Test
  void renderDeleteTermPage_whenTermHasLicenceScheduleAndNoPendingComment_showsEmptyPendingComment() throws Exception {
    licenceScheduleTerm.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());

    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleTermService.getTermByIdOrThrow(LICENCE_SCHEDULE_TERM_ID)).thenReturn(licenceScheduleTerm);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");
    when(eventCommentService.findPendingCommentForScheduleEvent(licenceScheduleTerm)).thenReturn(Optional.empty());

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(LICENCE_SCHEDULE_TERM_ID)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("pendingComment", ""));
  }
}