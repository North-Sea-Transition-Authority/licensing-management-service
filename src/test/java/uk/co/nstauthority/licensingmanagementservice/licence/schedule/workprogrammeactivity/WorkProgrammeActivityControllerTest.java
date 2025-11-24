package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

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

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ContextConfiguration(classes = WorkProgrammeActivityController.class)
class WorkProgrammeActivityControllerTest extends AbstractControllerTest {

  @MockitoBean
  private WorkProgrammeActivityFormService workProgrammeActivityFormService;

  @MockitoBean
  private WorkProgrammeActivityFormValidator workProgrammeActivityFormValidator;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private Licence licence;
  private static final String PAGE_CAPTION = "page caption";

  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);
  }

  @SecurityTest
  void renderAddNewActivityForm() throws Exception {
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(workProgrammeActivityFormService.getDateOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(workProgrammeActivityFormService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(workProgrammeActivityFormService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(WorkProgrammeActivityController.class)
                .renderAddNewActivityForm(licenceScheduleDetail.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createWorkProgrammeActivity"))
        .andExpect(model().attribute("categoryRadioOptions", WorkProgrammeActivityCategory.getCategoriesForLicenceType(licence.getType())))
        .andExpect(model().attribute("commitmentRadioOptions", DisplayableEnumOptionUtil.getDisplayableOptions(WorkProgrammeActivityCommitment.class)))
        .andExpect(model().attribute("activityDateRadioOptions", Map.of()))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));
  }

  @Test
  void submitAddNewActivityForm() throws Exception {
    when(workProgrammeActivityFormValidator.isValid(any(), any(), eq(licenceScheduleDetail))).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(WorkProgrammeActivityController.class)
                .submitAddNewActivityForm(licenceScheduleDetail.getId(), null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(workProgrammeActivityFormService).saveActivityFromForm(any(), eq(licenceScheduleDetail));
  }

  @Test
  void submitAddNewActivityForm_invalid() throws Exception {
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(workProgrammeActivityFormValidator.isValid(any(), any(), eq(licenceScheduleDetail))).thenReturn(false);
    when(workProgrammeActivityFormService.getDateOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(workProgrammeActivityFormService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(workProgrammeActivityFormService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());

    mockMvc.perform(
            post(ReverseRouter.route(on(WorkProgrammeActivityController.class)
                .submitAddNewActivityForm(licenceScheduleDetail.getId(), null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createWorkProgrammeActivity"))
        .andExpect(model().attribute("categoryRadioOptions", WorkProgrammeActivityCategory.getCategoriesForLicenceType(licence.getType())))
        .andExpect(model().attribute("commitmentRadioOptions", DisplayableEnumOptionUtil.getDisplayableOptions(WorkProgrammeActivityCommitment.class)))
        .andExpect(model().attribute("activityDateRadioOptions", Map.of()))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));

    verify(workProgrammeActivityFormService, never()).saveActivityFromForm(any(), any());
  }
}