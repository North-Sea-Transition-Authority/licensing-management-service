package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceSchedulePhaseController.class)
class LicenceSchedulePhaseControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceSchedulePhaseFormService licenceSchedulePhaseFormService;

  @MockitoBean
  private LicenceSchedulePhaseFormValidator licenceSchedulePhaseFormValidator;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private LicenceScheduleDetail licenceScheduleDetail;
  private static final UUID LICENCE_SCHEDULE_DETAIL_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    licenceScheduleDetail = LicenceTestUtil.createLicenceScheduleDetail(licence, LICENCE_SCHEDULE_DETAIL_ID);

    when(licenceScheduleDetailService.getByIdOrThrow(LICENCE_SCHEDULE_DETAIL_ID)).thenReturn(licenceScheduleDetail);
  }

  @SecurityTest
  void renderAddNewPhaseForm() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderAddNewPhaseForm(LICENCE_SCHEDULE_DETAIL_ID, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createSchedulePhase"))
        .andExpect(model().attribute("radioOptions", PhaseType.getPhaseRadioOptionsFor(LicenceType.SEAWARD_PRODUCTION)))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(LicenceScheduleTimelineController.class)
            .renderLicenceScheduleTimeline(LICENCE_SCHEDULE_DETAIL_ID, null))));
  }

  @Test
  void submitAddNewPhaseForm_validForm() throws Exception {
    when(licenceSchedulePhaseFormValidator.isValid(any(), any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceSchedulePhaseController.class).submitAddNewPhaseForm(LICENCE_SCHEDULE_DETAIL_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceSchedulePhaseFormService).savePhaseFromForm(any(), eq(licenceScheduleDetail));
  }

  @Test
  void submitAddNewPhaseForm_invalidForm() throws Exception {
    when(licenceSchedulePhaseFormValidator.isValid(any(), any(), any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceSchedulePhaseController.class).submitAddNewPhaseForm(LICENCE_SCHEDULE_DETAIL_ID, null, null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/createSchedulePhase"))
        .andExpect(model().attribute("radioOptions", PhaseType.getPhaseRadioOptionsFor(LicenceType.SEAWARD_PRODUCTION)))
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(LicenceScheduleTimelineController.class)
            .renderLicenceScheduleTimeline(LICENCE_SCHEDULE_DETAIL_ID, null))));

    verify(licenceSchedulePhaseFormService, never()).savePhaseFromForm(any(), any());
  }

}