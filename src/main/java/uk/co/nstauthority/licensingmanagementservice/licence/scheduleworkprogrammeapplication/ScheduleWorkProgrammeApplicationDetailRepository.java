package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.LatestApplicationDetailSummary;

@Repository
public interface ScheduleWorkProgrammeApplicationDetailRepository
    extends JpaRepository<ScheduleWorkProgrammeApplicationDetail, UUID>, NotDuplicationSource {

  Optional<ScheduleWorkProgrammeApplicationDetail> getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(
      ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication);

  int countByVersionNumberAndSubmittedDatetimeBetween(Integer versionNumber, Instant startOfYear, Instant endOfYear);

  @EntityGraph(attributePaths = "scheduleWorkProgrammeApplication")
  List<ScheduleWorkProgrammeApplicationDetail> findAllByScheduleWorkProgrammeApplication_IdIn(
      Collection<UUID> scheduleWorkProgrammeApplicationIds);

  // Selects a summary rather than the entities: loading ScheduleWorkProgrammeApplicationDetail and its application
  // would eagerly load a deep chain of schedule associations with a separate query per application.
  @Query("""
      SELECT new uk.co.nstauthority.licensingmanagementservice.licence.application.LatestApplicationDetailSummary(
          application.id, d.id, d.status, application.applicationReference
      )
      FROM schedule_work_programme_application_details d
      JOIN d.scheduleWorkProgrammeApplication application
      WHERE application.id IN :scheduleWorkProgrammeApplicationIds
      AND d.versionNumber = (
          SELECT MAX(latest.versionNumber) FROM schedule_work_programme_application_details latest
          WHERE latest.scheduleWorkProgrammeApplication = application
      )
      """)
  List<LatestApplicationDetailSummary> findLatestSummariesByScheduleWorkProgrammeApplication_IdIn(
      Collection<UUID> scheduleWorkProgrammeApplicationIds);

  @EntityGraph(attributePaths = "scheduleWorkProgrammeApplication.licenceSchedule.licence")
  List<ScheduleWorkProgrammeApplicationDetail> findAllByStatusIn(Set<ApplicationStatus> statuses);
}