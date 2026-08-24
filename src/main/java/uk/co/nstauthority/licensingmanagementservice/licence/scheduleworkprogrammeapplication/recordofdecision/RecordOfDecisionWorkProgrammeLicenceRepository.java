package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface RecordOfDecisionWorkProgrammeLicenceRepository
    extends JpaRepository<RecordOfDecisionWorkProgrammeLicence, UUID>, NotDuplicationSource {

  List<RecordOfDecisionWorkProgrammeLicence> findAllByRecordOfDecisionWorkProgramme(
      RecordOfDecisionWorkProgramme recordOfDecisionWorkProgramme);
}
