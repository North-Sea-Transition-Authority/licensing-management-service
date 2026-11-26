package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface PearsWorkProgrammeMigratedLicenceRepository
    extends JpaRepository<PearsWorkProgrammeMigratedLicence, UUID>, NotDuplicationSource {

  @Query(value = """
      SELECT l.licence_reference
      FROM lms.pears_work_programme_migrated_licences m
      JOIN lms.licences l ON l.id = m.licence_id
      ORDER BY l.licence_reference
      """, nativeQuery = true)
  List<String> findAllLicenceReferences();

  @Query(value = """
      SELECT m.*
      FROM lms.pears_work_programme_migrated_licences m
      JOIN lms.licences l ON l.id = m.licence_id
      WHERE l.licence_reference = :licenceReference
      """, nativeQuery = true)
  Optional<PearsWorkProgrammeMigratedLicence> findByLicenceReference(String licenceReference);
}
