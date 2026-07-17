package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import static uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationOtherRequirementController.PAGE_TITLE;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.file.FileControllerHelperService;
import uk.co.nstauthority.licensingmanagementservice.file.FileUploadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = LicenceContinuationOtherRequirementController.class)
class LicenceContinuationOtherRequirementControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceContinuationOtherRequirementService licenceContinuationOtherRequirementService;

  @MockitoBean
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  @MockitoBean
  private LicenceContinuationOtherRequirementValidator licenceContinuationOtherRequirementValidator;

  @MockitoBean
  private OtherRequirementsVisibilityResolverService otherRequirementsVisibilityResolverService;

  @MockitoBean
  private FileControllerHelperService fileControllerHelperService;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL
      = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(LICENCE));
  private static final LicenceContinuationApplicationDetail LICENCE_CONTINUATION_APPLICATION_DETAIL
      = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(LICENCE_SCHEDULE_DETAIL);

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private static final OtherRequirementsVisibility VISIBILITY_WITH_REQUIREMENTS =
      new OtherRequirementsVisibility(true, false, false);

  private static final OtherRequirementsVisibility VISIBILITY_NO_REQUIREMENTS =
      new OtherRequirementsVisibility(false, false, false);

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
  }

  @Test
  void renderForm() throws Exception {
    var licenceContinuationOtherRequirementForm = new LicenceContinuationOtherRequirementForm();

    when(licenceContinuationOtherRequirementService.getLicenceContinuationOtherRequirementForm(any()))
        .thenReturn(licenceContinuationOtherRequirementForm);
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    when(licenceContinuationService.getScheduleDetailFromApplicationDetail(any()))
        .thenReturn(LICENCE_SCHEDULE_DETAIL);
    when(otherRequirementsVisibilityResolverService.resolveVisibility(LICENCE_CONTINUATION_APPLICATION_DETAIL))
        .thenReturn(VISIBILITY_WITH_REQUIREMENTS);
    when(fileControllerHelperService.fileUploadComponentAttributes(
        any(List.class), any(Class.class), any(Function.class), any(Function.class)))
        .thenReturn(FileUploadTestUtil.FILE_UPLOAD_COMPONENT_ATTRIBUTES);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationOtherRequirementController.class).renderForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/licenceContinuationOtherRequirement"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("form", licenceContinuationOtherRequirementForm))
        .andExpect(model().attribute("fileUploadAttributes", FileUploadTestUtil.FILE_UPLOAD_COMPONENT_ATTRIBUTES))
        .andExpect(model().attribute("otherRequirementsVisibility", VISIBILITY_WITH_REQUIREMENTS))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null))))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)), "Work area",
            ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null)), "Task list"
        )))
        .andExpect(model().attribute("currentPage", PAGE_TITLE));
  }

  @Test
  void renderForm_RedirectsWhenNoRequirements() throws Exception {
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    when(licenceContinuationService.getScheduleDetailFromApplicationDetail(any()))
        .thenReturn(LICENCE_SCHEDULE_DETAIL);
    when(otherRequirementsVisibilityResolverService.resolveVisibility(LICENCE_CONTINUATION_APPLICATION_DETAIL))
        .thenReturn(VISIBILITY_NO_REQUIREMENTS);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationOtherRequirementController.class).renderForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null))));
  }

  @Test
  void submitForm_Valid() throws Exception {
    var licenceContinuationOtherRequirementForm = new LicenceContinuationOtherRequirementForm();

    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    when(licenceContinuationService.getScheduleDetailFromApplicationDetail(any()))
        .thenReturn(LICENCE_SCHEDULE_DETAIL);
    when(otherRequirementsVisibilityResolverService.resolveVisibility(LICENCE_CONTINUATION_APPLICATION_DETAIL))
        .thenReturn(VISIBILITY_WITH_REQUIREMENTS);

    when(licenceContinuationOtherRequirementValidator.isValid(any(), any(), eq(VISIBILITY_WITH_REQUIREMENTS)))
        .thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationOtherRequirementController.class).submitForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, licenceContinuationOtherRequirementForm, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", licenceContinuationOtherRequirementForm)
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(
            ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null))));

    verify(licenceContinuationOtherRequirementService).saveLicenceContinuationOtherRequirementForm(eq(licenceContinuationOtherRequirementForm), any());
  }

  @Test
  void submitForm_Invalid() throws Exception {
    var licenceContinuationOtherRequirementForm = new LicenceContinuationOtherRequirementForm();

    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    when(licenceContinuationService.getScheduleDetailFromApplicationDetail(any()))
        .thenReturn(LICENCE_SCHEDULE_DETAIL);
    when(otherRequirementsVisibilityResolverService.resolveVisibility(LICENCE_CONTINUATION_APPLICATION_DETAIL))
        .thenReturn(VISIBILITY_WITH_REQUIREMENTS);
    when(fileControllerHelperService.fileUploadComponentAttributes(
        any(List.class), any(Class.class), any(Function.class), any(Function.class)))
        .thenReturn(FileUploadTestUtil.FILE_UPLOAD_COMPONENT_ATTRIBUTES);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationOtherRequirementController.class).submitForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, licenceContinuationOtherRequirementForm, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", licenceContinuationOtherRequirementForm)
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/licenceContinuationOtherRequirement"))
        .andExpect(model().attribute("otherRequirementsVisibility", VISIBILITY_WITH_REQUIREMENTS))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)), "Work area",
            ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null)), "Task list"
        )))
        .andExpect(model().attribute("currentPage", PAGE_TITLE));
    verifyNoInteractions(licenceContinuationOtherRequirementService);
  }

  @Test
  void renderForm_ForbiddenUserNoAccess() throws Exception {
    when(licenceContinuationService.getDetailByIdOrThrow(any()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any()))
        .thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationOtherRequirementController.class).renderForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());

    verifyNoInteractions(licenceContinuationOtherRequirementService);
  }

  @Test
  void submitForm_ForbiddenUserNoAccess() throws Exception {
    when(licenceContinuationService.getDetailByIdOrThrow(any()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any()))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationOtherRequirementController.class).submitForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());
  }
}