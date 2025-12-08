package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Repository
public interface WorkProgrammeActivityRepository extends JpaRepository<WorkProgrammeActivity, UUID> {

  List<WorkProgrammeActivity> findAllByLicenceScheduleDetail(
      LicenceScheduleDetail licenceScheduleDetail
  );

  List<WorkProgrammeActivity> findAllByLicenceScheduleTermAndDateOption(
      LicenceScheduleTerm licenceScheduleTerm,
      WorkProgrammeActivityDateOption dateOption
  );

  List<WorkProgrammeActivity> findAllByLicenceSchedulePhaseAndDateOption(
      LicenceSchedulePhase licenceSchedulePhase,
      WorkProgrammeActivityDateOption dateOption
  );
}