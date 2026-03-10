package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.reviewandapply.DeleteDraftScheduleController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.reviewandapply.ReviewAndApplyScheduleController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceScheduleTimelineController.class)
class LicenceScheduleTimelineControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleTimelineService licenceScheduleTimelineService;

  private Licence licence;

  private LicenceScheduleDetail licenceScheduleDetail;

  private String viewTimelineUrl;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceReference("P1")
        .withRoundIssuedOn("1")
        .withStatus(LicenceStatus.EXTANT)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    viewTimelineUrl = ReverseRouter.route(on(LicenceScheduleTimelineController.class)
        .renderLicenceScheduleTimeline(licenceScheduleDetail.getId(), null, null));
  }

  @SecurityTest
  void renderLicenceScheduleTimeline() throws Exception {
    when(licenceService.findLicenceByIdOrThrow(licence.getId())).thenReturn(licence);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);

    var timelineSummaryCardView = new TimelineSummaryCardView("date", "date2", true, "1", LicenceStatus.EXTANT.getDisplayName());
    var timelineActionViews = List.of(new TimelineActionView(LicenceScheduleTimelineAction.ADD_A_TERM, ""));
    var scheduleEventViews = List.of(new TimelineTermView(List.of(), List.of(), TermType.INITIAL, "", "", "", "", true));

    when(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail)).thenReturn(timelineSummaryCardView);
    when(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail)).thenReturn(timelineActionViews);
    when(licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(eq(licenceScheduleDetail), any())).thenReturn(scheduleEventViews);

    mockMvc.perform(
            get(viewTimelineUrl)
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/timeline/scheduleTimeline"))
        .andExpect(model().attribute("pageTitle", LicenceScheduleTimelineController.PAGE_TITLE.formatted(licence.getLicenceReference())))
        .andExpect(model().attribute("timelineSummaryCardView", timelineSummaryCardView))
        .andExpect(model().attribute("actions", timelineActionViews))
        .andExpect(model().attribute("scheduleEventViews", scheduleEventViews))
        .andExpect(model().attribute("timelineFilterOptions", ScheduleEventType.getFilterableEventTypeOptions()))
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