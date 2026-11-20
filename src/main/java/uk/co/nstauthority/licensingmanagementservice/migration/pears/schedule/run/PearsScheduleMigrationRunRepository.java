package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
interface PearsScheduleMigrationRunRepository
    extends JpaRepository<PearsScheduleMigrationRun, UUID>, NotDuplicationSource {

  Optional<PearsScheduleMigrationRun> findFirstByStatusInOrderByRequestedAt(
      Collection<PearsScheduleMigrationRunStatus> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT r FROM PearsScheduleMigrationRun r
      WHERE r.status IN :statuses
      ORDER BY r.requestedAt
      LIMIT 1
      """)
  Optional<PearsScheduleMigrationRun> findOldestByStatusInForUpdate(
      Collection<PearsScheduleMigrationRunStatus> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM PearsScheduleMigrationRun r WHERE r.id = :id")
  Optional<PearsScheduleMigrationRun> findByIdForUpdate(UUID id);

  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM PearsScheduleMigrationRun r WHERE r.id <> :id")
  int deleteAllExcept(UUID id);
}
