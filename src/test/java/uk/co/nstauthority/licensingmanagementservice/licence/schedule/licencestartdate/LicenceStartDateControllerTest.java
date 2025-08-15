package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateController.PAGE_TITLE;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney.LicenceScheduleSelectionController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceStartDateController.class)
class LicenceStartDateControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceStartDateValidator licenceStartDateValidator;

  @MockitoBean
  private LicenceStartDateService licenceStartDateService;

  @MockitoBean
  private LicenceScheduleService licenceScheduleService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private Licence licence;
  private static final Integer LICENCE_ID = 1;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
    
    licence = new Licence();
    when(licenceService.findLicenceByIdOrThrow(LICENCE_ID)).thenReturn(licence);
  }

  @SecurityTest
  void renderScheduleDetailsForm_scheduleExists() throws Exception {
    when(licenceScheduleService.doesLicenceScheduleExistForLicence(licence)).thenReturn(true);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceStartDateController.class).renderScheduleDetailsForm(LICENCE_ID, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/startDate"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE));
  }

  @SecurityTest
  void renderScheduleDetailsForm_scheduleDoesNotExist() throws Exception {
    when(licenceScheduleService.doesLicenceScheduleExistForLicence(licence)).thenReturn(false);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceStartDateController.class).renderScheduleDetailsForm(LICENCE_ID, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/startDate"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(LicenceScheduleSelectionController.class).renderSelectLicenceForSchedule())));
  }

  @SecurityTest
  void submitScheduleDetailsForm_validForm() throws Exception {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setLicenceScheduleDetail(licenceScheduleDetail);

    when(licenceStartDateValidator.isValid(any(), any())).thenReturn(true);
    when(licenceStartDateService.saveNewLicenceStartDateFromForm(any(), eq(licence))).thenReturn(licenceStartDate);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceStartDateController.class).submitScheduleDetailsForm(LICENCE_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());
  }

  @SecurityTest
  void submitScheduleDetailsForm_invalidForm() throws Exception {
    when(licenceStartDateValidator.isValid(any(), any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceStartDateController.class).submitScheduleDetailsForm(LICENCE_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/startDate"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE));
  }
}