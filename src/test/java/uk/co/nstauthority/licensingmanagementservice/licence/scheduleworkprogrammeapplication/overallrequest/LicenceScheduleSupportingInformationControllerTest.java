package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadComponentAttributes;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileUsage;
import uk.co.nstauthority.licensingmanagementservice.file.FileControllerHelperService;
import uk.co.nstauthority.licensingmanagementservice.file.FileUploadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = LicenceScheduleSupportingInformationController.class)
class LicenceScheduleSupportingInformationControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService;

  @MockitoBean
  private LicenceScheduleSupportingInformationFormValidator licenceScheduleSupportingInformationFormValidator;

  @MockitoBean
  private LicenceScheduleExtensionService licenceScheduleExtensionService;

  @MockitoBean
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  @MockitoBean
  private FileControllerHelperService fileControllerHelperService;

  @Captor
  private ArgumentCaptor<Supplier<ApplicationFileUsage>> fileUsageSupplierCaptor;

  @MockitoBean
  private LicenceScheduleSupportingInformationHelperService licenceScheduleSupportingInformationHelperService;


  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  private static final UUID SCHEDULE_APPLICATION_DETAIL_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var licence = new Licence();
    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);
    var licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);
    var scheduleWorkProgrammeApplication = ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplication(licenceScheduleDetail);

    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(1);
    scheduleWorkProgrammeApplicationDetail.setStatus(ApplicationStatus.DRAFT);
    scheduleWorkProgrammeApplicationDetail.setId(SCHEDULE_APPLICATION_DETAIL_ID);
    scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(true);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_APPLICATION_DETAIL_ID)).thenReturn(
        scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void renderOverallRequestForm() throws Exception {
    when(licenceScheduleExtensionService.isExtensionRequested(scheduleWorkProgrammeApplicationDetail)).thenReturn(true);

    when(licenceScheduleSupportingInformationService.getLicenceScheduleRequestForm(any())).thenReturn(
        new LicenceScheduleSupportingInformationForm());

    FileUploadComponentAttributes fileUploadComponentAttributes = FileUploadTestUtil.FILE_UPLOAD_COMPONENT_ATTRIBUTES;

    when(fileControllerHelperService.fileUploadComponentAttributes(any(List.class), any(Class.class), any(Function.class), any(Function.class))).thenReturn(fileUploadComponentAttributes);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(
                on(LicenceScheduleSupportingInformationController.class).renderForm(SCHEDULE_APPLICATION_DETAIL_ID,
                                                                                    scheduleWorkProgrammeApplicationDetail)))
                .with(user(organisationUser)
                )
        )
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleLicenceSupportingInformationRequest"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("pageTitle", "Supporting information"))
        .andExpect(model().attribute("isExtension", false))
        .andExpect(model().attribute("isCarbonStorageLicence", false))
        .andExpect(model().attribute("fileUploadAttributes", fileUploadComponentAttributes))
        .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(
            ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)), "Work area",
            ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)), "Task list"
        )))
        .andExpect(model().attribute("currentPage", LicenceScheduleSupportingInformationController.PAGE_TITLE));

  }

  @Test
  void renderOverallRequestForm_whenCarbonStorageLicence_assertIsCarbonStorageLicenceTrue() throws Exception {
    when(licenceScheduleSupportingInformationService.getLicenceScheduleRequestForm(any())).thenReturn(
        new LicenceScheduleSupportingInformationForm());
    when(fileControllerHelperService.fileUploadComponentAttributes(any(List.class), any(Class.class), any(Function.class), any(Function.class)))
        .thenReturn(FileUploadTestUtil.FILE_UPLOAD_COMPONENT_ATTRIBUTES);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);
    when(licenceService.isCarbonStorageLicence(any(Licence.class))).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class)
                .renderForm(SCHEDULE_APPLICATION_DETAIL_ID, scheduleWorkProgrammeApplicationDetail)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("isCarbonStorageLicence", true));
  }

  @Test
  void renderOverallRequestForm_whenNonCarbonStorageLicence_assertIsCarbonStorageLicenceFalse() throws Exception {
    when(licenceScheduleSupportingInformationService.getLicenceScheduleRequestForm(any())).thenReturn(
        new LicenceScheduleSupportingInformationForm());
    when(fileControllerHelperService.fileUploadComponentAttributes(any(List.class), any(Class.class), any(Function.class), any(Function.class)))
        .thenReturn(FileUploadTestUtil.FILE_UPLOAD_COMPONENT_ATTRIBUTES);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);
    when(licenceService.isCarbonStorageLicence(any(Licence.class))).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class)
                .renderForm(SCHEDULE_APPLICATION_DETAIL_ID, scheduleWorkProgrammeApplicationDetail)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("isCarbonStorageLicence", false));
  }

  @Test
  void submitValidForm() throws Exception {
    when(licenceScheduleSupportingInformationFormValidator.isValid(any(), any())).thenReturn(true);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(
                on(LicenceScheduleSupportingInformationController.class).submitForm(SCHEDULE_APPLICATION_DETAIL_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleSupportingInformationService).saveRequestForm(any(), eq(scheduleWorkProgrammeApplicationDetail));
  }

  @Test
  void submitInvalidForm() throws Exception {

    FileUploadComponentAttributes fileUploadComponentAttributes = FileUploadTestUtil.FILE_UPLOAD_COMPONENT_ATTRIBUTES;
    when(fileControllerHelperService.fileUploadComponentAttributes(any(List.class), any(Class.class), any(Function.class), any(Function.class))).thenReturn(fileUploadComponentAttributes);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);


    mockMvc.perform(
            post(ReverseRouter.route(
                on(LicenceScheduleSupportingInformationController.class).submitForm(SCHEDULE_APPLICATION_DETAIL_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
           .andExpect(status().isOk())
           .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleLicenceSupportingInformationRequest"))
           .andExpect(model().attribute("pageTitle", "Supporting information"))
           .andExpect(model().attribute("isExtension", false))
           .andExpect(model().attribute("isCarbonStorageLicence", false))
           .andExpect(model().attribute("fileUploadAttributes", fileUploadComponentAttributes))
           .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(
                   ScheduleWorkProgrammeApplicationTaskListController.class)
                   .getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))))
           .andExpect(model().attribute("breadcrumbs", Map.of(
               ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)), "Work area",
               ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)), "Task list"
           )))
           .andExpect(model().attribute("currentPage", LicenceScheduleSupportingInformationController.PAGE_TITLE));

    verify(licenceScheduleSupportingInformationService, never()).saveRequestForm(any(), any());

  }

  @Test
  void download() throws Exception {
    when(fileControllerHelperService.download(any(UUID.class), any(Supplier.class), any(ServiceUserDetail.class))).thenReturn(ResponseEntity.ok().build());
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).downloadFile(
        UUID.randomUUID(), scheduleWorkProgrammeApplicationDetail.getId(), null, null)))
            .with(user(organisationUser)).with(csrf())).andExpect(status().isOk());

    verify(fileControllerHelperService).download(any(), fileUsageSupplierCaptor.capture(), any());
    assertThat(fileUsageSupplierCaptor.getValue().get()).isEqualTo(
        LicenceScheduleSupportingInformationFileUsages.fromApplication(scheduleWorkProgrammeApplicationDetail));
  }

  @Test
  void delete() throws Exception {
    when(fileControllerHelperService.delete(any(UUID.class), any(Supplier.class), any(ServiceUserDetail.class))).thenReturn(ResponseEntity.ok().build());
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).deleteFile(
        UUID.randomUUID(), scheduleWorkProgrammeApplicationDetail.getId(), null, null)))
        .with(user(organisationUser)).with(csrf())).andExpect(status().isOk());

    verify(fileControllerHelperService).delete(any(), fileUsageSupplierCaptor.capture(), any());
    assertThat(fileUsageSupplierCaptor.getValue().get()).isEqualTo(
        LicenceScheduleSupportingInformationFileUsages.fromApplication(scheduleWorkProgrammeApplicationDetail));
  }

  @ParameterizedTest
  @EnumSource(value = ApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void renderPage_assertForbiddenOnNotDraft(ApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(id)
        .withStatus(status)
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).renderForm(
        id, null))).with(user(organisationUser))).andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = ApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void submitPage_assertForbiddenOnNotDraft(ApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(id)
        .withStatus(status)
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).submitForm(
            id, null, null, null)))
            .with(user(organisationUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = ApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void deleteFile_assertForbiddenOnNotDraft(ApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(id)
        .withStatus(status)
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).deleteFile(
            UUID.randomUUID(), id, null, null)))
            .with(user(organisationUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).renderForm(
               SCHEDULE_APPLICATION_DETAIL_ID, null)))
               .with(user(organisationUser)))
           .andExpect(status().isForbidden());
  }

  @Test
  void submitPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).submitForm(
               SCHEDULE_APPLICATION_DETAIL_ID, null, null, null)))
               .with(user(organisationUser))
               .with(csrf()))
           .andExpect(status().isForbidden());
  }

  @Test
  void deleteFile_assertForbiddenUserNoAccess() throws Exception {
    var fileId = UUID.randomUUID();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_APPLICATION_DETAIL_ID)).thenReturn(scheduleWorkProgrammeApplicationDetail);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).deleteFile(
               fileId, SCHEDULE_APPLICATION_DETAIL_ID, null, null)))
               .with(user(organisationUser))
               .with(csrf()))
           .andExpect(status().isForbidden());
  }
}