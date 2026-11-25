package uk.co.nstauthority.licensingmanagementservice.licence.application.decision;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface ApplicationDecisionDetailRepository
    extends JpaRepository<ApplicationDecisionDetail, UUID>, NotDuplicationSource {

  Optional<ApplicationDecisionDetail> findByApplicationIdAndStatus(UUID applicationId, ApplicationDecisionStatus status);

  Optional<ApplicationDecisionDetail> findFirstByApplicationIdOrderByVersionNumberDesc(UUID applicationId);
}
