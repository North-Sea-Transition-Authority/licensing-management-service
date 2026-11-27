package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.reviewandapply.DeleteDraftScheduleController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.reviewandapply.ReviewAndApplyScheduleController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments.ScheduleCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments.ScheduleCommentView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = LicenceScheduleTimelineController.class)
class LicenceScheduleTimelineControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleTimelineService licenceScheduleTimelineService;

  @MockitoBean
  private ScheduleCommentService scheduleCommentService;

  private Licence licence;

  private LicenceScheduleDetail licenceScheduleDetail;

  private String viewTimelineUrl;

  @BeforeEach
  void setUp() {
    when(teamQueryService.userHasRoleInTeamType(
        regulatorUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR))
    ).thenReturn(true);

    licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceReference("P1")
        .withRoundIssuedOn("1")
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    viewTimelineUrl = ReverseRouter.route(on(LicenceScheduleTimelineController.class)
        .renderLicenceScheduleTimeline(licenceScheduleDetail.getId(), null, null, null));
  }

  @Test
  void renderLicenceScheduleTimeline() throws Exception {
    when(licenceService.findLicenceByIdOrThrow(licence.getId())).thenReturn(licence);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);

    var timelineSummaryCardView = new TimelineSummaryCardView("date", "date2", true, "1", LicenceStatusType.EXTANT.getDisplayName(), "", "", List.of());
    var timelineActionViews = List.of(new TimelineActionView(LicenceScheduleTimelineAction.ADD_A_TERM, ""));
    var scheduleEventViews = List.of(new TimelineTermView(List.of(), List.of(), TermType.INITIAL, "", "", "", "", "", true, List.of(), true, true));

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    when(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail)).thenReturn(timelineSummaryCardView);
    when(licenceScheduleTimelineService.getAllowedEventActionsForUser(regulatorUser)).thenReturn(allowedActions);
    var scheduleComments = List.of(new ScheduleCommentView("comment", "", "1 January 2025 10:00:00", ""));
    when(scheduleCommentService.getCommentViews(licenceScheduleDetail.getLicenceSchedule())).thenReturn(scheduleComments);
    when(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail, allowedActions)).thenReturn(timelineActionViews);
    when(licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(eq(licenceScheduleDetail), any(), eq(allowedActions))).thenReturn(scheduleEventViews);

    mockMvc.perform(
            get(viewTimelineUrl)
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/timeline/scheduleTimeline"))
        .andExpect(model().attribute("pageTitle", LicenceScheduleTimelineController.PAGE_TITLE.formatted(licence.getLicenceReference())))
        .andExpect(model().attribute("timelineSummaryCardView", timelineSummaryCardView))
        .andExpect(model().attribute("scheduleComments", scheduleComments))
        .andExpect(model().attribute("actions", timelineActionViews))
        .andExpect(model().attribute("scheduleEventViews", scheduleEventViews))
        .andExpect(model().attribute("timelineFilterOptions", ScheduleTimelineFilterOption.getOptions()))
        .andExpect(model().attribute("updateLicenceStartDateUrl",
            ReverseRouter.route(on(LicenceStartDateController.class).renderLicenceStartDateUpdateForm(licenceScheduleDetail.getId(), null)))
        )
        .andExpect(model().attribute("updateExpiryDateUrl",
            ReverseRouter.route(on(LicenceScheduleExpiryController.class).renderAddUpdateLicenceExpiryPage(licenceScheduleDetail.getId(), null)))
        )
        .andExpect(model().attribute("reviewAndApplyUrl", ReverseRouter.route(on(ReviewAndApplyScheduleController.class)
            .renderReviewAndApplyPage(licenceScheduleDetail.getId(), null)))
        )
        .andExpect(model().attribute("clearFilterUrl", ReverseRouter.route(on(LicenceScheduleTimelineController.class)
            .clearFilters(licenceScheduleDetail.getId(), null, null)))
        )
        .andExpect(model().attribute("deleteScheduleUrl", ReverseRouter.route(on(DeleteDraftScheduleController.class)
            .renderDeleteDraftPage(licenceScheduleDetail.getId(), null)))
        );
  }

  @Test
  void renderLicenceScheduleTimeline_withAreaEvents_rendersThemInTermsAndPhases() throws Exception {
    when(licenceService.findLicenceByIdOrThrow(licence.getId())).thenReturn(licence);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);

    var inATerm = new TimelineAreaEventView("Area one (A1)", "Development area", "Approval: 01-03-2025",
        LocalDate.of(2025, Month.MARCH, 1), "1 March 2025", true);
    var inAPhase = new TimelineAreaEventView("Area two (A2v2)", "Pre-producing area", "Drill one well",
        LocalDate.of(2031, Month.MARCH, 1), "1 March 2031", false);
    var phase = new TimelinePhaseView(List.of(inAPhase), List.of(), PhaseType.PHASE_A, LocalDate.of(2030, Month.JANUARY, 1),
        "", "", "", "", "", List.of(), false, false);
    var scheduleEventViews = List.of(
        new TimelineTermView(List.of(inATerm), List.of(), TermType.INITIAL, "", "", "", "", "", false, List.of(), true, true),
        new TimelineTermView(List.of(phase), List.of(), TermType.SECOND, "", "", "", "", "", true, List.of(), false, false)
    );
    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    when(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .thenReturn(new TimelineSummaryCardView("date", "date2", true, "1", LicenceStatusType.EXTANT.getDisplayName(), "", "",
            List.of()));
    when(licenceScheduleTimelineService.getAllowedEventActionsForUser(regulatorUser)).thenReturn(allowedActions);
    when(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail, allowedActions))
        .thenReturn(List.of());
    when(licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(eq(licenceScheduleDetail), any(), eq(allowedActions)))
        .thenReturn(scheduleEventViews);

    mockMvc.perform(
            get(viewTimelineUrl)
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Area one (A1)")))
        .andExpect(content().string(containsString("Development area")))
        .andExpect(content().string(containsString("Approval: 01-03-2025")))
        .andExpect(content().string(containsString("Area two (A2v2)")))
        .andExpect(content().string(containsString("Drill one well")))
        .andExpect(content().string(containsString(ScheduleEventType.AREA.getDisplayName())));
  }

  @Test
  void renderLicenceScheduleTimeline_withAreaEventsAfterTheTerms_rendersThemAfterTheLastTermEndingTheLineOnTheLast()
      throws Exception {
    when(licenceService.findLicenceByIdOrThrow(licence.getId())).thenReturn(licence);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);

    var firstAfter = new TimelineAreaEventView("Area one (A1)", "Retention area", "",
        LocalDate.of(2025, Month.JANUARY, 1), "1 January 2025", false);
    var lastAfter = new TimelineAreaEventView("Area two (A2v2)", "Pre-producing area", "Expiry: 22-05-2030",
        LocalDate.of(2030, Month.MAY, 22), "22 May 2030", false);
    var phase = new TimelinePhaseView(List.of(), List.of(), PhaseType.PHASE_A, LocalDate.of(2008, Month.JULY, 1),
        "", "", "", "", "", List.of(), true, true);
    var scheduleEventViews = List.of(
        new TimelineTermView(List.of(phase), List.of(), TermType.INITIAL, "", "", "", "", "", true, List.of(),
            true, true, List.of()),
        new TimelineTermView(List.of(), List.of(), TermType.SECOND, "", "30 June 2019", "", "", "", false, List.of(), true,
            true, List.of(firstAfter, lastAfter))
    );
    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    when(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .thenReturn(new TimelineSummaryCardView("date", "date2", true, "1", LicenceStatusType.EXTANT.getDisplayName(), "", "",
            List.of()));
    when(licenceScheduleTimelineService.getAllowedEventActionsForUser(regulatorUser)).thenReturn(allowedActions);
    when(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail, allowedActions))
        .thenReturn(List.of());
    when(licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(eq(licenceScheduleDetail), any(), eq(allowedActions)))
        .thenReturn(scheduleEventViews);

    mockMvc.perform(
            get(viewTimelineUrl)
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(content().string(stringContainsInOrder(
            "Initial Term", "After the Second Term", "30 June 2019",
            "<li class=\"fds-timeline__time-stamp \">", "Area one (A1)",
            "<li class=\"fds-timeline__time-stamp fds-timeline__time-stamp--no-border\">", "Area two (A2v2)",
            "Expiry: 22-05-2030"
        )));
  }

  @Test
  void renderLicenceScheduleTimeline_withARelinquishmentPercentage_showsItBeforeTheDescription() throws Exception {
    when(licenceService.findLicenceByIdOrThrow(licence.getId())).thenReturn(licence);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);

    var relinquishment = new TimelineOtherScheduleEventView("Mandatory relinquishment",
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT, "Relinquish the northern blocks",
        "50%", LocalDate.of(2013, Month.FEBRUARY, 12), "12 February 2013", "", "", "", List.of(), true);
    var scheduleEventViews = List.of(
        new TimelineTermView(List.of(relinquishment), List.of(), TermType.INITIAL, "", "", "", "", "", false, List.of(), true,
            true)
    );
    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    when(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .thenReturn(new TimelineSummaryCardView("date", "date2", true, "1", LicenceStatusType.EXTANT.getDisplayName(), "", "",
            List.of()));
    when(licenceScheduleTimelineService.getAllowedEventActionsForUser(regulatorUser)).thenReturn(allowedActions);
    when(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail, allowedActions))
        .thenReturn(List.of());
    when(licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(eq(licenceScheduleDetail), any(), eq(allowedActions)))
        .thenReturn(scheduleEventViews);

    mockMvc.perform(
            get(viewTimelineUrl)
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(content().string(stringContainsInOrder(
            "Mandatory relinquishment", "50%", "Relinquish the northern blocks")));
  }

  @Test
  void renderLicenceScheduleTimeline_noScheduleAdminRole_doesNotAddEditDateUrls() throws Exception {
    when(licenceService.findLicenceByIdOrThrow(licence.getId())).thenReturn(licence);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);

    var timelineSummaryCardView = new TimelineSummaryCardView("date", "date2", true, "1", LicenceStatusType.EXTANT.getDisplayName(), "", "", List.of());
    var allowedActions = List.of(ScheduleEventAction.EDIT_WORK_PROGRAMME);

    when(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail)).thenReturn(timelineSummaryCardView);
    when(licenceScheduleTimelineService.getAllowedEventActionsForUser(regulatorUser)).thenReturn(allowedActions);
    when(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail, allowedActions)).thenReturn(List.of());
    when(licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(eq(licenceScheduleDetail), any(), eq(allowedActions))).thenReturn(List.of());

    mockMvc.perform(
            get(viewTimelineUrl)
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attributeDoesNotExist("updateLicenceStartDateUrl"))
        .andExpect(model().attributeDoesNotExist("updateExpiryDateUrl"));
  }

  @Test
  void filterTimeline() throws Exception {
    mockMvc.perform(
            post(viewTimelineUrl)
                .with(csrf())
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(viewTimelineUrl));
  }

  @Test
  void clearFilters() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTimelineController.class).clearFilters(licenceScheduleDetail.getId(), null, null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(viewTimelineUrl));
  }
}