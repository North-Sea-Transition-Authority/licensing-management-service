package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface WorkProgrammeActivityStatusRepository
    extends JpaRepository<WorkProgrammeActivityStatus, UUID>, NotDuplicationSource {

  List<WorkProgrammeActivityStatus> findAllByScheduleEvent_OriginalEventId(UUID originalEventId);

  @EntityGraph(attributePaths = "scheduleEvent")
  List<WorkProgrammeActivityStatus> findAllByScheduleEvent_OriginalEventIdIn(Collection<UUID> originalEventIds);

  @Query("""
      SELECT activityStatus.scheduleEvent.originalEventId
      FROM work_programme_activity_statuses activityStatus
      WHERE activityStatus.scheduleEvent.originalEventId IN :originalEventIds
      AND activityStatus.status IN :statuses
      AND activityStatus.appliedDatetime = (
        SELECT MAX(latestStatus.appliedDatetime)
        FROM work_programme_activity_statuses latestStatus
        WHERE latestStatus.scheduleEvent.originalEventId = activityStatus.scheduleEvent.originalEventId
      )
      """
  )
  Set<UUID> findOriginalEventIdsWithLatestStatusIn(
      Collection<UUID> originalEventIds,
      Collection<WorkProgrammeStatus> statuses
  );
}
