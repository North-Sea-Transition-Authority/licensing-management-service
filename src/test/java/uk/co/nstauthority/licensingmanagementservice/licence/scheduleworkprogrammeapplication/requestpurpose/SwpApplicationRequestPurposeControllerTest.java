package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import static uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController.WORK_AREA_PAGE_NAME;

import java.util.Map;
import java.util.Set;
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
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@ContextConfiguration(classes = SwpApplicationRequestPurposeController.class)
class SwpApplicationRequestPurposeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private SwpApplicationRequestPurposeValidator swpApplicationRequestPurposeValidator;

  @MockitoBean
  private SwpApplicationRequestPurposeService swpApplicationRequestPurposeService;

  private static final Long ORGANISATION_USER_WUA_ID = 2L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(ORGANISATION_USER_WUA_ID)
      .build();

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;
  private static final UUID SWP_APPLICATION_DETAIL_ID = UUID.randomUUID();

  private static final Set<SwpApplicationRequestPurposeOption> PURPOSE_OPTIONS = Set.of(
      SwpApplicationRequestPurposeOption.AMEND_THE_WORK_PROGRAMME,
      SwpApplicationRequestPurposeOption.EXTEND_A_TERM
  );

  @BeforeEach
  void setUp() {
    var scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setId(UUID.randomUUID());
    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(new Licence());
    scheduleWorkProgrammeApplication.setLicenceSchedule(licenceSchedule);
    scheduleWorkProgrammeApplicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(SWP_APPLICATION_DETAIL_ID)
        .withStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT)
        .withScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication)
        .build();
    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SWP_APPLICATION_DETAIL_ID)).thenReturn(
        scheduleWorkProgrammeApplicationDetail);
  }

  @Test
  void renderForm() throws Exception {
    when(swpApplicationRequestPurposeService.getFilledSwpApplicationRequestPurposeForm(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(new SwpApplicationRequestPurposeForm());
    when(swpApplicationRequestPurposeService.getPageOptions(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(PURPOSE_OPTIONS);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    var resultActions = mockMvc.perform(
            get(ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).renderForm(SWP_APPLICATION_DETAIL_ID, null)))
                .with(user(USER))
        )
        .andExpect(status().isOk());

    expectStandardModelExists(resultActions);
  }

  @Test
  void submitForm_validForm() throws Exception {
    when(swpApplicationRequestPurposeValidator.isValid(any(), any())).thenReturn(true);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).submitForm(SWP_APPLICATION_DETAIL_ID, null, null, null)))
                .with(user(USER))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(swpApplicationRequestPurposeService).saveOrUpdateRequestPurpose(eq(scheduleWorkProgrammeApplicationDetail), any());
  }

  @Test
  void submitForm_invalidForm() throws Exception {
    when(swpApplicationRequestPurposeService.getFilledSwpApplicationRequestPurposeForm(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(new SwpApplicationRequestPurposeForm());
    when(swpApplicationRequestPurposeService.getPageOptions(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(PURPOSE_OPTIONS);

    when(swpApplicationRequestPurposeValidator.isValid(any(), any())).thenReturn(false);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    var resultActions = mockMvc.perform(
        post(ReverseRouter.route(
            on(SwpApplicationRequestPurposeController.class).submitForm(SWP_APPLICATION_DETAIL_ID, scheduleWorkProgrammeApplicationDetail, null, null)))
            .with(user(USER))
            .with(csrf())
    )
    .andExpect(status().isOk());

    expectStandardModelExists(resultActions);
  }

  @ParameterizedTest
  @EnumSource(value = ScheduleWorkProgrammeApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void renderPage_assertForbiddenOnNotDraft(ScheduleWorkProgrammeApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(id)
        .withStatus(status)
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);

    mockMvc.perform(get(ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).renderForm(
        id, null))).with(user(USER))).andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = ScheduleWorkProgrammeApplicationStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void submitPage_assertForbiddenOnNotDraft(ScheduleWorkProgrammeApplicationStatus status) throws Exception {
    var id = UUID.randomUUID();
    var submittedDetail = ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(id)
        .withStatus(status)
        .build();

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(id)).thenReturn(submittedDetail);

    mockMvc.perform(post(ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).submitForm(
            id, null, null, null)))
            .with(user(USER))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(get(ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).renderForm(
            SWP_APPLICATION_DETAIL_ID, null)))
               .with(user(USER)))
           .andExpect(status().isForbidden());
  }

  @Test
  void submitPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(SwpApplicationRequestPurposeController.class).submitForm(
            SWP_APPLICATION_DETAIL_ID, null,null, null)))
               .with(user(USER))
               .with(csrf()))
           .andExpect(status().isForbidden());
  }

  private void expectStandardModelExists(ResultActions resultActions) throws Exception {
    var pageOptionsMap = PURPOSE_OPTIONS.stream()
        .collect(StreamUtil.toLinkedHashMap(Enum::name, SwpApplicationRequestPurposeOption::getDisplayName));

    resultActions
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/requestPurpose"))
        .andExpect(model().attribute("pageTitle", SwpApplicationRequestPurposeController.PAGE_TITLE))
        .andExpect(model().attribute("pageOptionsMap", pageOptionsMap))
        .andExpect(model().attribute("cancelUrl", getTaskListUrl()))
        .andExpect(model().attribute("breadcrumbs", Map.of(
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)),
            WORK_AREA_PAGE_NAME,
            getTaskListUrl(),
            ScheduleWorkProgrammeApplicationTaskListController.PAGE_TITLE
        )));
  }

  private String getTaskListUrl() {
    return ReverseRouter.route(
        on(ScheduleWorkProgrammeApplicationTaskListController.class).getTaskList(SWP_APPLICATION_DETAIL_ID, null, null));
  }
}