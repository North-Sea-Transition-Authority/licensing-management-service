package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceScheduleTermDeletionController.class)
class LicenceScheduleTermDeletionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleTermService licenceScheduleTermService;

  @MockitoBean
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private LicenceScheduleDetail licenceScheduleDetail;

  private LicenceScheduleTerm licenceScheduleTerm;
  private static final UUID LICENCE_SCHEDULE_TERM_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var licence = LicenceTestUtil.builder().build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setId(LICENCE_SCHEDULE_TERM_ID);
    licenceScheduleTerm.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleTerm.setTermType(TermType.INITIAL);
    licenceScheduleTerm.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    licenceScheduleTerm.setStartDate(LocalDate.of(2025, 1, 1));
    licenceScheduleTerm.setEndDate(LocalDate.of(2025, 12, 31));
  }

  @SecurityTest
  void renderDeleteTermPage() throws Exception {
    when(licenceScheduleTermService.getTermByIdOrThrow(LICENCE_SCHEDULE_TERM_ID)).thenReturn(licenceScheduleTerm);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(LICENCE_SCHEDULE_TERM_ID)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/deleteScheduleTerm"))
        .andExpect(model().attribute("pageTitle", "Do you want to delete the %s?".formatted(licenceScheduleTerm.getTermType().getDisplayName())))
        .andExpect(model().attribute("licenceScheduleTermSummaryView", LicenceScheduleTermSummaryView.fromTerm(licenceScheduleTerm)))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(LicenceScheduleTimelineController.class)
            .renderLicenceScheduleTimeline(licenceScheduleDetail.getId(), null))));
  }

  @Test
  void submitDeleteTermPage() throws Exception {
    when(licenceScheduleTermService.getTermByIdOrThrow(LICENCE_SCHEDULE_TERM_ID)).thenReturn(licenceScheduleTerm);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).submitDeleteTermPage(LICENCE_SCHEDULE_TERM_ID)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleTermService).deleteTerm(licenceScheduleTerm);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }
}