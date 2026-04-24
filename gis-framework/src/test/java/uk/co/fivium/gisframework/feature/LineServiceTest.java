package uk.co.fivium.gisframework.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LineServiceTest {

  private static final Feature FEATURE_1 = FeatureTestUtil.newBuilder().build();
  private static final Feature FEATURE_2 = FeatureTestUtil.newBuilder().build();
  private static final List<Feature> FEATURES = List.of(FEATURE_1, FEATURE_2);

  private static final Line LINE_1 = LineTestUtil.newBuilder().build();
  private static final Line LINE_2 = LineTestUtil.newBuilder().build();

  @Mock
  private LineRepository lineRepository;

  @InjectMocks
  private LineService lineService;

  @Test
  void saveLine() {
    lineService.saveLine(LINE_1);

    verify(lineRepository).save(LINE_1);
  }

  @Test
  void saveLines() {
    lineService.saveLines(List.of(LINE_1, LINE_2));
    verify(lineRepository).saveAll(List.of(LINE_1, LINE_2));
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
  void findAllByPolygon() {
    var polygon = PolygonTestUtil.newBuilder().build();

    when(lineRepository.findAllByPolygon(polygon)).thenReturn(List.of(LINE_1, LINE_2));

    var result = lineService.findAllByPolygon(polygon);

    var expected = List.of(LINE_1, LINE_2);
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void deleteAll() {
    lineService.deleteAll();
    verify(lineRepository).deleteAll();
  }
}
