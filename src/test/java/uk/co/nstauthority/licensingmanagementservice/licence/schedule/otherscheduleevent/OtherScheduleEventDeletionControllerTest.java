package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

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
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = OtherScheduleEventDeletionController.class)
class OtherScheduleEventDeletionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private OtherScheduleEventService otherScheduleEventService;
  
  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private Licence licence;
  private LicenceScheduleDetail licenceScheduleDetail;

  private OtherScheduleEvent otherScheduleEvent;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    licence = LicenceTestUtil.builder().build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    otherScheduleEvent = new OtherScheduleEvent();
    otherScheduleEvent.setId(UUID.randomUUID());
    otherScheduleEvent.setLicenceScheduleDetail(licenceScheduleDetail);
    otherScheduleEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    otherScheduleEvent.setDescription("description");
    otherScheduleEvent.setEventDate(LocalDate.of(2025, 1, 1));
    otherScheduleEvent.setComments("comments");
  }

  @Test
  void renderDeleteEventPage() throws Exception {
    when(otherScheduleEventService.getOtherScheduleEventByIdOrThrow(otherScheduleEvent.getId())).thenReturn(otherScheduleEvent);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");

    mockMvc.perform(
            get(ReverseRouter.route(on(OtherScheduleEventDeletionController.class).renderDeleteEventPage(otherScheduleEvent.getId())))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/deleteOtherScheduleEvent"))
        .andExpect(model().attribute("pageTitle", "Do you want to delete the %s event?".formatted(otherScheduleEvent.getCategoryString())))
        .andExpect(model().attribute("summaryView", OtherScheduleEventSummaryView.fromOtherScheduleEvent(otherScheduleEvent)))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", "caption"));
  }

  @Test
  void submitDeleteEventPage() throws Exception {
    when(otherScheduleEventService.getOtherScheduleEventByIdOrThrow(otherScheduleEvent.getId())).thenReturn(otherScheduleEvent);

    mockMvc.perform(
            post(ReverseRouter.route(on(OtherScheduleEventDeletionController.class).submitDeleteEventPage(otherScheduleEvent.getId(), null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(otherScheduleEventService).deleteOtherScheduleEvent(otherScheduleEvent);
  }

}