package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionTimelineController.class)
@ActiveProfiles({"test", "enable-lms2"})
class LicencePositionTimelineControllerTest extends AbstractControllerTest {

  private static final Integer LICENCE_ID = 1;
  private static final Licence LICENCE = LicenceTestUtil.builder().withId(LICENCE_ID).build();
  private static final String PAGE_TITLE = "Licence positions";
  private static final String PAGE_CAPTION = "licence - 1";

  @MockitoBean
  private LicencePositionService licencePositionService;

  @BeforeEach
  void setUp() {
    when(licenceService.findLicenceByIdOrThrow(LICENCE_ID)).thenReturn(LICENCE);
  }

  @Test
  void renderLicencePositionTimeline_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionTimelineController.class).renderLicencePositionTimeline(LICENCE))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderLicencePositionTimeline() throws Exception {
    var licencePositionTimelineView = List.of(
        new LicencePositionTimelineView("REF-2", LocalDate.of(2026,6,5)),
        new LicencePositionTimelineView("REF-1",  LocalDate.of(2026,2,3))
    );

    when(licenceService.getLicencePageCaption(LICENCE)).thenReturn(PAGE_CAPTION);
    when(licencePositionService.getTimelineView(LICENCE)).thenReturn(licencePositionTimelineView);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionTimelineController.class).renderLicencePositionTimeline(LICENCE)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/position/licencePositionTimeline"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("licencePositionTimelineView", licencePositionTimelineView)
        );
  }
}