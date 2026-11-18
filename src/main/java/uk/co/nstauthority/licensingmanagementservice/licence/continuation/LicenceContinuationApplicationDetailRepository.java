package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

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
public interface LicenceContinuationApplicationDetailRepository
    extends JpaRepository<LicenceContinuationApplicationDetail, UUID>, NotDuplicationSource {

  List<LicenceContinuationApplicationDetail> findAllByStatus(ApplicationStatus status);

  @Query("""
      SELECT d FROM licence_continuation_application_details d
      JOIN FETCH d.application application
      JOIN FETCH application.licence
      LEFT JOIN FETCH TREAT(application AS LicenceContinuationApplication).schedule schedule
      LEFT JOIN FETCH schedule.licenceSchedule
      LEFT JOIN FETCH schedule.submittedLicenceScheduleDetail
      LEFT JOIN FETCH d.currentTerm
      LEFT JOIN FETCH d.currentPhase
      LEFT JOIN FETCH d.nextTerm
      LEFT JOIN FETCH d.nextPhase
      WHERE d.status IN :statuses
      """)
  List<LicenceContinuationApplicationDetail> findAllByStatusIn(Set<ApplicationStatus> statuses);

  Optional<LicenceContinuationApplicationDetail> findFirstByApplicationOrderByVersionNumberDesc(
      LicenceContinuationApplication scheduleWorkProgrammeApplication
  );

  List<LicenceContinuationApplicationDetail> findAllByApplication(
      LicenceContinuationApplication licenceContinuationApplication
  );

  boolean existsByApplicationAndVersionNumberGreaterThanAndStatusNot(
      LicenceContinuationApplication licenceContinuationApplication,
      Integer versionNumber,
      ApplicationStatus status
  );

  int countByVersionNumberAndSubmittedDatetimeBetween(Integer versionNumber, Instant startOfYear, Instant endOfYear);

  // Selects a summary rather than the entities: loading LicenceContinuationApplicationDetail would eagerly load its
  // current/next term and phase, the application and their schedule associations with a separate query each.
  @Query("""
      SELECT new uk.co.nstauthority.licensingmanagementservice.licence.application.LatestApplicationDetailSummary(
          application.id, d.id, d.status, application.applicationReference
      )
      FROM licence_continuation_application_details d
      JOIN d.application application
      WHERE application.id IN :licenceContinuationApplicationIds
      AND d.versionNumber = (
          SELECT MAX(latest.versionNumber) FROM licence_continuation_application_details latest
          WHERE latest.application = application
      )
      """)
  List<LatestApplicationDetailSummary> findLatestSummariesByLicenceContinuationApplication_IdIn(
      Collection<UUID> licenceContinuationApplicationIds);
}
