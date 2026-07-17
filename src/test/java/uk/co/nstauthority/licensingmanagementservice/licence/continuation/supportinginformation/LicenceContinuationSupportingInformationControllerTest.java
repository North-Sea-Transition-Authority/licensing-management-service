package uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation;

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
import static uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformationController.PAGE_TITLE;

import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.Errors;
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
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicenceContinuationSupportingInformationController.class)
class LicenceContinuationSupportingInformationControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceContinuationSupportingInformationService licenceContinuationSupportingInformationService;

  @MockitoBean
  private LicenceContinuationSupportingInformationValidator licenceContinuationSupportingInformationValidator;

  @MockitoBean
  private FileControllerHelperService fileControllerHelperService;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL
      = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(LICENCE));
  private static final LicenceContinuationApplicationDetail LICENCE_CONTINUATION_APPLICATION_DETAIL
      = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(LICENCE_SCHEDULE_DETAIL);

  private ServiceUserDetail organisationUser;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(2L)
        .build();
  }

  @Test
  void renderForm() throws Exception {
    var form = new LicenceContinuationSupportingInformationForm();

    when(licenceContinuationSupportingInformationService.getSupportingInformationForm(LICENCE_CONTINUATION_APPLICATION_DETAIL))
        .thenReturn(form);
    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);
    when(fileControllerHelperService.fileUploadComponentAttributes(
        eq(form.getDocuments()), any(Class.class), any(Function.class), any(Function.class)))
        .thenReturn(FileUploadTestUtil.FILE_UPLOAD_COMPONENT_ATTRIBUTES);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationSupportingInformationController.class)
                .renderForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/licenceContinuationAdditionalSupportingInformation"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("form", form))
        .andExpect(model().attribute("fileUploadAttributes", FileUploadTestUtil.FILE_UPLOAD_COMPONENT_ATTRIBUTES))
        .andExpect(model().attribute("cancelUrl",
            ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
                .getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null))));
  }

  @Test
  void submitForm_whenValid_savesAndRedirectsToTaskList() throws Exception {
    var form = new LicenceContinuationSupportingInformationForm();

    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);
    when(licenceContinuationSupportingInformationValidator.isValid(eq(form), any(Errors.class))).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationSupportingInformationController.class)
                .submitForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, form, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form)
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
            .getTaskList(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null))));

    verify(licenceContinuationSupportingInformationService)
        .saveSupportingInformationForm(form, LICENCE_CONTINUATION_APPLICATION_DETAIL);
  }

  @Test
  void submitForm_whenInvalid_reRendersFormAndDoesNotSave() throws Exception {
    var form = new LicenceContinuationSupportingInformationForm();

    when(licenceContinuationService.getDetailByIdOrThrow(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);
    when(licenceContinuationSupportingInformationValidator.isValid(eq(form), any(Errors.class))).thenReturn(false);
    when(fileControllerHelperService.fileUploadComponentAttributes(
        eq(form.getDocuments()), any(Class.class), any(Function.class), any(Function.class)))
        .thenReturn(FileUploadTestUtil.FILE_UPLOAD_COMPONENT_ATTRIBUTES);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationSupportingInformationController.class)
                .submitForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, form, null)))
                .with(user(organisationUser))
                .with(csrf())
                .flashAttr("form", form)
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/continuation/licenceContinuationAdditionalSupportingInformation"));

    verifyNoInteractions(licenceContinuationSupportingInformationService);
  }

  @Test
  void renderForm_whenUserHasNoAccess_isForbidden() throws Exception {
    when(licenceContinuationService.getDetailByIdOrThrow(any()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceContinuationSupportingInformationController.class)
                .renderForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isForbidden());

    verifyNoInteractions(licenceContinuationSupportingInformationService);
  }

  @Test
  void submitForm_whenUserHasNoAccess_isForbidden() throws Exception {
    when(licenceContinuationService.getDetailByIdOrThrow(any()))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceContinuationSupportingInformationController.class)
                .submitForm(LICENCE_CONTINUATION_APPLICATION_DETAIL.getId(), null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());
  }
}
