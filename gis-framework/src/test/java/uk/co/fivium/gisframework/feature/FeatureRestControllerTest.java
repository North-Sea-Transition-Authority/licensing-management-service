package uk.co.fivium.gisframework.feature;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityNotFoundException;
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

    when(featureService.getFeatureOrThrow(feature.getId())).thenReturn(feature);
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

    mockMvc.perform(get("/api/gis-framework/feature/{featureId}", feature.getId()))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(expectedFeatureSetMap), JsonCompareMode.STRICT));
  }

  @Test
  void getFeaturesEsriJson_featureNotFound_throw404() throws Exception {
    var feature = FeatureTestUtil.newBuilder().build();

    when(featureService.getFeatureOrThrow(feature.getId())).thenThrow(new EntityNotFoundException());

    mockMvc.perform(get("/api/gis-framework/feature/{featureId}", feature.getId()))
        .andExpect(status().isNotFound());
  }
}
