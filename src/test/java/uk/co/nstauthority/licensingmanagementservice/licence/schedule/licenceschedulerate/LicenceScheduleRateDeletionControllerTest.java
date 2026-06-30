package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = LicenceScheduleRateDeletionController.class)
class LicenceScheduleRateDeletionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleRateService licenceScheduleRateService;

  @MockitoBean
  private EventCommentService eventCommentService;

  private Licence licence;
  private LicenceScheduleDetail licenceScheduleDetail;

  private LicenceScheduleRate rate;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder().build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setLicenceScheduleDetail(licenceScheduleDetail);
    rate.setStartDate(LocalDate.now());
    rate.setRentalRate(BigDecimal.ONE);
  }

  @Test
  void renderDeleteRatePage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleRateService.getRateByIdOrThrow(rate.getId())).thenReturn(rate);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleRateDeletionController.class).renderDeleteRatePage(rate.getId())))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/deleteScheduleRate"))
        .andExpect(model().attribute("pageTitle", "Do you want to delete this rate?"))
        .andExpect(model().attribute("summaryView", LicenceScheduleRateSummaryView.from(rate)))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", "caption"))
        .andExpect(model().attribute("pendingComment", ""));
  }

  @Test
  void renderDeleteRatePage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);
    when(licenceScheduleRateService.getRateByIdOrThrow(rate.getId())).thenReturn(rate);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleRateDeletionController.class).renderDeleteRatePage(rate.getId())))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void submitDeleteRatePage() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleRateService.getRateByIdOrThrow(rate.getId())).thenReturn(rate);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleRateDeletionController.class).submitDeleteRatePage(rate.getId(), null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(licenceScheduleRateService).deleteLicenceScheduleRate(rate);
  }

  @Test
  void submitDeleteRatePage_noRoles() throws Exception {
    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleRateDeletionController.class).submitDeleteRatePage(rate.getId(), null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());

    verify(licenceScheduleRateService, never()).deleteLicenceScheduleRate(rate);
  }

  @Test
  void renderDeleteRatePage_whenRateHasEventReferenceAndPendingCommentExists_showsPendingComment() throws Exception {
    var eventReference = new EventReference();
    rate.setEventReference(eventReference);

    var eventComment = new EventComment();
    eventComment.setComment("a pending comment");

    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleRateService.getRateByIdOrThrow(rate.getId())).thenReturn(rate);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");
    when(eventCommentService.findPendingCommentForEventReference(eventReference)).thenReturn(Optional.of(eventComment));

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleRateDeletionController.class).renderDeleteRatePage(rate.getId())))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("pendingComment", "a pending comment"));
  }

  @Test
  void renderDeleteRatePage_whenRateHasEventReferenceAndNoPendingComment_showsEmptyPendingComment() throws Exception {
    var eventReference = new EventReference();
    rate.setEventReference(eventReference);

    when(teamQueryService.userHasRoleInTeamType(regulatorUser.wuaId(), TeamType.LICENCE_MANAGEMENT, Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(licenceScheduleRateService.getRateByIdOrThrow(rate.getId())).thenReturn(rate);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");
    when(eventCommentService.findPendingCommentForEventReference(eventReference)).thenReturn(Optional.empty());

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleRateDeletionController.class).renderDeleteRatePage(rate.getId())))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(model().attribute("pendingComment", ""));
  }

}