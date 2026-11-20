package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
interface PearsScheduleMigrationQueueEntryRepository
    extends JpaRepository<PearsScheduleMigrationQueueEntry, PearsScheduleMigrationQueueEntryId>, NotDuplicationSource {

  List<PearsScheduleMigrationQueueEntry> findAllByIdRunIdAndStatusOrderByIdLicenceReference(
      UUID runId, PearsScheduleMigrationQueueStatus status, Limit limit);

  List<PearsScheduleMigrationQueueEntry> findAllByIdRunIdAndStatusOrderByIdLicenceReference(
      UUID runId, PearsScheduleMigrationQueueStatus status);

  List<PearsScheduleMigrationQueueEntry> findAllByIdRunIdOrderByIdLicenceReference(UUID runId);

  long countByIdRunIdAndStatus(UUID runId, PearsScheduleMigrationQueueStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT e FROM PearsScheduleMigrationQueueEntry e WHERE e.id = :id")
  Optional<PearsScheduleMigrationQueueEntry> findByIdForUpdate(PearsScheduleMigrationQueueEntryId id);

  /**
   * Moves a run's licences to another status to be worked again, clearing what their last attempt left but keeping
   * the count of attempts.
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("""
      UPDATE PearsScheduleMigrationQueueEntry e
      SET e.status = :to, e.outcome = NULL, e.notes = NULL, e.startedAt = NULL, e.finishedAt = NULL
      WHERE e.id.runId = :runId AND e.status = :from
      """)
  int requeue(UUID runId, PearsScheduleMigrationQueueStatus from, PearsScheduleMigrationQueueStatus to);

  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM PearsScheduleMigrationQueueEntry e WHERE e.id.runId <> :runId")
  int deleteAllNotOfRun(UUID runId);
}
