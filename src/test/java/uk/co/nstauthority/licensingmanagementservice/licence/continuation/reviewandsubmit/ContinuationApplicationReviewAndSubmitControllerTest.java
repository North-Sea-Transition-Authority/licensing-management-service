package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.feedback.FeedbackController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = ContinuationApplicationReviewAndSubmitController.class)
class ContinuationApplicationReviewAndSubmitControllerTest extends AbstractControllerTest {

  private static final String CAPTION = "Licence type - Licence ref";
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();

  @MockitoBean
  ContinuationSummarySectionService continuationSummarySectionService;

  @MockitoBean
  LicenceContinuationApplicationTaskListService licenceContinuationApplicationTaskListService;

  @MockitoBean
  LicenceContinuationApplication licenceContinuationApplication;

  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;
  private static final UUID LICENCE_CONTINUATION_APPLICATION_DETAIL_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    licenceContinuationApplicationDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(LICENCE_CONTINUATION_APPLICATION_DETAIL_ID)
        .withStatus(LicenceContinuationApplicationStatus.DRAFT)
        .build();

    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL_ID)).thenReturn(licenceContinuationApplicationDetail);
    when(workProgrammeActivityService.getCurrentWorkProgrammeActivitiesViews(any())).thenReturn(List.of());
  }

  @Test
  void getReviewAndSubmit() throws Exception {
    var applicationDetailId = licenceContinuationApplicationDetail.getId();
    when(licenceService.getLicencePageCaption(any())).thenReturn(CAPTION);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);

    var resultActions = mockMvc.perform(
        get(ReverseRouter.route(on(ContinuationApplicationReviewAndSubmitController.class).getReviewAndSubmit(applicationDetailId, null, null)))
            .with(user(USER))
        )
        .andExpect(status().isOk());

    assertRenderPageModelsAttributesArePresent(resultActions, applicationDetailId);
  }

  @Test
  void getReviewAndSubmit_UserCantSubmit() throws Exception {
    var applicationDetailId = licenceContinuationApplicationDetail.getId();
    when(licenceService.getLicencePageCaption(any())).thenReturn(CAPTION);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);
    when(licenceContinuationService.userCanSubmitApplication(licenceContinuationApplicationDetail, USER)).thenReturn(false);

    var resultActions = mockMvc.perform(
            get(ReverseRouter.route(on(ContinuationApplicationReviewAndSubmitController.class).getReviewAndSubmit(applicationDetailId, null, null))).with(user(USER))
        )
        .andExpect(status().isOk());

    assertRenderPageModelsAttributesArePresent(resultActions, applicationDetailId);
    resultActions.andExpect(model().attribute("userCanSubmit", false));
  }

  @Test
  void submitApplication_notSubmittable() throws Exception {
    var applicationDetailId = licenceContinuationApplicationDetail.getId();
    when(licenceService.getLicencePageCaption(any())).thenReturn(CAPTION);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);
    when(licenceContinuationApplicationTaskListService.isSubmittable(any(), any())).thenReturn(false);

    var resultActions = mockMvc.perform(
        post(ReverseRouter.route(on(ContinuationApplicationReviewAndSubmitController.class).submitApplication(applicationDetailId, null, null, null)))
                     .with(user(USER))
                     .with(csrf())
        )
        .andExpect(status().isOk());

    assertRenderPageModelsAttributesArePresent(resultActions, applicationDetailId);
    resultActions.andExpect(model().attribute("isSubmittable", false));
  }

  @Test
  void submitApplication_submittable() throws Exception {
    var applicationDetailId = licenceContinuationApplicationDetail.getId();
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);
    when(licenceContinuationApplicationTaskListService.isSubmittable(any(), any())).thenReturn(true);

    when(licenceContinuationApplication.getApplicationReference()).thenReturn("APP-REF-123");
    when(licenceContinuationService.submitApplication(any(), any())).thenReturn(licenceContinuationApplication);

    mockMvc.perform(
        post(ReverseRouter.route(on(ContinuationApplicationReviewAndSubmitController.class).submitApplication(applicationDetailId, null, null, null)))
                     .with(user(USER))
                     .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/submissionConfirmation"))
        .andExpect(model().attribute("feedbackUrl", ReverseRouter.route(on(FeedbackController.class).getFeedback(null))))
        .andExpect(model().attribute("workAreaUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))))
        .andExpect(model().attribute("applicationReference", licenceContinuationApplication.getApplicationReference()));
  }

  @ParameterizedTest
  @EnumSource(value = LicenceContinuationApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void renderPage_assertForbiddenOnNotDraft(LicenceContinuationApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(id)
        .withStatus(status)
        .build();

    when(licenceContinuationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);

    mockMvc.perform(
        get(ReverseRouter.route(on(ContinuationApplicationReviewAndSubmitController.class).getReviewAndSubmit(id, null, null))).with(user(USER))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void renderPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(false);
    mockMvc.perform(
        get(ReverseRouter.route(on(ContinuationApplicationReviewAndSubmitController.class).getReviewAndSubmit(LICENCE_CONTINUATION_APPLICATION_DETAIL_ID, null, null))).with(user(USER))
        )
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = LicenceContinuationApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void submitPage_assertForbiddenOnNotDraft(LicenceContinuationApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(id)
        .withStatus(status)
        .build();

    when(licenceContinuationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);

    mockMvc.perform(
        post(ReverseRouter.route(on(ContinuationApplicationReviewAndSubmitController.class).submitApplication(id, null, null, null)))
                     .with(user(USER))
                     .with(csrf())
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(false);
    mockMvc.perform(
        post(ReverseRouter.route(on(ContinuationApplicationReviewAndSubmitController.class).submitApplication(LICENCE_CONTINUATION_APPLICATION_DETAIL_ID, null, null, null)))
                     .with(user(USER))
                     .with(csrf())
        )
        .andExpect(status().isForbidden());
  }

  private void assertRenderPageModelsAttributesArePresent(
      ResultActions resultActions,
      UUID id
  ) throws Exception {
    resultActions
        .andExpect(view().name("lms/licence/continuation/reviewAndSubmit"))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(id, null, null))))
        .andExpect(model().attribute("pageCaption", CAPTION))
        .andExpect(model().attribute("summarySections", continuationSummarySectionService.getSummarySections(licenceContinuationApplicationDetail, null)))
        .andExpect(model().attribute("accordionId", licenceContinuationApplicationDetail.getId()))
        .andExpect(model().attributeExists("isSubmittable"))
        .andExpect(model().attributeExists("userCanSubmit"))
        .andExpect(model().attribute("submitterRoleName", Role.APPLICATION_SUBMITTER.getName()));
  }
}