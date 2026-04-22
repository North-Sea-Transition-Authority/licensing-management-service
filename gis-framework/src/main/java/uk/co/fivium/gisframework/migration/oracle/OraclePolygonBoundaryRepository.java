package uk.co.fivium.gisframework.migration.oracle;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Profile("gis-migration")
@Repository
interface OraclePolygonBoundaryRepository extends ListCrudRepository<OraclePolygonBoundary, Long> {
}
