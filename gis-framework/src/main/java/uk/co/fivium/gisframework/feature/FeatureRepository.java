package uk.co.fivium.gisframework.feature;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
interface FeatureRepository extends ListCrudRepository<Feature, UUID> {

  List<Feature> findAllByParentFeatureId(UUID parentFeatureId);
}
