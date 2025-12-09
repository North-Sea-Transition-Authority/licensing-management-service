package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

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
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.LicenceScheduleRelativeOptionsService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceScheduleRateController.class)
class LicenceScheduleRateControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleRateFormService licenceScheduleRateFormService;

  @MockitoBean
  private LicenceScheduleRateFormValidator licenceScheduleRateFormValidator;

  @MockitoBean
  private LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private Licence licence;
  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence)
    );

    when(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetail.getId())).thenReturn(licenceScheduleDetail);
  }

  @SecurityTest
  void renderNewLicenceScheduleRateForm() throws Exception {
    var pageCaption = "P001";

    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRateFormService.getRateDefinitionOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceService.getLicencePageCaption(licence)).thenReturn(pageCaption);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleRateController.class).renderNewLicenceScheduleRateForm(licenceScheduleDetail.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createScheduleRate"))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("rateDefinitionOptions", Map.of()))
        .andExpect(model().attribute("relativeEventOptions", Map.of()))
        .andExpect(model().attribute("relativeDateOptions", RateRelativeDateOption.getRateRelativeDateOptions()))
        .andExpect(model().attribute("pageCaption", pageCaption))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()));
  }

  @Test
  void submitNewLicenceScheduleRateForm() throws Exception {
    when(licenceScheduleRateFormValidator.isValid(any(LicenceScheduleRateForm.class), any(Errors.class))).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleRateController.class).renderNewLicenceScheduleRateForm(licenceScheduleDetail.getId(), null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleRateFormService).saveRateFromForm(any(), eq(licenceScheduleDetail));
  }

  @Test
  void submitNewLicenceScheduleRateForm_invalid() throws Exception {
    when(licenceScheduleRateFormValidator.isValid(any(LicenceScheduleRateForm.class), any(Errors.class))).thenReturn(false);

    var pageCaption = "P001";

    when(licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceScheduleRateFormService.getRateDefinitionOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceService.getLicencePageCaption(licence)).thenReturn(pageCaption);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleRateController.class).renderNewLicenceScheduleRateForm(licenceScheduleDetail.getId(), null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createScheduleRate"))
        .andExpect(model().attribute("termOptions", Map.of()))
        .andExpect(model().attribute("phaseOptions", Map.of()))
        .andExpect(model().attribute("rateDefinitionOptions", Map.of()))
        .andExpect(model().attribute("relativeEventOptions", Map.of()))
        .andExpect(model().attribute("relativeDateOptions", RateRelativeDateOption.getRateRelativeDateOptions()))
        .andExpect(model().attribute("pageCaption", pageCaption))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()));

    verify(licenceScheduleRateFormService, never()).saveRateFromForm(any(), any());
  }

}