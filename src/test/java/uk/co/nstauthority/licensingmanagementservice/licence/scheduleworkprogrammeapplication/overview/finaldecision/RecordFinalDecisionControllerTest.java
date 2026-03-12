package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.finaldecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import static uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.finaldecision.RecordFinalDecisionController.PAGE_TITLE;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadComponentAttributes;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanAccessScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleAmendmentApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.file.FileControllerHelperService;
import uk.co.nstauthority.licensingmanagementservice.file.FileUploadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action.ScheduleWorkProgrammeApplicationActionItem;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.AnnotationSecurityTest;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = RecordFinalDecisionController.class)
class RecordFinalDecisionControllerTest extends AbstractControllerTest {

  private static final Long REGULATOR_WUA_ID = 1L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(REGULATOR_WUA_ID)
      .build();
  private static final FileUploadComponentAttributes FILE_UPLOAD_ATTRIBUTES = FileUploadTestUtil
      .getFileUploadComponentAttributesBuilder("form.finalDecisionSupportPapers")
      .build();

  @MockitoBean
  private RecordFinalDecisionService recordFinalDecisionService;

  @MockitoBean
  private RecordFinalDecisionFormValidator recordFinalDecisionFormValidator;

  @MockitoBean
  private FileControllerHelperService fileControllerHelperService;

  @AnnotationSecurityTest
  void render_classAnnotations_presentAndCorrect() {
    assertThat(RecordFinalDecisionController.class)
        .hasAnnotation(ScheduleAmendmentApplicationHasStatus.class);
    assertThat(RecordFinalDecisionController.class.getAnnotation(ScheduleAmendmentApplicationHasStatus.class).value())
        .containsOnly(ScheduleWorkProgrammeApplicationStatus.SUBMITTED);
    assertThat(RecordFinalDecisionController.class)
        .hasAnnotation(InvokingUserCanAccessScheduleApplication.class);
    assertThat(RecordFinalDecisionController.class)
        .hasAnnotation(ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint.class);
    assertThat(RecordFinalDecisionController.class
        .getAnnotation(ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule.ActionEndPoint.class).value())
        .containsOnly(ScheduleWorkProgrammeApplicationActionItem.RECORD_FINAL_DECISION);
  }

  @SecurityTest
  void render_noApplicationAccess_returnsForbidden() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(
        applicationDetail.getScheduleWorkProgrammeApplication().getId().toString(),
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        applicationDetail.getResponsibleOrganisationUnitId(),
        REGULATOR_WUA_ID))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordFinalDecisionController.class).render(applicationDetailId, null)))
                .with(user(USER))
        )
        .andExpect(status().isForbidden());
  }

  @SecurityTest
  void render_noActionAccess_returnsForbidden() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(
        applicationDetail.getScheduleWorkProgrammeApplication().getId().toString(),
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        applicationDetail.getResponsibleOrganisationUnitId(),
        REGULATOR_WUA_ID))
        .thenReturn(true);
    when(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, USER))
        .thenReturn(List.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordFinalDecisionController.class).render(applicationDetailId, null)))
                .with(user(USER))
        )
        .andExpect(status().isForbidden());
  }

  @SecurityTest
  void render_withAccess_returnsOk() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordFinalDecisionService.getFormForApplication(applicationDetail))
        .thenReturn(new RecordFinalDecisionForm());
    when(scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(createLicence());
    mockFileUploadComponentAttributes();

    mockMvc.perform(
            get(ReverseRouter.route(on(RecordFinalDecisionController.class).render(applicationDetailId, null)))
                .with(user(USER))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/recordFinalDecision"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("caption", LicenceType.CARBON_STORAGE.getDisplayName()))
        .andExpect(model().attribute("fileUploadAttributes", FILE_UPLOAD_ATTRIBUTES))
        .andExpect(model().attributeExists("form", "backUrl"));
  }

  @SecurityTest
  void save_validForm_redirectsToOverview() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordFinalDecisionFormValidator.isValid(any(RecordFinalDecisionForm.class), any(Errors.class)))
        .thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordFinalDecisionController.class)
                .save(applicationDetailId, null, null, null, null)))
                .with(user(USER))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(WorkAreaController.class)
            .getWorkArea(null, null))));
  }

  @SecurityTest
  void save_invalidForm_returnsForm() throws Exception {
    var applicationDetailId = UUID.randomUUID();
    var applicationDetail = buildApplicationDetail(applicationDetailId);

    setupPassingInterceptors(applicationDetail);
    when(recordFinalDecisionFormValidator.isValid(any(RecordFinalDecisionForm.class), any(Errors.class)))
        .thenReturn(false);
    when(scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(createLicence());
    mockFileUploadComponentAttributes();

    mockMvc.perform(
            post(ReverseRouter.route(on(RecordFinalDecisionController.class)
                .save(applicationDetailId, null, null, null, null)))
                .with(user(USER))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/recordFinalDecision"));
  }

  private void mockFileUploadComponentAttributes() {
    when(fileControllerHelperService.fileUploadComponentAttributes(anyList(), any(Class.class), any(Function.class), any(Function.class), any(String.class)))
        .thenReturn(FILE_UPLOAD_ATTRIBUTES);
  }

  private ScheduleWorkProgrammeApplicationDetail buildApplicationDetail(UUID applicationDetailId) {
    return ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(applicationDetailId)
        .withStatus(ScheduleWorkProgrammeApplicationStatus.SUBMITTED)
        .build();
  }

  private void setupPassingInterceptors(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetail.getId()))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(
        applicationDetail.getScheduleWorkProgrammeApplication().getId().toString(),
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        applicationDetail.getResponsibleOrganisationUnitId(),
        REGULATOR_WUA_ID))
        .thenReturn(true);
    when(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, USER))
        .thenReturn(List.of(
            ScheduleWorkProgrammeApplicationActionItem.RECORD_FINAL_DECISION.toActionItemView(applicationDetail)));
  }

  private Licence createLicence() {
    var licence = new Licence();
    licence.setId(1);
    licence.setType(LicenceType.CARBON_STORAGE);
    licence.setLicenceReference("CS1");
    return licence;
  }
}
