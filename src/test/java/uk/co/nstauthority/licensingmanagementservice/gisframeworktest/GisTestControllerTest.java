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
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.command.FeatureJourneyStateService;
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

  @MockitoBean
  private CommandJourneyService commandJourneyService;

  @MockitoBean
  private FeatureJourneyStateService featureJourneyStateService;

  @Test
  void renderSplitByPointAndClick_whenNotLoggedIn() throws Exception {
    mockMvc.perform(
            get(ReverseRouter.route(on(GisTestController.class).renderSplitByPointAndClick(CoordinateSystem.ED50))))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderSplitByPointAndClick_assertModelProperties() throws Exception {
    UUID featureId = UUID.randomUUID();
    var feature = getMockFeature(featureId);
    var commandJourneyId = UUID.randomUUID();
    var commandJourney = mock(CommandJourney.class);
    when(commandJourney.getId()).thenReturn(commandJourneyId);

    when(featureJourneyStateService.findFeatureWithNoJourneyStateOrThrow(CoordinateSystem.ED50)).thenReturn(feature);
    when(commandJourneyService.createAndAssignCommandJourney(List.of(feature))).thenReturn(commandJourney);

    mockMvc.perform(get(ReverseRouter.route(on(GisTestController.class).renderSplitByPointAndClick(CoordinateSystem.ED50)))
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/mockups/gis/pointAndClickMapTester"))
        .andExpect(model().attribute("commandJourneyId", commandJourneyId.toString()))
        .andExpect(model().attribute("srsWkid", 4230));
  }

  @Test
  void renderSplitByCoordinateEntry_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(
            on(GisTestController.class).renderSplitByCoordinateEntry(CoordinateSystem.ED50, 4))))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderSplitByCoordinateEntry_assertModelProperties() throws Exception {
    UUID ed50Id = UUID.randomUUID();
    var ed50Feature = getMockFeature(ed50Id);

    when(featureJourneyStateService.findFeatureWithNoJourneyStateOrThrow(CoordinateSystem.ED50)).thenReturn(ed50Feature);

    mockMvc.perform(get(ReverseRouter.route(
                on(GisTestController.class).renderSplitByCoordinateEntry(CoordinateSystem.ED50, 4)))
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/mockups/gis/splitByCoordinateEntryTester"))
        .andExpect(model().attribute("featureIds", List.of(ed50Id.toString())))
        .andExpect(model().attribute("srsWkid", 4230))
        .andExpect(model().attribute("precision", 4));
  }

  @Test
  void renderMapWithTextualDescription_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(
            on(GisTestController.class).renderMapWithTextualDescription(UUID.randomUUID()))))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderMapWithTextualDescription_assertModelProperties() throws Exception {
    UUID featureId = UUID.randomUUID();
    var feature = getMockFeature(featureId);
    when(feature.getCoordinateSystem()).thenReturn(CoordinateSystem.ED50);

    when(featureService.getFeatureOrThrow(featureId)).thenReturn(feature);

    mockMvc.perform(get(ReverseRouter.route(
                on(GisTestController.class).renderMapWithTextualDescription(featureId)))
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(view().name("lms/mockups/gis/mapWithTextualDescriptionTester"))
        .andExpect(model().attribute("featureIds", List.of(featureId.toString())))
        .andExpect(model().attribute("srsWkid", 4230));
  }

  private Feature getMockFeature(UUID featureId) {
    var mock = mock(Feature.class);
    when(mock.getId()).thenReturn(featureId);
    return mock;
  }
}