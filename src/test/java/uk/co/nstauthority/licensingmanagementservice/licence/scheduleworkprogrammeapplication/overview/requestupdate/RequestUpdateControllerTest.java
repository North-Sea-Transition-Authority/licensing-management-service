package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.requestupdate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
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
import static uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.requestupdate.RequestUpdateController.PAGE_TITLE;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = RequestUpdateController.class)
class RequestUpdateControllerTest extends AbstractControllerTest {

  private static final Long REGULATOR_WUA_ID = 1L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(REGULATOR_WUA_ID)
      .build();

  @MockitoBean
  private RequestUpdateService requestUpdateService;

  @MockitoBean
  private RequestUpdateFormValidator requestUpdateFormValidator;

  @Test
  void classAnnotations_presentAndCorrect() {
    assertThat(RequestUpdateController.class)
        .hasAnnotation(ScheduleAmendmentApplicationHasStatus.class)
        .hasAnnotation(InvokingUserCanAccessScheduleApplication.class)
        .hasAnnotation(ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint.class);
    assertThat(RequestUpdateController.class.getAnnotation(ScheduleAmendmentApplicationHasStatus.class).value())
        .containsOnly(ApplicationStatus.SUBMITTED);
    assertThat(RequestUpdateController.class
        .getAnnotation(ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint.class).value())
        .containsOnly(ScheduleWorkProgrammeApplicationActionItem.REQUEST_UPDATE);
  }

  @Test
  void render_noActionAccess_returnsForbidden() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(eq(applicationDetail), anyMap(), eq(REGULATOR_WUA_ID)))
        .thenReturn(true);
    when(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, USER))
        .thenReturn(List.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(RequestUpdateController.class).render(applicationDetailId, null)))
                .with(user(USER))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void render_withAccess_returnsForm() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(requestUpdateService.getFormForApplication(applicationDetail)).thenReturn(new RequestUpdateForm());

    mockMvc.perform(
            get(ReverseRouter.route(on(RequestUpdateController.class).render(applicationDetailId, null)))
                .with(user(USER))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/requestUpdate"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("caption", LicenceType.CARBON_STORAGE.getDisplayName()))
        .andExpect(model().attributeExists("form", "backUrl"));
  }

  @Test
  void save_validForm_savesAndRedirectsToOverview() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(requestUpdateFormValidator.isValid(any(RequestUpdateForm.class), any(Errors.class))).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(RequestUpdateController.class).save(applicationDetailId, null, null, null, null, null)))
                .with(user(USER))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
            .renderOverview(applicationDetailId, null, null, null))));

    verify(requestUpdateService).requestUpdate(eq(applicationDetail), any(RequestUpdateForm.class), eq(USER));
  }

  @Test
  void save_invalidForm_returnsForm() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(requestUpdateFormValidator.isValid(any(RequestUpdateForm.class), any(Errors.class))).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(RequestUpdateController.class).save(applicationDetailId, null, null, null, null, null)))
                .with(user(USER))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/application/requestUpdate"));
  }

  private ScheduleWorkProgrammeApplicationDetail buildApplicationDetail(UUID applicationDetailId) {
    var licence = new Licence();
    licence.setId(1);
    licence.setType(LicenceType.CARBON_STORAGE);
    licence.setLicenceReference("CS1");
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
    var swpApp = ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplication(licenceScheduleDetail);
    return ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(applicationDetailId)
        .withStatus(ApplicationStatus.SUBMITTED)
        .withScheduleWorkProgrammeApplication(swpApp)
        .build();
  }

  private void setupPassingInterceptors(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetail.getId()))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(eq(applicationDetail), anyMap(), eq(REGULATOR_WUA_ID)))
        .thenReturn(true);
    when(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, USER))
        .thenReturn(List.of(
            ScheduleWorkProgrammeApplicationActionItem.REQUEST_UPDATE.toActionItemView(applicationDetail)));
  }
}
