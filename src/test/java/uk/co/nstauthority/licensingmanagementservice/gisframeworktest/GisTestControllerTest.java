package uk.co.nstauthority.licensingmanagementservice.gisframeworktest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.mockups.gis.GisTestController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = GisTestController.class)
@ActiveProfiles("mockups")
class GisTestControllerTest extends AbstractControllerTest {

  @MockitoBean
  private FeatureService featureService;

  @Test
  void renderSplitByPointAndClick_whenNotLoggedIn() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(GisTestController.class).renderSplitByPointAndClick())))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderSplitByPointAndClick_assertModelProperties() throws Exception {
    UUID ed50Id = UUID.randomUUID();
    var ed50Feature = getMockFeature(ed50Id);
    UUID bngId = UUID.randomUUID();
    var bngFeature = getMockFeature(bngId);

    when(featureService.findFeatureOrThrow(CoordinateSystem.ED50)).thenReturn(ed50Feature);
    when(featureService.findFeatureOrThrow(CoordinateSystem.BRITISH_NATIONAL_GRID)).thenReturn(bngFeature);

    mockMvc.perform(get(ReverseRouter.route(on(GisTestController.class).renderSplitByPointAndClick()))
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/mockups/gis/pointAndClickMapTester"))
        .andExpect(model().attribute("featureIdsEd50", List.of(ed50Id.toString())))
        .andExpect(model().attribute("featureIdsBng", List.of(bngId.toString())));
  }

  private Feature getMockFeature(UUID featureId) {
    var mock = mock(Feature.class);
    when(mock.getId()).thenReturn(featureId);
    return mock;
  }
}