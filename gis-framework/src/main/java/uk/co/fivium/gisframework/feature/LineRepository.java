package uk.co.fivium.gisframework.feature;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
interface LineRepository extends ListCrudRepository<Line, UUID> {

  List<Line> findAllByPolygon_FeatureIn(Collection<Feature> features);

  List<Line> findAllByPolygon_Feature_LegacyIdIn(Collection<Integer> legacyFeatureId);

  List<Line> findAllByPolygon_Feature(Feature feature);

  List<Line> findAllByPolygonIn(Collection<Polygon> polygons);

  void deleteAllByPolygon_FeatureIn(Collection<Feature> features);
}
