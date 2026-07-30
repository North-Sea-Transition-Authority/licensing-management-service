package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionController.class)
@ActiveProfiles({"test", "enable-lms2"})
class LicencePositionControllerTest extends AbstractControllerTest {

  private static final Integer LICENCE_ID = 1;
  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(LICENCE_ID)
      .withLicenceReference("REF-1")
      .build();
  private static final String PAGE_CAPTION = "licence - 1";
  private static final UUID POSITION_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    when(licenceService.findLicenceByIdOrThrow(LICENCE_ID)).thenReturn(LICENCE);
  }

  @Test
  void renderLicencePositionTimeline_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionController.class).renderLicencePositionTimeline(LICENCE))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderLicencePositionTimeline_redirectsToLatestPosition() throws Exception {
    var older  = LicencePositionTestUtil.newBuilder().withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).build();
    var latest = LicencePositionTestUtil.newBuilder().withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(older, latest));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionController.class).renderLicencePositionTimeline(LICENCE)))
            .with(user(regulatorUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionController.class)
            .renderLicencePosition(LICENCE, latest.getId()))));
  }

  @Test
  void renderLicencePositionTimeline_whenNoPositions() throws Exception {
    when(licenceService.getLicencePageCaption(LICENCE)).thenReturn(PAGE_CAPTION);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionController.class)
            .renderLicencePositionTimeline(LICENCE)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/position/licencePositions"),
            model().attributeExists("licencePositionPageView")
        );
  }

  @Test
  void renderLicencePosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionController.class)
            .renderLicencePosition(LICENCE, POSITION_ID))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderLicencePosition() throws Exception {
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var pageView = new LicencePositionPageView(
        List.of(new LicencePositionTimelineView(POSITION_ID, "url1", "REF-2", "5 June 2026", false, null, false, null, null, false, null, null)),
        position.getFormattedPositionDate(),
        position.getLicence().getLicenceReference(),
        Map.of(),
        new LicencePositionStateView(
            new AdministratorStateView("admin organisation")
        ),
        false,
        POSITION_ID,
        false,
        LicencePositionPageView.Actions.none()
    );

    when(licenceService.getLicencePageCaption(LICENCE)).thenReturn(PAGE_CAPTION);
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionViewService.getPositionPageView(position)).thenReturn(pageView);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionController.class)
            .renderLicencePosition(LICENCE, POSITION_ID)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/position/licencePositions"),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("licencePositionPageView", pageView)
        );
  }
}