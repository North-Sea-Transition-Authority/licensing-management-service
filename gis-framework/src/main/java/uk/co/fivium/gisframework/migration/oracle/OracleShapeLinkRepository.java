package uk.co.fivium.gisframework.migration.oracle;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Profile("gis-migration")
@Repository
interface OracleShapeLinkRepository extends ListCrudRepository<OracleShapeLink, OracleShapeLinkCompositeKey> {

  Optional<OracleShapeLink> findByChildShapeId(Integer childShapeShapeSiId);
}
