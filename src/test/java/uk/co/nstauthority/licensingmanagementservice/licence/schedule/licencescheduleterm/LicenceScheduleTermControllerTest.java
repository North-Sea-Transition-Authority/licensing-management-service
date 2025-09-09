package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

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

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceScheduleTermController.class)
class LicenceScheduleTermControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleTermFormService licenceScheduleTermFormService;

  @MockitoBean
  private LicenceScheduleTermFormValidator licenceScheduleTermFormValidator;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private LicenceScheduleDetail licenceScheduleDetail;
  private static final UUID LICENCE_SCHEDULE_DETAIL_ID = UUID.randomUUID();


  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var licence = new Licence();
    licence.setType(LicenceType.SEAWARD_PRODUCTION);

    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);

    licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);
    licenceScheduleDetail.setId(LICENCE_SCHEDULE_DETAIL_ID);

    when(licenceScheduleDetailService.getByIdOrThrow(LICENCE_SCHEDULE_DETAIL_ID)).thenReturn(licenceScheduleDetail);
  }

  @SecurityTest
  void renderAddNewTermForm() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTermController.class).renderAddNewTermForm(LICENCE_SCHEDULE_DETAIL_ID, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createScheduleTerm"))
        .andExpect(model().attribute("radioOptions", TermType.getTermRadioOptionsFor(LicenceType.SEAWARD_PRODUCTION)))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(LicenceScheduleTimelineController.class)
            .renderLicenceScheduleTimeline(LICENCE_SCHEDULE_DETAIL_ID, null))));
  }

  @Test
  void submitAddNewTermForm_validForm() throws Exception {
    when(licenceScheduleTermFormValidator.isValid(any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleTermController.class).submitAddNewTermForm(LICENCE_SCHEDULE_DETAIL_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleTermFormService).saveTermFromForm(any(), eq(licenceScheduleDetail));
  }

  @Test
  void submitAddNewTermForm_invalidForm() throws Exception {
    when(licenceScheduleTermFormValidator.isValid(any(), any(), any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleTermController.class).submitAddNewTermForm(LICENCE_SCHEDULE_DETAIL_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createScheduleTerm"))
        .andExpect(model().attribute("radioOptions", TermType.getTermRadioOptionsFor(LicenceType.SEAWARD_PRODUCTION)));

    verify(licenceScheduleTermFormService, never()).saveTermFromForm(any(), any());
  }

}