package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListSectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceWorkProgrammeAmendmentSummaryController.class)
class LicenceWorkProgrammeAmendmentSummaryControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  @MockitoBean
  private LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService;

  @MockitoBean
  private LicenceWorkProgrammeAmendmentSummaryFormValidator licenceWorkProgrammeAmendmentSummaryFormValidator;

  @MockitoBean
  private ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService;

  @MockitoBean
  private ScheduleWorkProgrammeApplicationTaskListSectionService scheduleWorkProgrammeApplicationTaskListSectionService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  private static final UUID SCHEDULE_APPLICATION_DETAIL_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(1);
    scheduleWorkProgrammeApplicationDetail.setId(SCHEDULE_APPLICATION_DETAIL_ID);
    scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(true);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_APPLICATION_DETAIL_ID)).thenReturn(
        scheduleWorkProgrammeApplicationDetail);
  }

  @SecurityTest
  void renderForm_withExistingAmendments() throws Exception {
    LicenceWorkProgrammeAmendmentRequest amendmentRequest = new LicenceWorkProgrammeAmendmentRequest();
    LicenceWorkProgrammeAmendmentSummaryForm form = new LicenceWorkProgrammeAmendmentSummaryForm();

    List<LicenceWorkProgrammeAmendmentSummaryView> amendmentViews = List.of(
        new LicenceWorkProgrammeAmendmentSummaryView("duration",
            "additionalInfo",
            "label",
            "extensionRequired",
            "information",
            LicenceWorkProgrammeAmendmentSummaryMode.VIEW,
            "changeUrl",
            "deleteUrl",false,
            false));

    when(licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail)).thenReturn(List.of(amendmentRequest));
    when(licenceWorkProgrammeAmendmentSummaryService.getWorkProgrammeAmendmentByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail)).thenReturn(form);
    when(licenceWorkProgrammeAmendmentSummaryService.getWorkProgrammeAmendmentSummaryViewsFromScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail)).thenReturn(amendmentViews);

    mockMvc.perform(
            get(ReverseRouter.route(
                on(LicenceWorkProgrammeAmendmentSummaryController.class).renderForm(
                    SCHEDULE_APPLICATION_DETAIL_ID, scheduleWorkProgrammeApplicationDetail)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendmentSummary"))
        .andExpect(model().attribute("pageTitle", "Work programme amendments"))
        .andExpect(model().attribute("form", is(form)))
        .andExpect(model().attributeExists("licenceWorkProgrammeAmendmentSummaryOptions"))
        .andExpect(model().attribute("licenceWorkProgrammeAmendments", is(amendmentViews)))
        .andExpect(model().attribute("cancelUrl", (ReverseRouter.route  (on(
            ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null)))));

    verify(licenceWorkProgrammeAmendmentRepository).findAllByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
    verify(licenceWorkProgrammeAmendmentSummaryService).getWorkProgrammeAmendmentByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail);
  }

  @SecurityTest
  void renderFormWithNoAmendmentsRedirectsToSelectPage() throws Exception {
    when(licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail)).thenReturn(List.of());

    mockMvc.perform(
            get(ReverseRouter.route(
                on(LicenceWorkProgrammeAmendmentSummaryController.class).renderForm(
                    SCHEDULE_APPLICATION_DETAIL_ID, scheduleWorkProgrammeApplicationDetail)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceWorkProgrammeAmendmentRepository).findAllByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
    verify(licenceWorkProgrammeAmendmentSummaryService, never()).getWorkProgrammeAmendmentByScheduleWorkProgrammeApplicationDetail(any());
  }

  @Test
  void submitValidForm_withOptionSelectedYesNow() throws Exception {
    when(licenceWorkProgrammeAmendmentSummaryFormValidator.isValid(any())).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceWorkProgrammeAmendmentSummaryController.class).submitForm(
            SCHEDULE_APPLICATION_DETAIL_ID,
               scheduleWorkProgrammeApplicationDetail,
               null,
               null
           )))
            .param(
                "licenceWorkProgrammeAmendmentSummaryOptions",
                LicenceWorkProgrammeAmendmentSummaryOptions.YES_NOW.getEnumName()
            )
            .with(user(organisationUser))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(SelectLicenceWorkAmendmentController.class)
            .renderForm(scheduleWorkProgrammeApplicationDetail.getId(), scheduleWorkProgrammeApplicationDetail))));
    verify(licenceWorkProgrammeAmendmentSummaryService).saveWorkProgrammeAmendmentSummaryForm(any(), any());
  }

  @Test
  void submitValidForm_withOptionSelectedNo() throws Exception {
    when(licenceWorkProgrammeAmendmentSummaryFormValidator.isValid(any())).thenReturn(true);

    mockMvc.perform(
               post(ReverseRouter.route(
                   on(LicenceWorkProgrammeAmendmentSummaryController.class).submitForm(
                       SCHEDULE_APPLICATION_DETAIL_ID, scheduleWorkProgrammeApplicationDetail, null, null)))
                   .param(
                       "licenceWorkProgrammeAmendmentSummaryOptions",
                       LicenceWorkProgrammeAmendmentSummaryOptions.NO.getEnumName()
                   )
                   .with(user(organisationUser))
                   .with(csrf()))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
               .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null))));
    verify(licenceWorkProgrammeAmendmentSummaryService).saveWorkProgrammeAmendmentSummaryForm(any(), any());
  }

  @Test
  void submitValidForm_withOptionSelectedYesLater() throws Exception {
    when(licenceWorkProgrammeAmendmentSummaryFormValidator.isValid(any())).thenReturn(true);

    mockMvc.perform(
               post(ReverseRouter.route(
                   on(LicenceWorkProgrammeAmendmentSummaryController.class).submitForm(
                       SCHEDULE_APPLICATION_DETAIL_ID, scheduleWorkProgrammeApplicationDetail, null, null)))
                   .param(
                       "licenceWorkProgrammeAmendmentSummaryOptions",
                       LicenceWorkProgrammeAmendmentSummaryOptions.YES_LATER.getEnumName()
                   )
                   .with(user(organisationUser))
                   .with(csrf()))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
               .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null))));
    verify(licenceWorkProgrammeAmendmentSummaryService).saveWorkProgrammeAmendmentSummaryForm(any(), any());
  }

  @Test
  void submitInvalidForm() throws Exception {
    LicenceWorkProgrammeAmendmentSummaryForm form = new LicenceWorkProgrammeAmendmentSummaryForm();

    List<LicenceWorkProgrammeAmendmentSummaryView> amendmentViews = List.of(
        new LicenceWorkProgrammeAmendmentSummaryView(
            "duration",
            "additionalInfo",
            "label",
            "extensionRequired",
            "information",
            LicenceWorkProgrammeAmendmentSummaryMode.VIEW,
            "changeUrl",
            "deleteUrl",false,
            false));

    when(licenceWorkProgrammeAmendmentSummaryFormValidator.isValid(any())).thenReturn(false);
    when(licenceWorkProgrammeAmendmentSummaryService.getWorkProgrammeAmendmentSummaryViewsFromScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail)).thenReturn(amendmentViews);

    mockMvc.perform(
            post(ReverseRouter.route(
                on(LicenceWorkProgrammeAmendmentSummaryController.class).submitForm(
                    SCHEDULE_APPLICATION_DETAIL_ID, scheduleWorkProgrammeApplicationDetail, form, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendmentSummary"))
        .andExpect(model().attribute("pageTitle", "Work programme amendments"))
        .andExpect(model().attributeExists("licenceWorkProgrammeAmendmentSummaryOptions"))
        .andExpect(model().attribute("licenceWorkProgrammeAmendments", is(amendmentViews)))
        .andExpect(model().attribute("cancelUrl", (ReverseRouter.route  (on(
            ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null)))));

    verify(licenceWorkProgrammeAmendmentSummaryFormValidator).isValid(any());
  }

  @SecurityTest
  void renderFormWithMultipleAmendments() throws Exception {
    LicenceWorkProgrammeAmendmentRequest amendmentRequest1 = new LicenceWorkProgrammeAmendmentRequest();
    LicenceWorkProgrammeAmendmentRequest amendmentRequest2 = new LicenceWorkProgrammeAmendmentRequest();
    LicenceWorkProgrammeAmendmentSummaryForm form = new LicenceWorkProgrammeAmendmentSummaryForm();

    List<LicenceWorkProgrammeAmendmentSummaryView> amendmentViews = List.of(
        new LicenceWorkProgrammeAmendmentSummaryView(
            "duration1",
            "additionalInfo1",
            "label1",
            "extensionRequired1",
            "information1",
            LicenceWorkProgrammeAmendmentSummaryMode.VIEW,
            "changeUrl1",
            "deleteUrl1",false,
            false),
        new LicenceWorkProgrammeAmendmentSummaryView(
            "duration2",
            "additionalInfo2",
            "label2",
            "extensionRequired2",
            "information2",
            LicenceWorkProgrammeAmendmentSummaryMode.VIEW,
            "changeUrl2",
            "deleteUrl2",false,
            false));

    when(licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail)).thenReturn(List.of(amendmentRequest1, amendmentRequest2));
    when(licenceWorkProgrammeAmendmentSummaryService.getWorkProgrammeAmendmentByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail)).thenReturn(form);
    when(licenceWorkProgrammeAmendmentSummaryService.getWorkProgrammeAmendmentSummaryViewsFromScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail)).thenReturn(amendmentViews);

    mockMvc.perform(
            get(ReverseRouter.route(
                on(LicenceWorkProgrammeAmendmentSummaryController.class).renderForm(
                    SCHEDULE_APPLICATION_DETAIL_ID, scheduleWorkProgrammeApplicationDetail)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeAmendmentSummary"))
        .andExpect(model().attribute("form", is(form)))
        .andExpect(model().attribute("licenceWorkProgrammeAmendments", hasSize(2)))
        .andExpect(model().attribute("licenceWorkProgrammeAmendments", is(amendmentViews)));

    verify(licenceWorkProgrammeAmendmentRepository).findAllByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
  }
}