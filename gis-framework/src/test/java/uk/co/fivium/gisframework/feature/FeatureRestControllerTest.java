package uk.co.fivium.gisframework.feature;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import uk.co.fivium.gisframework.AbstractControllerTest;
import uk.co.fivium.grpc.gis.CoordinateSystem;

@ContextConfiguration(classes = FeatureRestController.class)
class FeatureRestControllerTest extends AbstractControllerTest {

  @MockitoBean
  private FeatureService featureService;

  @MockitoBean
  private PolygonService polygonService;

  @MockitoBean
  private LineService lineService;

  @MockitoBean
  private TextualDescriptionService textualDescriptionService;

  //TODO EPGF-153 add auth to rest endpoints and add tests

  @Test
  void getFeaturesEsriJson_assertJsonMap() throws Exception {
    var feature = FeatureTestUtil.newBuilder().build();
    var esriJsonPolygon1 = """
        {
          "rings": [[[0, 0], [1, 0], [1, 1], [0, 0]]],
          "spatialReference": {"wkid": 4326}
        }
        """;
    var esriJsonPolygon2 = """
        {
          "rings": [[[2, 2], [3, 2], [3, 3], [2, 2]]],
          "spatialReference": {"wkid": 4326}
        }
        """;

    when(featureService.getFeaturesByIds(List.of(feature.getId()))).thenReturn(List.of(feature));
    when(polygonService.getPolygonsAsEsriJson(feature, true))
        .thenReturn(List.of(esriJsonPolygon1, esriJsonPolygon2));

    List<Map<String, Object>> esriJsonFeatures = new ArrayList<>();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("featureId", feature.getId().toString());
    attributes.put("featureName", feature.getFeatureName());

    Map<String, Object> esriJsonFeature1 = new HashMap<>();
    esriJsonFeature1.put("geometry", objectMapper.readValue(esriJsonPolygon1, Map.class));
    esriJsonFeature1.put("attributes", attributes);
    esriJsonFeatures.add(esriJsonFeature1);

    Map<String, Object> esriJsonFeature2 = new HashMap<>();
    esriJsonFeature2.put("geometry", objectMapper.readValue(esriJsonPolygon2, Map.class));
    esriJsonFeature2.put("attributes", attributes);
    esriJsonFeatures.add(esriJsonFeature2);

    Map<String, Object> expectedFeatureSetMap = new HashMap<>();
    expectedFeatureSetMap.put("features", esriJsonFeatures);
    expectedFeatureSetMap.put("spatialReference",
        Map.of("wkid", CoordinateSystemUtils.getWkid(CoordinateSystem.WGS84)));

    mockMvc.perform(get("/api/gis-framework/features")
            .param("featureIds", feature.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(expectedFeatureSetMap), JsonCompareMode.STRICT));
  }

  @Test
  void getOutlineNodes() throws Exception {
    var feature1 = FeatureTestUtil.newBuilder().build();
    var feature2 = FeatureTestUtil.newBuilder().build();

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
            .param("featureIds", feature1.getId().toString(), feature2.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(expected), JsonCompareMode.STRICT));
  }

  @Test
  void getTextualDescription() throws Exception {
    var feature1 = FeatureTestUtil.newBuilder().build();
    var feature2 = FeatureTestUtil.newBuilder().build();

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
            .param("featureId", feature1.getId().toString(), feature2.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(expected), JsonCompareMode.STRICT));
  }
}
