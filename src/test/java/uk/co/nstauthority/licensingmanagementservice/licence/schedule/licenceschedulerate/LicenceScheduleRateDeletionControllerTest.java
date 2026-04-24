package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicenceScheduleRateDeletionController.class)
class LicenceScheduleRateDeletionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleRateService licenceScheduleRateService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private Licence licence;
  private LicenceScheduleDetail licenceScheduleDetail;

  private LicenceScheduleRate rate;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    licence = LicenceTestUtil.builder().build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setLicenceScheduleDetail(licenceScheduleDetail);
    rate.setStartDate(LocalDate.now());
    rate.setRentalRate(BigDecimal.ONE);
    rate.setComments("comments");
  }

  @Test
  void renderDeletePhasePage() throws Exception {
    when(licenceScheduleRateService.getRateByIdOrThrow(rate.getId())).thenReturn(rate);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleRateDeletionController.class).renderDeleteRatePage(rate.getId())))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/deleteScheduleRate"))
        .andExpect(model().attribute("pageTitle", "Do you want to delete this rate?"))
        .andExpect(model().attribute("summaryView", LicenceScheduleRateSummaryView.from(rate)))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", "caption"));
  }

  @Test
  void submitDeletePhasePage() throws Exception {
    when(licenceScheduleRateService.getRateByIdOrThrow(rate.getId())).thenReturn(rate);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleRateDeletionController.class).submitDeleteRatePage(rate.getId(), null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleRateService).deleteLicenceScheduleRate(rate);
  }

}