package uk.co.fivium.gisframework.feature;

import java.util.Collection;
import java.util.List;
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

  @Transactional
  public void deleteAll() {
    lineRepository.deleteAll();
  }
}
