package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenceContinuationApplicationDetailRepository
    extends JpaRepository<LicenceContinuationApplicationDetail, UUID> {

  List<LicenceContinuationApplicationDetail> findAllByStatus(LicenceContinuationApplicationStatus status);

  List<LicenceContinuationApplicationDetail> findAllByStatusIn(Set<LicenceContinuationApplicationStatus> statuses);

  Optional<LicenceContinuationApplicationDetail> findFirstByLicenceContinuationApplicationOrderByVersionNumberDesc(
      LicenceContinuationApplication scheduleWorkProgrammeApplication
  );
}