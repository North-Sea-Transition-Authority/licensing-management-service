package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Repository
public interface RecordOfDecisionReductionRepository
    extends JpaRepository<RecordOfDecisionReduction, UUID>, NotDuplicationSource {

  List<RecordOfDecisionReduction> findAllByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail);

  Optional<RecordOfDecisionReduction> findByScheduleWorkProgrammeApplicationDetailAndLicenceSchedulePhaseId(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      UUID phaseId);

  Optional<RecordOfDecisionReduction> findByScheduleWorkProgrammeApplicationDetailAndLicenceScheduleTermId(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      UUID termId);
}
