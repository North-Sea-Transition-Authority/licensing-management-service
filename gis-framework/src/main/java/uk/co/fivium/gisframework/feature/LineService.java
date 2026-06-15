package uk.co.fivium.gisframework.feature;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LineService {

  private final LineRepository lineRepository;

  public LineService(LineRepository lineRepository) {
    this.lineRepository = lineRepository;
  }

  @Transactional
  public void saveLine(Line line) {
    lineRepository.save(line);
  }

  @Transactional
  public void saveLines(Collection<Line> lines) {
    lineRepository.saveAll(lines);
  }

  public List<Line> findAllByFeatureIn(Collection<Feature> features) {
    return lineRepository.findAllByPolygon_FeatureIn(features);
  }

  public List<Line> findAllByFeatureLegacyIdIn(Collection<Integer> featureLegacyIds) {
    return lineRepository.findAllByPolygon_Feature_LegacyIdIn(featureLegacyIds);
  }

  public List<Line> getLines(List<Polygon> polygons) {
    return lineRepository.findAllByPolygonIn(polygons);
  }

  public Map<Polygon, List<Line>> getPolygonToLines(Feature feature) {
    return lineRepository.findAllByPolygon_Feature(feature)
        .stream()
        .collect(Collectors.groupingBy(Line::getPolygon));
  }

  public Map<Polygon, List<Line>> getPolygonToLinesIn(Collection<Feature> features) {
    return lineRepository.findAllByPolygon_FeatureIn(features)
        .stream()
        .collect(Collectors.groupingBy(Line::getPolygon));
  }

  public List<Line> findAllByPolygon(Polygon polygon) {
    return lineRepository.findAllByPolygon(polygon);
  }

  @Transactional
  public void deleteAll() {
    lineRepository.deleteAll();
  }
}
