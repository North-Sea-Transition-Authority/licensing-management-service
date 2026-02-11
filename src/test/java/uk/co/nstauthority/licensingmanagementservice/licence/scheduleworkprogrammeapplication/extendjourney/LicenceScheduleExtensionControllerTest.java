package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListSectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceScheduleExtensionController.class)
class LicenceScheduleExtensionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleExtensionService licenceScheduleExtensionService;

  @MockitoBean
  private LicenceScheduleExtensionFormValidator licenceScheduleExtensionFormValidator;

  @MockitoBean
  private ScheduleWorkProgrammeApplicationTaskListService scheduleWorkProgrammeApplicationTaskListService;

  @MockitoBean
  private ScheduleWorkProgrammeApplicationTaskListSectionService scheduleWorkProgrammeApplicationTaskListSectionService;

  @MockitoBean
  private LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  @MockitoBean
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;
  private LicenceScheduleTermAndPhases validTermAndPhases;

  private static final UUID SCHEDULE_APPLICATION_DETAIL_ID = UUID.randomUUID();
  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    validTermAndPhases = new LicenceScheduleTermAndPhases("1", "Term A", Collections.emptyList());
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var licence = new Licence();
    licence.setType(LicenceType.SEAWARD_PRODUCTION);

    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);

    licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);

    var scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setLicenceScheduleDetail(licenceScheduleDetail);
    scheduleWorkProgrammeApplication.setId(UUID.randomUUID());
    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(1);
    scheduleWorkProgrammeApplicationDetail.setStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT);
    scheduleWorkProgrammeApplicationDetail.setId(SCHEDULE_APPLICATION_DETAIL_ID);
    scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(true);

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(SCHEDULE_APPLICATION_DETAIL_ID)).thenReturn(
        scheduleWorkProgrammeApplicationDetail);
  }

  @SecurityTest
  void renderExtensionForm() throws Exception {
      LicenceScheduleTerm licenceScheduleTerm = new LicenceScheduleTerm();
      licenceScheduleTerm.setTermType(TermType.APPRAISAL);
      licenceScheduleTerm.setId(UUID.randomUUID());
    licenceScheduleTerm.setEndDate(LocalDate.of(1,1,1));

    when(licenceScheduleExtensionService.getCurrentTerm(any())).thenReturn(licenceScheduleTerm);
    when(licenceScheduleExtensionService.getlicenceScheduleExtensionForm(any())).thenReturn(new LicenceScheduleExtensionForm());
    when(licenceScheduleExtensionService.getExtendableTermAndPhases(any())).thenReturn(List.of(validTermAndPhases));
    when(licenceScheduleExtensionService.canExtendMoreThanOneOption(any())).thenReturn(false);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);

      mockMvc.perform(
              get(ReverseRouter.route(
                  on(LicenceScheduleExtensionController.class).renderForm(SCHEDULE_APPLICATION_DETAIL_ID,
                      scheduleWorkProgrammeApplicationDetail)))
                  .with(user(organisationUser)
                  )
          )
             .andExpect(status().isOk())
             .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleLicenceExtension"))
             .andExpect(model().attribute("pageTitle", "Extension Details"))
             .andExpect(model().attribute("validTermsAndPhases", List.of(validTermAndPhases)))
             .andExpect(model().attribute("canExtendMoreThanOneOption", false))
             .andExpect(model().attribute("currentTerm", licenceScheduleTerm))
             .andExpect(model().attribute("currentPhase", nullValue()))
             .andExpect(model().attribute("currentTermEndDate",
                 DateFormatUtil.convertToDisplayText(licenceScheduleTerm.getEndDate())))
          .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(
              ScheduleWorkProgrammeApplicationTaskListController.class)
              .getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))));
  }

  @SecurityTest
  void renderExtensionForm_withCurrentPhase() throws Exception {
    LicenceScheduleTerm licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setTermType(TermType.APPRAISAL);
    licenceScheduleTerm.setId(UUID.randomUUID());
    licenceScheduleTerm.setEndDate(LocalDate.of(1,1,1));

    LicenceSchedulePhase currentPhase = new LicenceSchedulePhase();
    LocalDate phaseEndDate = LocalDate.of(2026, 12, 31);
    currentPhase.setEndDate(phaseEndDate);

    String expectedPhaseEndDateDisplay = DateFormatUtil.convertToDisplayText(phaseEndDate);

    when(licenceScheduleExtensionService.getCurrentTerm(any())).thenReturn(licenceScheduleTerm);

    when(licenceScheduleExtensionService.getCurrentPhase(licenceScheduleDetail)).thenReturn(currentPhase);

    when(licenceScheduleExtensionService.getlicenceScheduleExtensionForm(any())).thenReturn(new LicenceScheduleExtensionForm());
    when(licenceScheduleExtensionService.getExtendableTermAndPhases(any())).thenReturn(List.of(validTermAndPhases));
    when(licenceScheduleExtensionService.canExtendMoreThanOneOption(any())).thenReturn(false);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);

    mockMvc.perform(
               get(ReverseRouter.route(
                   on(LicenceScheduleExtensionController.class).renderForm(SCHEDULE_APPLICATION_DETAIL_ID,
                                                                           scheduleWorkProgrammeApplicationDetail)))
                   .with(user(organisationUser)
                   )
           )
           .andExpect(status().isOk())
           .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleLicenceExtension"))
           .andExpect(model().attribute("pageTitle", "Extension Details"))
           .andExpect(model().attribute("currentTerm", licenceScheduleTerm))
           .andExpect(model().attribute("currentPhase", currentPhase))
           .andExpect(model().attribute("currentPhaseEndDate", expectedPhaseEndDateDisplay)) // 🚨 NEW ASSERTION
           .andExpect(model().attribute("currentTermEndDate",
            DateFormatUtil.convertToDisplayText(licenceScheduleTerm.getEndDate())))
           .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(
               ScheduleWorkProgrammeApplicationTaskListController.class)
                .getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))));
  }

  @Test
  void submitValidForm() throws Exception {
    when(licenceScheduleExtensionFormValidator.isValid(any(), any(), any())).thenReturn(true);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(
                on(LicenceScheduleExtensionController.class).submitForm(SCHEDULE_APPLICATION_DETAIL_ID, null, null,
                    null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleExtensionService).saveExtensionForm(any(), eq(scheduleWorkProgrammeApplicationDetail));
  }


  @Test
  void submitInvalidForm() throws Exception {
      LicenceScheduleTerm licenceScheduleTerm = new LicenceScheduleTerm();
      licenceScheduleTerm.setTermType(TermType.INITIAL);
      licenceScheduleTerm.setId(UUID.randomUUID());
      licenceScheduleTerm.setEndDate(LocalDate.of(1,1,1));

    when(licenceScheduleExtensionService.getCurrentTerm(any())).thenReturn(licenceScheduleTerm);
    when(licenceScheduleExtensionFormValidator.isValid(any(), any(), any())).thenReturn(false);
    when(licenceScheduleExtensionService.getExtendableTermAndPhases(any())).thenReturn(List.of(validTermAndPhases));
    when(licenceScheduleExtensionService.canExtendMoreThanOneOption(any())).thenReturn(false);
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);

      mockMvc.perform(
              post(ReverseRouter.route(
                  on(LicenceScheduleExtensionController.class).submitForm(SCHEDULE_APPLICATION_DETAIL_ID, null, null,
                      null)))
                  .with(user(organisationUser))
                  .with(csrf())
          )
          .andExpect(status().isOk())
             .andExpect(view().name("lms/licence/scheduleWorkProgrammeApplication/scheduleLicenceExtension"))
             .andExpect(model().attribute("pageTitle", "Extension Details"))
             .andExpect(model().attribute("validTermsAndPhases", List.of(validTermAndPhases)))
             .andExpect(model().attribute("canExtendMoreThanOneOption", false))
             .andExpect(model().attribute("currentTerm", licenceScheduleTerm))
             .andExpect(model().attribute("currentPhase", nullValue()))
             .andExpect(model().attribute("currentTermEndDate", DateFormatUtil.convertToDisplayText(licenceScheduleTerm.getEndDate())))
             .andExpect(model().attribute("cancelUrl", (ReverseRouter.route(on(
                     ScheduleWorkProgrammeApplicationTaskListController.class)
                                                       .getTaskList(SCHEDULE_APPLICATION_DETAIL_ID, null, null)))
             ));

    verify(licenceScheduleExtensionService, never()).saveExtensionForm(any(), any());

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

    mockMvc.perform(get(ReverseRouter.route(on(LicenceScheduleExtensionController.class).renderForm(
        id, null))).with(user(organisationUser))).andExpect(status().isForbidden());
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

    mockMvc.perform(post(ReverseRouter.route(on(LicenceScheduleExtensionController.class).submitForm(
            id, null, null, null)))
            .with(user(organisationUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(false);
    mockMvc.perform(get(ReverseRouter.route(on(LicenceScheduleExtensionController.class).renderForm(
            SCHEDULE_APPLICATION_DETAIL_ID, null)))
               .with(user(organisationUser)))
           .andExpect(status().isForbidden());
  }

  @Test
  void submitPage_assertForbiddenUserNoAccess() throws Exception {
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(false);
    mockMvc.perform(post(ReverseRouter.route(on(LicenceScheduleExtensionController.class).submitForm(
               SCHEDULE_APPLICATION_DETAIL_ID, null, null, null)))
               .with(user(organisationUser))
               .with(csrf()))
           .andExpect(status().isForbidden());
  }
  }