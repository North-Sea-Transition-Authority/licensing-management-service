package uk.co.nstauthority.licensingmanagementservice.caseevent;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

@Repository
public interface CaseEventRepository extends JpaRepository<CaseEvent, UUID>, NotDuplicationSource {

  List<CaseEvent> findByApplicationTypeAndApplicationIdOrderByEventInstantDesc(
      ApplicationType applicationType,
      UUID applicationId
  );
}
