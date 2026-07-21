package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;

@Repository
public interface LicenceContinuationApplicationDetailRepository
    extends JpaRepository<LicenceContinuationApplicationDetail, UUID>, NotDuplicationSource {

  List<LicenceContinuationApplicationDetail> findAllByStatus(ApplicationStatus status);

  List<LicenceContinuationApplicationDetail> findAllByStatusIn(Set<ApplicationStatus> statuses);

  Optional<LicenceContinuationApplicationDetail> findFirstByLicenceContinuationApplicationOrderByVersionNumberDesc(
      LicenceContinuationApplication scheduleWorkProgrammeApplication
  );

  int countByVersionNumberAndSubmittedDatetimeBetween(Integer versionNumber, Instant startOfYear, Instant endOfYear);
}