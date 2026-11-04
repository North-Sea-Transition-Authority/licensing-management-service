package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.requestupdate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import static uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.requestupdate.LicenceContinuationRequestUpdateController.PAGE_TITLE;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.InvokingUserCanAccessContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.LicenceContinuationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action.LicenceContinuationActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action.LicenceContinuationActionService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicenceContinuationRequestUpdateController.class)
class LicenceContinuationRequestUpdateControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build();
  private static final LicenceContinuationApplicationDetail APPLICATION_DETAIL =
      LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(
          LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(LICENCE)));
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().withWuaId(2L).build();

  @MockitoBean
  private LicenceContinuationRequestUpdateService requestUpdateService;

  @MockitoBean
  private RequestUpdateFormValidator requestUpdateFormValidator;

  @MockitoBean
  private LicenceContinuationActionService licenceContinuationActionService;

  @BeforeEach
  void setUp() {
    APPLICATION_DETAIL.setStatus(ApplicationStatus.SUBMITTED);
    when(licenceContinuationService.getDetailByIdOrThrow(APPLICATION_DETAIL.getId())).thenReturn(APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);
    when(licenceContinuationActionService.getAvailableUserActionItems(any(), any()))
        .thenReturn(List.of(LicenceContinuationActionItem.REQUEST_UPDATE.toActionItemView(APPLICATION_DETAIL)));
  }

  @Test
  void classAnnotations_presentAndCorrect() {
    assertThat(LicenceContinuationRequestUpdateController.class)
        .hasAnnotation(ContinuationApplicationHasStatus.class)
        .hasAnnotation(InvokingUserCanAccessContinuationApplication.class)
        .hasAnnotation(LicenceContinuationActionEndPointInterceptorRule.ActionEndPoint.class);
    assertThat(LicenceContinuationRequestUpdateController.class
        .getAnnotation(ContinuationApplicationHasStatus.class).value())
        .containsOnly(ApplicationStatus.SUBMITTED);
    assertThat(LicenceContinuationRequestUpdateController.class
        .getAnnotation(LicenceContinuationActionEndPointInterceptorRule.ActionEndPoint.class).value())
        .containsOnly(LicenceContinuationActionItem.REQUEST_UPDATE);
  }

  @Test
  void render_withAccess_returnsForm() throws Exception {
    when(requestUpdateService.getFormForApplication(APPLICATION_DETAIL)).thenReturn(new RequestUpdateForm());

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationRequestUpdateController.class)
                .render(APPLICATION_DETAIL.getId(), null)))
                .with(user(USER))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/requestUpdate"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attributeExists("form", "backUrl", "caption"));
  }

  @Test
  void save_validForm_savesAndRedirectsToOverview() throws Exception {
    when(requestUpdateFormValidator.isValid(any(RequestUpdateForm.class), any(Errors.class))).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationRequestUpdateController.class)
                .save(APPLICATION_DETAIL.getId(), null, null, null, null, null)))
                .with(user(USER))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
            .renderOverview(APPLICATION_DETAIL.getId(), null, null, null))));

    verify(requestUpdateService).requestUpdate(any(LicenceContinuationApplicationDetail.class), any(RequestUpdateForm.class), eq(USER));
  }

  @Test
  void save_invalidForm_returnsForm() throws Exception {
    when(requestUpdateFormValidator.isValid(any(RequestUpdateForm.class), any(Errors.class))).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationRequestUpdateController.class)
                .save(APPLICATION_DETAIL.getId(), null, null, null, null, null)))
                .with(user(USER))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/requestUpdate"));
  }
}
