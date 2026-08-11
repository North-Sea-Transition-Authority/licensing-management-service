package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineSummaryCardView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineTermView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicenceScheduleHistoryOverviewController.class)
class LicenceScheduleHistoryOverviewControllerTest extends AbstractControllerTest {
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(ORGANISATION_USER_WUA_ID)
      .build();

  @MockitoBean
  private LicenceScheduleTimelineService licenceScheduleTimelineService;

  private Licence licence;

  private LicenceScheduleDetail licenceScheduleDetail;

  private String viewOverviewUrl;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceReference("P1")
        .withRoundIssuedOn("1")
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    viewOverviewUrl = ReverseRouter.route(on(LicenceScheduleHistoryOverviewController.class)
        .renderLicenceOverview(licenceScheduleDetail.getId(), null, null, null));
  }

  @Test
  void renderLicenceOverview() throws Exception {
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);

    var actions = List.of(LicenceActionItem.EDIT_LICENCE_DETAILS.toActionItemView(licence));

    when(licenceActionService.getAvailableUserActionItems(licence, USER)).thenReturn(actions);

    var scheduleHistoryOptions = Map.of(licenceScheduleDetail.getId().toString(), "1 January 2024 10:00am");
    when(licenceScheduleDetailService.getScheduleDetailHistoryOptions(licence)).thenReturn(scheduleHistoryOptions);

    var timelineSummaryCardView = new TimelineSummaryCardView("date", "date2", true, "1", LicenceStatusType.EXTANT.getDisplayName(), "", "");
    var scheduleEventViews = List.of(new TimelineTermView(List.of(), List.of(), TermType.INITIAL, "", "", "", "", "", true, List.of(), true, true));

    when(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail)).thenReturn(timelineSummaryCardView);
    when(licenceScheduleTimelineService.getLicenceScheduleEventViewsForOverview(eq(licenceScheduleDetail), any(), any())).thenReturn(scheduleEventViews);

    mockMvc.perform(
            get(viewOverviewUrl)
                .with(user(USER))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/licenceOverview"))
        .andExpect(model().attribute("licenceReference", licence.getLicenceReference()))
        .andExpect(model().attribute("caption", licence.getType().getDisplayName()))
        .andExpect(model().attribute("licenceActions", actions))
        .andExpect(model().attributeExists("historyForm"))
        .andExpect(model().attribute("scheduleHistoryOptions", scheduleHistoryOptions))
        .andExpect(model().attribute("viewScheduleHistoryUrl", ReverseRouter.route(on(LicenceScheduleHistoryOverviewController.class)
            .viewScheduleHistory(licenceScheduleDetail.getId(), null)))
        )
        .andExpect(model().attribute("timelineSummaryCardView", timelineSummaryCardView))
        .andExpect(model().attribute("scheduleEventViews", scheduleEventViews))
        .andExpect(model().attribute("timelineFilterOptions", ScheduleEventType.getFilterableEventTypeOptions()))
        .andExpect(model().attribute("clearFilterUrl", ReverseRouter.route(on(LicenceScheduleHistoryOverviewController.class)
            .clearFilters(licenceScheduleDetail.getId(), null, null)))
        );
  }

  @Test
  void filterTimeline() throws Exception {
    mockMvc.perform(
            post(viewOverviewUrl)
                .with(csrf())
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(viewOverviewUrl));
  }

  @Test
  void clearFilters() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleHistoryOverviewController.class)
                .clearFilters(licenceScheduleDetail.getId(), null, null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(viewOverviewUrl));
  }

  @Test
  void viewScheduleHistory_whenLicenceScheduleDetailIdProvided_redirectsToSelectedVersion() throws Exception {
    var otherLicenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceScheduleDetail.getLicenceSchedule());

    var viewScheduleHistoryUrl = ReverseRouter.route(on(LicenceScheduleHistoryOverviewController.class)
        .viewScheduleHistory(licenceScheduleDetail.getId(), null));

    var expectedRedirectUrl = ReverseRouter.route(on(LicenceScheduleHistoryOverviewController.class)
        .renderLicenceOverview(otherLicenceScheduleDetail.getId(), null, null, null));

    mockMvc.perform(
            post(viewScheduleHistoryUrl)
                .param("licenceScheduleDetailId", otherLicenceScheduleDetail.getId().toString())
                .with(csrf())
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(expectedRedirectUrl));
  }

  @Test
  void viewScheduleHistory_whenLicenceScheduleDetailIdNotProvided_redirectsToOverview() throws Exception {
    var viewScheduleHistoryUrl = ReverseRouter.route(on(LicenceScheduleHistoryOverviewController.class)
        .viewScheduleHistory(licenceScheduleDetail.getId(), null));

    mockMvc.perform(
            post(viewScheduleHistoryUrl)
                .with(csrf())
                .with(user(regulatorUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(viewOverviewUrl));
  }
}
