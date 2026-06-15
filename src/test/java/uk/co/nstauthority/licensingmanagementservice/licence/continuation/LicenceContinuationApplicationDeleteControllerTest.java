package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import static org.mockito.Mockito.any;
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

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit.ContinuationSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = LicenceContinuationApplicationDeleteController.class)
class LicenceContinuationApplicationDeleteControllerTest extends AbstractControllerTest {

  @MockitoBean
  private ContinuationSummarySectionService continuationSummarySectionService;

  @MockitoBean
  private LicenceContinuationApplicationTaskListController taskListController;

  @MockitoBean
  private WorkAreaController workAreaController;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;
  private static final UUID CONTINUATION_APPLICATION_DETAIL_ID = UUID.randomUUID();
  private static final Licence LICENCE = LicenceTestUtil.builder().build();

  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
                                                .withWuaId(ORGANISATION_USER_WUA_ID)
                                                .build();

    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(LICENCE)
    );

    licenceContinuationApplicationDetail = LicenceContinuationApplicationTestUtil
        .builder()
        .withId(CONTINUATION_APPLICATION_DETAIL_ID)
        .withStatus(LicenceContinuationApplicationStatus.DRAFT)
        .withLicenceContinuationApplication(
            LicenceContinuationApplicationTestUtil.createLicenceContinuationApplication(licenceScheduleDetail))
        .build();

    when(licenceContinuationService.getDetailByIdOrThrow(CONTINUATION_APPLICATION_DETAIL_ID))
        .thenReturn(licenceContinuationApplicationDetail);
  }

  @Test
  void renderForm() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any()))
        .thenReturn(true);

    mockMvc.perform(
               get(ReverseRouter.route(on(LicenceContinuationApplicationDeleteController.class)
                   .renderForm(CONTINUATION_APPLICATION_DETAIL_ID, null, null)))
                   .with(user(organisationUser))
           )
           .andExpect(status().isOk())
           .andExpect(view().name("lms/licence/continuation/licenceContinuationApplicationDeleteConfirmation"))
           .andExpect(model().attribute("backToTaskListUrl", ReverseRouter.route(on(
               LicenceContinuationApplicationTaskListController.class)
               .getTaskList(CONTINUATION_APPLICATION_DETAIL_ID, null, null))))
           .andExpect(model().attribute("actionUrl", ReverseRouter.route(on(
               LicenceContinuationApplicationDeleteController.class)
               .deleteLicenceContinuationApplication(CONTINUATION_APPLICATION_DETAIL_ID, null, null))))
           .andExpect(model().attributeExists("summarySections"))
           .andExpect(model().attribute("accordionId", CONTINUATION_APPLICATION_DETAIL_ID));

    verify(continuationSummarySectionService).getSummarySections(licenceContinuationApplicationDetail, organisationUser);
  }

  @Test
  void deleteLicenceContinuationApplication() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any()))
        .thenReturn(true);

    String expectedRedirectUrl = ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null));

    mockMvc.perform(
        post(ReverseRouter.route(on(LicenceContinuationApplicationDeleteController.class)
            .deleteLicenceContinuationApplication(CONTINUATION_APPLICATION_DETAIL_ID, null, null)))
                        .with(user(organisationUser))
                        .with(csrf())
           )
           .andExpect(status().is3xxRedirection())
           .andExpect(view().name("redirect:" + expectedRedirectUrl));

    verify(licenceContinuationService).deleteLicenceContinuationApplication(licenceContinuationApplicationDetail);
  }

  @ParameterizedTest
  @EnumSource(value = LicenceContinuationApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void renderForm_assertForbiddenOnNotDraft(LicenceContinuationApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var nonDraftDetail = LicenceContinuationApplicationTestUtil
        .builder()
        .withId(id)
        .withStatus(status)
        .build();

    when(licenceContinuationService.getDetailByIdOrThrow(id)).thenReturn(nonDraftDetail);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceContinuationApplicationDeleteController.class)
            .renderForm(id, null, null)))
                        .with(user(organisationUser))
           )
           .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = LicenceContinuationApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void deleteLicenceContinuationApplication_assertForbiddenOnNotDraft(
      LicenceContinuationApplicationStatus status
  ) throws Exception {
    var id = UUID.randomUUID();
    var nonDraftDetail = LicenceContinuationApplicationTestUtil
        .builder()
        .withId(id)
        .withStatus(status)
        .build();

    when(licenceContinuationService.getDetailByIdOrThrow(id)).thenReturn(nonDraftDetail);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceContinuationApplicationDeleteController.class)
            .deleteLicenceContinuationApplication(id, null, null)))
                        .with(user(organisationUser))
                        .with(csrf())
           )
           .andExpect(status().isForbidden());
  }

  @Test
  void renderForm_assertForbiddenUserNoAccess() throws Exception {
    var id = UUID.randomUUID();

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(false);
    when(licenceContinuationService.getDetailByIdOrThrow(id)).thenReturn(licenceContinuationApplicationDetail);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceContinuationApplicationDeleteController.class)
            .renderForm(id, null, null)))
                     .with(user(organisationUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteLicenceContinuationApplication_assertForbiddenUserNoAccess() throws Exception {
    var id = UUID.randomUUID();

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(false);
    when(licenceContinuationService.getDetailByIdOrThrow(id)).thenReturn(licenceContinuationApplicationDetail);

    mockMvc.perform(post(ReverseRouter.route(
            on(LicenceContinuationApplicationDeleteController.class)
                .deleteLicenceContinuationApplication(id, null, null)))
                     .with(user(organisationUser))
                     .with(csrf()))
        .andExpect(status().isForbidden());
  }
}
