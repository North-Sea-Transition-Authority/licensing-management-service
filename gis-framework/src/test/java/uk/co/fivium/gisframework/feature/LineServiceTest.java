package uk.co.fivium.gisframework.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.esri.core.geometry.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.operator.LineWithStartEndPoints;

@ExtendWith(MockitoExtension.class)
class LineServiceTest {

  private static final Feature FEATURE_1 = FeatureTestUtil.newBuilder().build();
  private static final Feature FEATURE_2 = FeatureTestUtil.newBuilder().build();
  private static final List<Feature> FEATURES = List.of(FEATURE_1, FEATURE_2);

  private static final Line LINE_1 = LineTestUtil.newBuilder().build();
  private static final Line LINE_2 = LineTestUtil.newBuilder().build();

  @Mock
  private LineRepository lineRepository;

  @Mock
  private GrpcClientService grpcClientService;

  @InjectMocks
  private LineService lineService;

  @Test
  void saveLine() {
    lineService.saveLine(LINE_1);

    verify(lineRepository).save(LINE_1);
  }

  @Test
  void findAllByFeatureIn() {
    when(lineRepository.findAllByPolygon_FeatureIn(FEATURES)).thenReturn(List.of(LINE_1, LINE_2));

    var result = lineService.findAllByFeatureIn(FEATURES);

    var expected = List.of(LINE_1, LINE_2);
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void findAllByFeatureIn_whenNoResult_thenReturnsEmptyList() {
    when(lineRepository.findAllByPolygon_FeatureIn(FEATURES)).thenReturn(List.of());

    var result = lineService.findAllByFeatureIn(FEATURES);

    assertThat(result).isEmpty();
  }

  @Test
  void findAllByFeatureLegacyIdIn() {
    when(lineRepository.findAllByPolygon_Feature_LegacyIdIn(List.of(1, 2))).thenReturn(List.of(LINE_1, LINE_2));
    assertThat(lineService.findAllByFeatureLegacyIdIn(List.of(1, 2))).isEqualTo(List.of(LINE_1, LINE_2));
  }

  @Test
  void saveLines() {
    var lines = List.of(LINE_1, LINE_2);
    lineService.saveLines(lines);
    verify(lineRepository).saveAll(lines);
  }

  @Test
  void getLines() {
    var polygons = List.of(PolygonTestUtil.newBuilder().build(), PolygonTestUtil.newBuilder().build());
    when(lineRepository.findAllByPolygonIn(polygons)).thenReturn(List.of(LINE_1, LINE_2));
    assertThat(lineService.getLines(polygons)).containsExactlyInAnyOrder(LINE_1, LINE_2);
  }

  @Test
  void getPolygonToLines() {
    var polygon1 = PolygonTestUtil.newBuilder().build();
    var polygon2 = PolygonTestUtil.newBuilder().build();
    var line1 = LineTestUtil.newBuilder().withPolygon(polygon1).build();
    var line2 = LineTestUtil.newBuilder().withPolygon(polygon2).build();
    var line3 = LineTestUtil.newBuilder().withPolygon(polygon1).build();

    when(lineRepository.findAllByPolygon_Feature(FEATURE_1)).thenReturn(List.of(line1, line2, line3));

    assertThat(lineService.getPolygonToLines(FEATURE_1)).isEqualTo(
        Map.of(
            polygon1, List.of(line1, line3),
            polygon2, List.of(line2)
        )
    );
  }

  @Test
  void getPolygonToLinesIn() {
    var polygon1 = PolygonTestUtil.newBuilder().withFeature(FEATURE_1).build();
    var polygon2 = PolygonTestUtil.newBuilder().withFeature(FEATURE_2).build();
    var line1 = LineTestUtil.newBuilder().withPolygon(polygon1).build();
    var line2 = LineTestUtil.newBuilder().withPolygon(polygon2).build();
    var line3 = LineTestUtil.newBuilder().withPolygon(polygon1).build();

    when(lineRepository.findAllByPolygon_FeatureIn(FEATURES)).thenReturn(List.of(line1, line2, line3));

    assertThat(lineService.getPolygonToLinesIn(FEATURES)).isEqualTo(
        Map.of(
            polygon1, List.of(line1, line3),
            polygon2, List.of(line2)
        )
    );
  }

  @Test
  void getOutlineNodes_whenMultipleRingsAcrossPolygons_thenContinuousNumbering() {
    var polygon1 = PolygonTestUtil.newBuilder().withFeature(FEATURE_1).build();
    var polygon2 = PolygonTestUtil.newBuilder().withFeature(FEATURE_1).build();

    var ring1Lines = ringLines(polygon1, 1, 1, 4);
    var ring2Lines = ringLines(polygon1, 2, 5, 8);
    var ring3Lines = ringLines(polygon2, 3, 9, 12);

    var allLines = new ArrayList<Line>();
    allLines.addAll(ring1Lines);
    allLines.addAll(ring2Lines);
    allLines.addAll(ring3Lines);

    when(lineRepository.findAllByPolygon_FeatureIn(List.of(FEATURE_1))).thenReturn(allLines);
    when(grpcClientService.getLineStartAndEndPoints(allLines, true)).thenReturn(getTestStartAndEndPoints(allLines));

    var result = lineService.getOutlineNodes(List.of(FEATURE_1));

    assertThat(result).containsExactly(
        new JsonFeatureOutlineNodes(FEATURE_1.getId().toString(),
            List.of(
                new JsonOutlineNode(ring1Lines.get(0), 1, 1, 0),
                new JsonOutlineNode(ring1Lines.get(1), 2, 2, 0),
                new JsonOutlineNode(ring1Lines.get(2), 3, 3, 0),
                new JsonOutlineNode(ring1Lines.get(3), 4, 4, 0),
                new JsonOutlineNode(ring1Lines.get(3), 5, 4, 100),
                new JsonOutlineNode(ring2Lines.get(0), 6, 5, 0),
                new JsonOutlineNode(ring2Lines.get(1), 7, 6, 0),
                new JsonOutlineNode(ring2Lines.get(2), 8, 7, 0),
                new JsonOutlineNode(ring2Lines.get(3), 9, 8, 0),
                new JsonOutlineNode(ring2Lines.get(3), 10, 8, 100),
                new JsonOutlineNode(ring3Lines.get(0), 11, 9, 0),
                new JsonOutlineNode(ring3Lines.get(1), 12, 10, 0),
                new JsonOutlineNode(ring3Lines.get(2), 13, 11, 0),
                new JsonOutlineNode(ring3Lines.get(3), 14, 12, 0),
                new JsonOutlineNode(ring3Lines.get(3), 15, 12, 100)
            )
        ));
  }

  @Test
  void getOutlineNodes_whenMultipleFeatures_thenResetNumbering() {
    var polygon1 = PolygonTestUtil.newBuilder().withFeature(FEATURE_1).build();
    var polygon2 = PolygonTestUtil.newBuilder().withFeature(FEATURE_2).build();

    var ring1Lines = ringLines(polygon1, 1, 1, 4);
    var ring2Lines = ringLines(polygon1, 2, 5, 8);
    var ring3Lines = ringLines(polygon2, 1, 1, 4);

    var feature1Lines = new ArrayList<Line>();
    feature1Lines.addAll(ring1Lines);
    feature1Lines.addAll(ring2Lines);

    var allLines = new ArrayList<Line>();
    allLines.addAll(feature1Lines);
    allLines.addAll(ring3Lines);

    when(lineRepository.findAllByPolygon_FeatureIn(FEATURES)).thenReturn(allLines);
    when(grpcClientService.getLineStartAndEndPoints(allLines, true)).thenReturn(getTestStartAndEndPoints(allLines));

    var result = lineService.getOutlineNodes(FEATURES);

    var feature1Nodes = List.of(
        new JsonOutlineNode(ring1Lines.get(0), 1, 1, 0),
        new JsonOutlineNode(ring1Lines.get(1), 2, 2, 0),
        new JsonOutlineNode(ring1Lines.get(2), 3, 3, 0),
        new JsonOutlineNode(ring1Lines.get(3), 4, 4, 0),
        new JsonOutlineNode(ring1Lines.get(3), 5, 4, 100),
        new JsonOutlineNode(ring2Lines.get(0), 6, 5, 0),
        new JsonOutlineNode(ring2Lines.get(1), 7, 6, 0),
        new JsonOutlineNode(ring2Lines.get(2), 8, 7, 0),
        new JsonOutlineNode(ring2Lines.get(3), 9, 8, 0),
        new JsonOutlineNode(ring2Lines.get(3), 10, 8, 100)
    );
    var feature2Nodes = List.of(
        new JsonOutlineNode(ring3Lines.get(0), 1, 1, 0),
        new JsonOutlineNode(ring3Lines.get(1), 2, 2, 0),
        new JsonOutlineNode(ring3Lines.get(2), 3, 3, 0),
        new JsonOutlineNode(ring3Lines.get(3), 4, 4, 0),
        new JsonOutlineNode(ring3Lines.get(3), 5, 4, 100)
    );

    assertThat(result).containsExactlyInAnyOrder(
        new JsonFeatureOutlineNodes(FEATURE_1.getId().toString(), feature1Nodes),
        new JsonFeatureOutlineNodes(FEATURE_2.getId().toString(), feature2Nodes)
    );
  }

  private static List<Line> ringLines(Polygon polygon, int ringNumber, int fromDisplayOrder, int toDisplayOrder) {
    var lines = new ArrayList<Line>();
    for (int displayOrder = fromDisplayOrder; displayOrder <= toDisplayOrder; displayOrder++) {
      lines.add(LineTestUtil.newBuilder()
          .withPolygon(polygon)
          .withRingNumber(ringNumber)
          .withDisplayOrder(displayOrder)
          .build());
    }
    return lines;
  }

  private static List<LineWithStartEndPoints> getTestStartAndEndPoints(List<Line> allLines) {
    return allLines
        .stream()
        .map(line -> new LineWithStartEndPoints(
            line,
            new Point(line.getDisplayOrder(), 0),
            new Point(line.getDisplayOrder(), 100)
        ))
        .toList();
  }

  @Test
  void deleteAll() {
    lineService.deleteAll();
    verify(lineRepository).deleteAll();
  }
}
