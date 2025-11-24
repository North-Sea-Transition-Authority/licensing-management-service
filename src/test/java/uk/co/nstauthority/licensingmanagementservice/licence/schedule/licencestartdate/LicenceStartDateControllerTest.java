package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

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
import static uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateController.PAGE_TITLE;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney.StartLicenceScheduleJourneyController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceStartDateController.class)
class LicenceStartDateControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceStartDateValidator licenceStartDateValidator;

  @MockitoBean
  private LicenceStartDateService licenceStartDateService;

  @MockitoBean
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private Licence licence;
  private static final Integer LICENCE_ID = 1;

  private LicenceScheduleDetail licenceScheduleDetail;
  private static final UUID LICENCE_SCHEDULE_DETAIL_ID = UUID.randomUUID();

  private static final String PAGE_CAPTION = "pageCaption";

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();
    
    licence = new Licence();
    when(licenceService.findLicenceByIdOrThrow(LICENCE_ID)).thenReturn(licence);

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(LICENCE_SCHEDULE_DETAIL_ID, licenceSchedule);
    when(licenceScheduleDetailService.getByIdOrThrow(LICENCE_SCHEDULE_DETAIL_ID)).thenReturn(licenceScheduleDetail);
  }

  @SecurityTest
  void renderLicenceStartDateForm() throws Exception {
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceStartDateController.class).renderLicenceStartDateForm(LICENCE_ID, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/startDate"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("backUrl", ReverseRouter.route(on(StartLicenceScheduleJourneyController.class).renderStartLicenceScheduleJourney(LICENCE_ID, null))))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));
  }

  @SecurityTest
  void submitLicenceStartDateForm_validForm() throws Exception {
    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setLicenceScheduleDetail(licenceScheduleDetail);

    when(licenceStartDateValidator.isValid(any(), any())).thenReturn(true);
    when(licenceStartDateService.saveNewLicenceStartDateFromForm(any(), eq(licence))).thenReturn(licenceStartDate);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceStartDateController.class).submitLicenceStartDateForm(LICENCE_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());
  }

  @SecurityTest
  void submitLicenceStartDateForm_invalidForm() throws Exception {
    when(licenceStartDateValidator.isValid(any(), any())).thenReturn(false);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceStartDateController.class).submitLicenceStartDateForm(LICENCE_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/startDate"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("backUrl", ReverseRouter.route(on(StartLicenceScheduleJourneyController.class).renderStartLicenceScheduleJourney(LICENCE_ID, null))));
  }

  @SecurityTest
  void renderLicenceStartDateUpdateForm() throws Exception {
    when(licenceStartDateService.getLicenceStartDateForm(licenceScheduleDetail)).thenReturn(new LicenceStartDateForm());
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceStartDateController.class).renderLicenceStartDateUpdateForm(LICENCE_SCHEDULE_DETAIL_ID, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/startDate"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("backUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));
  }

  @SecurityTest
  void submitLicenceStartDateUpdateForm_validForm() throws Exception {
    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setLicenceScheduleDetail(licenceScheduleDetail);

    when(licenceStartDateValidator.isValid(any(), any())).thenReturn(true);
    when(licenceStartDateService.saveOrUpdateLicenceStartDateFromForm(any(), eq(licenceScheduleDetail))).thenReturn(licenceStartDate);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceStartDateController.class).submitLicenceStartDateUpdateForm(LICENCE_SCHEDULE_DETAIL_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @SecurityTest
  void submitLicenceStartDateUpdateForm_invalidForm() throws Exception {
    when(licenceStartDateValidator.isValid(any(), any())).thenReturn(false);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceStartDateController.class).submitLicenceStartDateUpdateForm(LICENCE_SCHEDULE_DETAIL_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/startDate"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("backUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION));
  }
}