package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface CarbonStorageWorkProgrammeMigrationRepository
    extends ListCrudRepository<CarbonStorageWorkProgrammeMigrationExtract, Integer>, NotDuplicationSource {

  @Query(value = """
      SELECT DISTINCT licence_ref AS licenceRef, case_date AS caseDate
      FROM lms.cs_work_programme_migration_extract
      """, nativeQuery = true)
  List<CsLicenceCase> findDistinctCases();
}
