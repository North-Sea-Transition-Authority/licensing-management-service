package uk.co.nstauthority.licensingmanagementservice.licence.schedule.reviewandapply;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.ScheduleRelativeDateValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineSummaryCardView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = ReviewAndApplyScheduleController.class)
class ReviewAndApplyScheduleControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleTimelineService licenceScheduleTimelineService;

  @MockitoBean
  private EventCommentService eventCommentService;

  @MockitoBean
  private ScheduleRelativeDateValidationService scheduleRelativeDateValidationService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private static final String PAGE_CAPTION = "page caption";

  private Licence licence;
  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    licence = LicenceTestUtil.builder().build();

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence)
    );
  }

  @Test
  void renderReviewAndApplyPage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR))
    ).thenReturn(true);

    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);
    var summaryCardView = new TimelineSummaryCardView("date", "date2", true, "1", "status", "", "");
    when(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail)).thenReturn(summaryCardView);
    when(scheduleRelativeDateValidationService.doesFinalPhaseEndDateMatchEndOfInitialTerm(licenceScheduleDetail))
        .thenReturn(true);
    when(scheduleRelativeDateValidationService.doesExpiryDateMatchEndOfFinalTerm(licenceScheduleDetail))
        .thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(ReviewAndApplyScheduleController.class).renderReviewAndApplyPage(licenceScheduleDetail.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/reviewAndApply"))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION))
        .andExpect(model().attribute("summaryCardView", summaryCardView))
        .andExpect(model().attribute("initialTermPhaseValidationError", false))
        .andExpect(model().attribute("expiryDateMatchValidationError", false))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()));
  }

  @Test
  void renderReviewAndApplyPage_whenFinalPhaseEndDateDoesNotMatchInitialTermEndDate_hasValidationError() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR))
    ).thenReturn(true);

    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);
    var summaryCardView = new TimelineSummaryCardView("date", "date2", true, "1", "status", "", "");
    when(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail)).thenReturn(summaryCardView);
    when(scheduleRelativeDateValidationService.doesFinalPhaseEndDateMatchEndOfInitialTerm(licenceScheduleDetail))
        .thenReturn(false);
    when(scheduleRelativeDateValidationService.doesExpiryDateMatchEndOfFinalTerm(licenceScheduleDetail))
        .thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(ReviewAndApplyScheduleController.class).renderReviewAndApplyPage(licenceScheduleDetail.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/reviewAndApply"))
        .andExpect(model().attribute("initialTermPhaseValidationError", true))
        .andExpect(model().attribute("expiryDateMatchValidationError", false));
  }

  @Test
  void renderReviewAndApplyPage_whenExpiryDateDoesNotMatchFinalTermEndDate_hasValidationError() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR))
    ).thenReturn(true);

    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);
    var summaryCardView = new TimelineSummaryCardView("date", "date2", true, "1", "status", "", "");
    when(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail)).thenReturn(summaryCardView);
    when(scheduleRelativeDateValidationService.doesFinalPhaseEndDateMatchEndOfInitialTerm(licenceScheduleDetail))
        .thenReturn(true);
    when(scheduleRelativeDateValidationService.doesExpiryDateMatchEndOfFinalTerm(licenceScheduleDetail))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(ReviewAndApplyScheduleController.class).renderReviewAndApplyPage(licenceScheduleDetail.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/reviewAndApply"))
        .andExpect(model().attribute("initialTermPhaseValidationError", false))
        .andExpect(model().attribute("expiryDateMatchValidationError", true));
  }

  @Test
  void applyDraftSchedule() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR))
    ).thenReturn(true);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);

    mockMvc.perform(
            post(ReverseRouter.route(on(ReviewAndApplyScheduleController.class).applyDraftSchedule(licenceScheduleDetail.getId(), null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(eventCommentService).publishPendingCommentsForSchedule(licenceScheduleDetail.getLicenceSchedule());
    verify(licenceScheduleDetailService).applyAndReplaceActiveScheduleDetail(licenceScheduleDetail);
  }

  @Test
  void renderReviewAndApplyPage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR))
    ).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(ReviewAndApplyScheduleController.class).renderReviewAndApplyPage(licenceScheduleDetail.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void applyDraftSchedule_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(
        organisationUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR))
    ).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(ReviewAndApplyScheduleController.class).applyDraftSchedule(licenceScheduleDetail.getId(), null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());
  }

}