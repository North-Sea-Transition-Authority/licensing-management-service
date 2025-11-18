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
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.requestpurpose.SwpApplicationRequestPurpose;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

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

    var scheduleWorkProgrammeApplication = ScheduleWorkProgrammeApplicationTestUtil.createScheduleWorkProgrammeApplication(new LicenceScheduleDetail());

    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(1);
    scheduleWorkProgrammeApplicationDetail.setId(SCHEDULE_APPLICATION_DETAIL_ID);
    scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(true);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_APPLICATION_DETAIL_ID)).thenReturn(
        scheduleWorkProgrammeApplicationDetail);
  }

  @SecurityTest
  void renderOverallRequestForm() throws Exception {
    SwpApplicationRequestPurpose swpApplicationRequestPurpose = new SwpApplicationRequestPurpose();
    swpApplicationRequestPurpose.setExtendPhaseOrTerm(true);
    swpApplicationRequestPurpose.setExtendTerm(true);

    when(licenceScheduleExtensionService.isExtensionRequested(scheduleWorkProgrammeApplicationDetail)).thenReturn(true);

    when(licenceScheduleSupportingInformationService.getLicenceScheduleRequestForm(any())).thenReturn(
        new LicenceScheduleSupportingInformationForm());

    FileUploadComponentAttributes fileUploadComponentAttributes = FileUploadTestUtil.FILE_UPLOAD_COMPONENT_ATTRIBUTES;

    when(fileControllerHelperService.fileUploadComponentAttributes(any(List.class), any(Class.class), any(Function.class), any(Function.class))).thenReturn(fileUploadComponentAttributes);

    mockMvc.perform(
            get(ReverseRouter.route(
                on(LicenceScheduleSupportingInformationController.class).renderForm(SCHEDULE_APPLICATION_DETAIL_ID,
                                                                                    scheduleWorkProgrammeApplicationDetail)))
                .with(user(organisationUser)
                ).with(csrf())
        )
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleLicenceSupportingInformationRequest"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("pageTitle", "Supporting information"))
        .andExpect(model().attribute("isExtension", false))
        .andExpect(model().attribute("fileUploadAttributes", fileUploadComponentAttributes))
        .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(
            ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))));

  }

  @Test
  void submitValidForm() throws Exception {
    when(licenceScheduleSupportingInformationFormValidator.isValid(any(), any())).thenReturn(true);

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
           .andExpect(model().attribute("fileUploadAttributes", fileUploadComponentAttributes))
           .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(
                   ScheduleWorkProgrammeApplicationTaskListController.class)
                   .getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))));

    verify(licenceScheduleSupportingInformationService, never()).saveRequestForm(any(), any());

  }

  @Test
  void download() throws Exception {
    when(fileControllerHelperService.download(any(UUID.class), any(Supplier.class), any(ServiceUserDetail.class))).thenReturn(ResponseEntity.ok().build());

    mockMvc.perform(
               get(ReverseRouter.route(
                   on(LicenceScheduleSupportingInformationController.class).downloadFile(
                       scheduleWorkProgrammeApplicationDetail.getId(),
                       scheduleWorkProgrammeApplicationDetail, organisationUser,
                       scheduleWorkProgrammeApplicationDetail.getId())))
                   .with(user(organisationUser))
                   .with(csrf())
           )
           .andExpect(status().isOk());

    verify(fileControllerHelperService).download(any(), fileUsageSupplierCaptor.capture(), any());
    assertThat(fileUsageSupplierCaptor.getValue().get()).isEqualTo(
        LicenceScheduleSupportingInformationFileUsages.fromApplication(scheduleWorkProgrammeApplicationDetail));
  }

  @Test
  void delete() throws Exception {
    when(fileControllerHelperService.delete(any(UUID.class), any(Supplier.class), any(ServiceUserDetail.class))).thenReturn(ResponseEntity.ok().build());

    mockMvc.perform(
               post(ReverseRouter.route(
                   on(LicenceScheduleSupportingInformationController.class).deleteFile(
                                                UUID.randomUUID(),
                                                scheduleWorkProgrammeApplicationDetail,
                                                organisationUser,
                                                scheduleWorkProgrammeApplicationDetail.getId())))
                   .with(user(organisationUser))
                   .with(csrf()))
           .andExpect(status().isOk());

    verify(fileControllerHelperService).delete(any(), fileUsageSupplierCaptor.capture(), any());
    assertThat(fileUsageSupplierCaptor.getValue().get()).isEqualTo(
        LicenceScheduleSupportingInformationFileUsages.fromApplication(scheduleWorkProgrammeApplicationDetail));
  }


}