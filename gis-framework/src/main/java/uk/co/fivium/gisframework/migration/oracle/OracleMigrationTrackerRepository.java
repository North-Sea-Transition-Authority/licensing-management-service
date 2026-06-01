package uk.co.fivium.gisframework.migration.oracle;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Profile("gis-migration")
@Repository
interface OracleMigrationTrackerRepository extends ListCrudRepository<OracleMigrationTracker, Integer> {

  List<OracleMigrationTracker> findAllByOrderNumber(Integer orderNumber);
}
