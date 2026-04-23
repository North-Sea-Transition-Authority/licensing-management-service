package uk.co.fivium.gisframework.migration.oracle;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Profile("gis-migration")
@Repository
interface OracleShapePolygonRepository extends ListCrudRepository<OracleShapePolygon, Integer> {

  List<OracleShapePolygon> findAllByShapeSidId(Integer shapeSidId);
}