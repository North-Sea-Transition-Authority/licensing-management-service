package uk.co.fivium.gisframework.feature;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
interface FeatureRepository extends ListCrudRepository<Feature, UUID> {

  List<Feature> findAllByParentFeatureId(UUID parentFeatureId);

  Optional<Feature> findByLegacyId(Integer legacyId);

  List<Feature> findAllByLegacyIdIn(Collection<Integer> legacyIds);

  void deleteAllByParentFeatureIsNotNull();

  List<Feature> findAllByParentFeatureIsNotNull();

  @Query(value = "SELECT * FROM lms.gis_framework_features WHERE attributes ->> ?1 = ?2", nativeQuery = true)
  List<Feature> findAllByAttribute(String key, String value);

  @Query(value = "SELECT * FROM lms.gis_framework_features WHERE attributes ->> ?1 IN (?2)", nativeQuery = true)
  List<Feature> findAllByAttributeValueIn(String key, Collection<String> values);
}
