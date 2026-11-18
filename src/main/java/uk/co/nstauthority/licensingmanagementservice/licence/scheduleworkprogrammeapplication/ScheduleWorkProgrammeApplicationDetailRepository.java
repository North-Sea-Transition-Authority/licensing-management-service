package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.LatestApplicationDetailSummary;

@Repository
public interface ScheduleWorkProgrammeApplicationDetailRepository
    extends JpaRepository<ScheduleWorkProgrammeApplicationDetail, UUID>, NotDuplicationSource {

  Optional<ScheduleWorkProgrammeApplicationDetail> getFirstByApplicationOrderByVersionNumberDesc(
      ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication);

  Optional<ScheduleWorkProgrammeApplicationDetail>
      findFirstByApplicationAndStatusNotOrderByVersionNumberDesc(
      ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication,
      ApplicationStatus status
  );

  boolean existsByApplicationAndVersionNumberGreaterThanAndStatusNot(
      ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication,
      Integer versionNumber,
      ApplicationStatus status
  );

  int countByVersionNumberAndSubmittedDatetimeBetween(Integer versionNumber, Instant startOfYear, Instant endOfYear);

  // Selects a summary rather than the entities: loading ScheduleWorkProgrammeApplicationDetail and its application
  // would eagerly load a deep chain of schedule associations with a separate query per application.
  @Query("""
      SELECT new uk.co.nstauthority.licensingmanagementservice.licence.application.LatestApplicationDetailSummary(
          application.id, d.id, d.status, application.applicationReference
      )
      FROM schedule_work_programme_application_details d
      JOIN d.application application
      WHERE application.id IN :scheduleWorkProgrammeApplicationIds
      AND d.versionNumber = (
          SELECT MAX(latest.versionNumber) FROM schedule_work_programme_application_details latest
          WHERE latest.application = application
      )
      """)
  List<LatestApplicationDetailSummary> findLatestSummariesByScheduleWorkProgrammeApplication_IdIn(
      Collection<UUID> scheduleWorkProgrammeApplicationIds);

  // applicationData is reached through schedule.application because Hibernate rejects a second TREAT of the same alias
  @Query("""
      SELECT d FROM schedule_work_programme_application_details d
      JOIN FETCH d.application application
      JOIN FETCH application.licence
      LEFT JOIN FETCH TREAT(application AS ScheduleWorkProgrammeApplication).schedule schedule
      LEFT JOIN FETCH schedule.licenceSchedule
      LEFT JOIN FETCH schedule.submittedLicenceScheduleDetail
      LEFT JOIN FETCH schedule.application scheduleApplication
      LEFT JOIN FETCH TREAT(scheduleApplication AS ScheduleWorkProgrammeApplication).applicationData
      WHERE d.status IN :statuses
      """)
  List<ScheduleWorkProgrammeApplicationDetail> findAllByStatusIn(Set<ApplicationStatus> statuses);
}