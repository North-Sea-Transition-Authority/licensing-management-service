package uk.co.nstauthority.licensingmanagementservice.gis;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.JsonFeature;
import uk.co.fivium.gisframework.feature.JsonFeatureOutlineNodes;
import uk.co.fivium.gisframework.feature.JsonFeatureOutlineNodesResponse;
import uk.co.fivium.gisframework.feature.JsonFeatures;
import uk.co.fivium.gisframework.feature.JsonOutlineNode;
import uk.co.fivium.gisframework.feature.JsonTextualDescription;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.feature.TextualDescriptionService;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;

@ContextConfiguration(classes = FeatureRestController.class)
class FeatureRestControllerTest extends AbstractControllerTest {

  @MockitoBean
  private FeatureService featureService;

  @MockitoBean
  private PolygonService polygonService;

  @MockitoBean
  private LineService lineService;

  @MockitoBean
  private CommandJourneyService commandJourneyService;

  @MockitoBean
  private TextualDescriptionService textualDescriptionService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void getFeaturesEsriJson_assertJsonMap() throws Exception {
    var feature = FeatureTestUtil.builder().build();
    var jsonFeatures = new JsonFeatures(
        List.of(new JsonFeature(Map.of("rings", List.of()), JsonFeature.Attributes.from(feature))),
        JsonFeatures.SpatialReference.from(CoordinateSystem.WGS84)
    );

    when(featureService.getFeaturesByIds(List.of(feature.getId()))).thenReturn(List.of(feature));
    when(polygonService.getFeaturesAsWgs84EsriJson(List.of(feature))).thenReturn(jsonFeatures);

    mockMvc.perform(get("/api/gis-framework/features")
            .param("featureIds", feature.getId().toString())
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(jsonFeatures), JsonCompareMode.STRICT));
  }

  @Test
  void getCommandJourneyActiveFeatures_assertJsonMap() throws Exception {
    var commandJourneyId = UUID.randomUUID();
    var feature = FeatureTestUtil.builder().build();
    var jsonFeatures = new JsonFeatures(
        List.of(new JsonFeature(Map.of("rings", List.of()), JsonFeature.Attributes.from(feature))),
        JsonFeatures.SpatialReference.from(CoordinateSystem.WGS84)
    );

    when(commandJourneyService.getActiveFeatures(commandJourneyId)).thenReturn(List.of(feature));
    when(polygonService.getFeaturesAsWgs84EsriJson(List.of(feature))).thenReturn(jsonFeatures);

    mockMvc.perform(get("/api/gis-framework/command-journey-features/{commandJourneyId}", commandJourneyId)
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(jsonFeatures), JsonCompareMode.STRICT));
  }

  @Test
  void getCommandJourneyActiveOutlineNodes() throws Exception {
    var commandJourneyId = UUID.randomUUID();
    var feature1 = FeatureTestUtil.builder().build();
    var feature2 = FeatureTestUtil.builder().build();

    var feature1Nodes = List.of(
        new JsonOutlineNode("polygon-1", "line-1", 0, 1, 2, 3, "(1)"),
        new JsonOutlineNode("polygon-1", "line-1", 0, 2, 4, 5, "(2)")
    );
    var feature2Nodes = List.of(
        new JsonOutlineNode("polygon-2", "line-2", 1, 1, 6, 7, "(1)")
    );

    var featureOutlineNodes = List.of(
        new JsonFeatureOutlineNodes(feature1.getId().toString(), feature1Nodes),
        new JsonFeatureOutlineNodes(feature2.getId().toString(), feature2Nodes)
    );
    var expected = new JsonFeatureOutlineNodesResponse(featureOutlineNodes);

    when(commandJourneyService.getActiveFeatures(commandJourneyId)).thenReturn(List.of(feature1, feature2));
    when(lineService.getOutlineNodes(List.of(feature1, feature2))).thenReturn(featureOutlineNodes);

    mockMvc.perform(get("/api/gis-framework/command-journey-outline-nodes/{commandJourneyId}", commandJourneyId)
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(expected), JsonCompareMode.STRICT));
  }

  @Test
  void getOutlineNodes() throws Exception {
    var feature1 = FeatureTestUtil.builder().build();
    var feature2 = FeatureTestUtil.builder().build();

    var feature1Nodes = List.of(
        new JsonOutlineNode("polygon-1", "line-1", 0, 1, 2, 3, "(1)"),
        new JsonOutlineNode("polygon-1", "line-1", 0, 2, 4, 5, "(2)")
    );
    var feature2Nodes = List.of(
        new JsonOutlineNode("polygon-2", "line-2", 1, 1, 6, 7, "(1)")
    );

    var featureOutlineNodes = List.of(
        new JsonFeatureOutlineNodes(feature1.getId().toString(), feature1Nodes),
        new JsonFeatureOutlineNodes(feature2.getId().toString(), feature2Nodes)
    );
    var expected = new JsonFeatureOutlineNodesResponse(featureOutlineNodes);

    when(featureService.getFeaturesByIds(List.of(feature1.getId(), feature2.getId())))
        .thenReturn(List.of(feature1, feature2));
    when(lineService.getOutlineNodes(List.of(feature1, feature2))).thenReturn(featureOutlineNodes);

    mockMvc.perform(get("/api/gis-framework/outline-nodes")
            .param("featureIds", feature1.getId().toString(), feature2.getId().toString())
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(expected), JsonCompareMode.STRICT));
  }

  @Test
  void getTextualDescription() throws Exception {
    var feature1 = FeatureTestUtil.builder().build();
    var feature2 = FeatureTestUtil.builder().build();

    var description = String.join("\n",
        "<div class=\"gis-textual-description\">",
        "<div class=\"gis-textual-description__feature\">",
        "<p>Subarea 30/1a is bounded by the following coordinates:</p>",
        "<table class=\"gis-textual-description__coordinates\"><tbody>",
        "<tr><td class=\"gis-textual-description__label\">(1)</td>" +
            "<td class=\"gis-textual-description__ordinate\">1E</td>" +
            "<td class=\"gis-textual-description__ordinate\">1N</td></tr>",
        "</tbody></table>",
        "<p>The above coordinates were specified using \"British National Grid\".</p>",
        "</div>",
        "</div>");
    var expected = new JsonTextualDescription(description);

    when(featureService.getFeaturesByIds(List.of(feature1.getId(), feature2.getId())))
        .thenReturn(List.of(feature1, feature2));
    when(textualDescriptionService.getTextualDescription(List.of(feature1, feature2))).thenReturn(description);

    mockMvc.perform(get("/api/gis-framework/textual-description")
            .param("featureId", feature1.getId().toString(), feature2.getId().toString())
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(expected), JsonCompareMode.STRICT));
  }
}
