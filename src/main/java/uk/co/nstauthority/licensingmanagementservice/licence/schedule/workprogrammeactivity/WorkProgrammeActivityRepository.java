package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicateThisOnUpdate;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Repository
public interface WorkProgrammeActivityRepository
    extends JpaRepository<WorkProgrammeActivity, UUID>, DuplicationSource<LicenceScheduleDetail> {

  @DuplicateThisOnUpdate
  List<WorkProgrammeActivity> findAllByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail);

  List<WorkProgrammeActivity> findAllByLicenceScheduleDetailAndStatus(
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleEventStatus status
  );

  List<WorkProgrammeActivity> findAllByLicenceScheduleTermAndDateOptionAndStatus(
      LicenceScheduleTerm licenceScheduleTerm,
      WorkProgrammeActivityDateOption dateOption,
      LicenceScheduleEventStatus status
  );

  List<WorkProgrammeActivity> findAllByLicenceSchedulePhaseAndDateOptionAndStatus(
      LicenceSchedulePhase licenceSchedulePhase,
      WorkProgrammeActivityDateOption dateOption,
      LicenceScheduleEventStatus status
  );

  List<WorkProgrammeActivity> findAllByLicenceScheduleDetailAndDueDateBetweenAndStatus(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate startDate,
      LocalDate endDate,
      LicenceScheduleEventStatus status
  );

  List<WorkProgrammeActivity> findAllByLicenceScheduleDetailAndDueDateAfterAndStatus(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate date,
      LicenceScheduleEventStatus licenceScheduleEventStatus
  );
}