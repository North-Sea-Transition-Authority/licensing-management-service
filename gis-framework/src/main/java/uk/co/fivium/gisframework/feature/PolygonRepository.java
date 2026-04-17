package uk.co.fivium.gisframework.feature;

import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
interface PolygonRepository extends ListCrudRepository<Polygon, UUID> {

}
