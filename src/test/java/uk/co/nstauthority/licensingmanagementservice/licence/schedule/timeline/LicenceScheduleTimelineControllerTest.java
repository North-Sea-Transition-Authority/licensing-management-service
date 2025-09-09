package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceScheduleTimelineController.class)
class LicenceScheduleTimelineControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleTimelineService licenceScheduleTimelineService;

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private Licence licence;

  private LicenceScheduleDetail licenceScheduleDetail;
  private static final UUID LICENCE_SCHEDULE_DETAIL_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    licence = new Licence();
    licence.setType(LicenceType.SEAWARD_PRODUCTION);
    licence.setLicenceReference("P1");

    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);

    licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);

    when(licenceScheduleDetailService.getByIdOrThrow(LICENCE_SCHEDULE_DETAIL_ID)).thenReturn(licenceScheduleDetail);
  }

  @SecurityTest
  void renderLicenceScheduleTimeline() throws Exception {
    var timelineSummaryCardView = new TimelineSummaryCardView("date");

    when(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail)).thenReturn(timelineSummaryCardView);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleTimelineController.class).renderLicenceScheduleTimeline(LICENCE_SCHEDULE_DETAIL_ID, null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/scheduleTimeline"))
        .andExpect(model().attribute("pageTitle", LicenceScheduleTimelineController.PAGE_TITLE.formatted(licence.getLicenceReference())))
        .andExpect(model().attribute("timelineSummaryCardView", timelineSummaryCardView));
  }
}