package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicateThisOnUpdate;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Repository
public interface WorkProgrammeActivityRepository
    extends JpaRepository<WorkProgrammeActivity, UUID>, DuplicationSource<LicenceScheduleDetail> {

  @DuplicateThisOnUpdate
  List<WorkProgrammeActivity> findAllByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail);

  List<WorkProgrammeActivity> findAllByLicenceScheduleTermAndDateOption(
      LicenceScheduleTerm licenceScheduleTerm,
      WorkProgrammeActivityDateOption dateOption
  );

  List<WorkProgrammeActivity> findAllByLicenceSchedulePhaseAndDateOption(
      LicenceSchedulePhase licenceSchedulePhase,
      WorkProgrammeActivityDateOption dateOption
  );

  List<WorkProgrammeActivity> findAllByLicenceScheduleDetailAndDueDateBetween(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate startDate,
      LocalDate endDate
  );

  List<WorkProgrammeActivity> findAllByLicenceScheduleDetailAndDueDateAfter(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate date
  );

  @EntityGraph(attributePaths = {"licenceSchedulePhase.licenceScheduleTerm", "licenceScheduleTerm"})
  List<WorkProgrammeActivity> findByLicenceSchedulePhase(LicenceSchedulePhase licenceSchedulePhase);

  @EntityGraph(attributePaths = {"licenceSchedulePhase.licenceScheduleTerm", "licenceScheduleTerm"})
  List<WorkProgrammeActivity> findByLicenceScheduleTerm(LicenceScheduleTerm licenceScheduleTerm);

  boolean existsByLicenceSchedulePhase(LicenceSchedulePhase licenceSchedulePhase);

  boolean existsByLicenceScheduleTerm(LicenceScheduleTerm licenceScheduleTerm);

  Optional<WorkProgrammeActivity> findByLicenceScheduleDetailAndEventReference(
      LicenceScheduleDetail licenceScheduleDetail,
      EventReference eventReference
  );
}
