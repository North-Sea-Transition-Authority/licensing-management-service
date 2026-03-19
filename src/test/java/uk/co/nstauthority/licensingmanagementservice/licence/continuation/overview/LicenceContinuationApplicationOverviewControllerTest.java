package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action.LicenceContinuationActionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit.ContinuationSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceContinuationApplicationOverviewController.class)
class LicenceContinuationApplicationOverviewControllerTest extends AbstractControllerTest {

  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(ORGANISATION_USER_WUA_ID)
      .build();

  @MockitoBean
  private LicenceContinuationApplicationOverviewService overviewService;

  @MockitoBean
  private ContinuationSummarySectionService continuationSummarySectionService;

  @MockitoBean
  private LicenceContinuationActionService licenceContinuationActionService;

  @SecurityTest
  void renderOverview_displaysApplicationContextAndSummaries() throws Exception {
    var licence = LicenceTestUtil .builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS1")
        .build();
    var applicationDetailId = UUID.randomUUID();
    var submittedDatetime = Instant.parse("2024-03-15T10:30:00Z");

    var applicationDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(applicationDetailId)
        .withStatus(LicenceContinuationApplicationStatus.SUBMITTED)
        .withSubmittedDatetime(submittedDatetime)
        .withApplicationReference("LMS/CA/2024/1")
        .build();

    var applicationContext = new LicenceContinuationApplicationContext(
        "LMS/CA/2024/1",
        "Carbon storage licence - CS1",
        List.of(SummaryDataView.newBuilder()
                    .addStringValue("Submitted by", "John Smith")
                    .addStringValue("Submission date", "15 March 2024")
                    .build())
    );

    when(licenceContinuationService.getDetailByIdOrThrow(applicationDetailId))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any()))
        .thenReturn(true);
    when(licenceContinuationService.getLicenceFromContinuationApplicationDetail(applicationDetail))
        .thenReturn(licence);
    when(overviewService.getApplicationContext(applicationDetail, licence))
        .thenReturn(applicationContext);
    when(continuationSummarySectionService.getSummarySections(applicationDetail, USER))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getLicenceWorkProgramActivitiesViews(any()))
        .thenReturn(List.of());
    when(licenceContinuationActionService.getAvailableUserActionItems(applicationDetail, USER))
        .thenReturn(List.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).renderOverview(applicationDetailId, applicationDetail, null)))
                .with(user(USER))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/licenceContinuationApplicationOverview"))
        .andExpect(model().attribute("applicationContext", applicationContext))
        .andExpect(model().attribute("summarySections", List.of()))
        .andExpect(model().attribute("workProgrammeActivities", List.of()))
        .andExpect(model().attribute("accordionId", applicationDetailId))
        .andExpect(model().attribute("applicationActions", List.of()));
  }
}