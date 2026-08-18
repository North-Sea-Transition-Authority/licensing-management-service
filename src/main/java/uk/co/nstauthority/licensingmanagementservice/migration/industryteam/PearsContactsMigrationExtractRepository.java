package uk.co.nstauthority.licensingmanagementservice.migration.industryteam;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface PearsContactsMigrationExtractRepository
    extends ListCrudRepository<PearsContactsMigrationExtract, PearsContactsMigrationExtractCompositeKey>,
    NotDuplicationSource {
}
