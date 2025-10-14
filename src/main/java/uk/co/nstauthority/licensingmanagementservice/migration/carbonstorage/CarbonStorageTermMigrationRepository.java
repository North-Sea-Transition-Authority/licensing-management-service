package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarbonStorageTermMigrationRepository
    extends CrudRepository<CarbonStorageTermMigrationExtract, Integer> {
}
