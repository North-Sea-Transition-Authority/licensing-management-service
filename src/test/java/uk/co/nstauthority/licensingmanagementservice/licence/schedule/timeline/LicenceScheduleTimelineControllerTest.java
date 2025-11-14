package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceScheduleTimelineController.class)
class LicenceScheduleTimelineControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleTimelineService licenceScheduleTimelineService;

  @MockitoBean
  private LicenceTypeRulesResolver licenceTypeRulesResolver;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private Licence licence;

  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceReference("P1")
        .withRoundIssuedOn("1")
        .withStatus(LicenceStatus.EXTANT)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
  }

  @SecurityTest
  void renderLicenceScheduleTimeline() throws Exception {
    when(licenceService.findLicenceByIdOrThrow(licence.getId())).thenReturn(licence);
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(licence, LicenceScheduleDetailStatus.DRAFT))
        .thenReturn(licenceScheduleDetail);

    var timelineSummaryCardView = new TimelineSummaryCardView("date", "1", LicenceStatus.EXTANT.getDisplayText());
    var timelineActionViews = List.of(new TimelineActionView(LicenceScheduleTimelineAction.ADD_A_TERM, ""));
    var scheduleEventViews = List.of(new TimelineTermView(List.of(), TermType.INITIAL, "", "", "", "", true));

    when(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail)).thenReturn(timelineSummaryCardView);
    when(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail)).thenReturn(timelineActionViews);
    when(licenceScheduleTimelineService.getLicenceScheduleEventViews(licenceScheduleDetail)).thenReturn(scheduleEventViews);
    when(licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType())).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTimelineController.class).renderLicenceScheduleTimeline(licence.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/timeline/scheduleTimeline"))
        .andExpect(model().attribute("pageTitle", LicenceScheduleTimelineController.PAGE_TITLE.formatted(licence.getLicenceReference())))
        .andExpect(model().attribute("timelineSummaryCardView", timelineSummaryCardView))
        .andExpect(model().attribute("actions", timelineActionViews))
        .andExpect(model().attribute("scheduleEventViews", scheduleEventViews))
        .andExpect(model().attribute("updateLicenceStartDateUrl",
            ReverseRouter.route(on(LicenceStartDateController.class).renderLicenceStartDateUpdateForm(licenceScheduleDetail.getId(), null))))
        .andExpect(model().attribute("showRoundIssuedOn", true));
  }

  @SecurityTest
  void renderLicenceScheduleTimeline_withScheduleDetail() throws Exception {
    when(licenceService.findLicenceByIdOrThrow(licence.getId())).thenReturn(licence);
    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);

    var timelineSummaryCardView = new TimelineSummaryCardView("date", "1", LicenceStatus.EXTANT.getDisplayText());
    var timelineActionViews = List.of(new TimelineActionView(LicenceScheduleTimelineAction.ADD_A_TERM, ""));
    var scheduleEventViews = List.of(new TimelineTermView(List.of(), TermType.INITIAL, "", "", "", "", true));

    when(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail)).thenReturn(timelineSummaryCardView);
    when(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail)).thenReturn(timelineActionViews);
    when(licenceScheduleTimelineService.getLicenceScheduleEventViews(licenceScheduleDetail)).thenReturn(scheduleEventViews);
    when(licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType())).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTimelineController.class)
                .renderLicenceScheduleTimeline(licenceScheduleDetail.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/timeline/scheduleTimeline"))
        .andExpect(model().attribute("pageTitle", LicenceScheduleTimelineController.PAGE_TITLE.formatted(licence.getLicenceReference())))
        .andExpect(model().attribute("timelineSummaryCardView", timelineSummaryCardView))
        .andExpect(model().attribute("actions", timelineActionViews))
        .andExpect(model().attribute("scheduleEventViews", scheduleEventViews))
        .andExpect(model().attribute("updateLicenceStartDateUrl",
            ReverseRouter.route(on(LicenceStartDateController.class).renderLicenceStartDateUpdateForm(licenceScheduleDetail.getId(), null))))
        .andExpect(model().attribute("showRoundIssuedOn", false));
  }
}