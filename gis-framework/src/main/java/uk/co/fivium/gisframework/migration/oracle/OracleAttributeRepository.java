package uk.co.fivium.gisframework.migration.oracle;

import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Profile("gis-migration")
@Repository
interface OracleAttributeRepository extends ListCrudRepository<OracleAttribute, OracleAttributeCompositeKey> {

  List<OracleAttribute> findAllByAssociatedSiIdAndAttributeLevel(Integer associatedSiId, AttributeLevel attributeLevel);

  List<OracleAttribute> findAllByAssociatedSiIdInAndAttributeLevel(
      Collection<Integer> associatedSiId,
      AttributeLevel attributeLevel
  );
}
