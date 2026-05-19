package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicateThisOnUpdate;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Repository
public interface OtherScheduleEventRepository
    extends JpaRepository<OtherScheduleEvent, UUID>, DuplicationSource<LicenceScheduleDetail> {

  @DuplicateThisOnUpdate
  List<OtherScheduleEvent> findAllByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail);

  List<OtherScheduleEvent> findAllByLicenceScheduleTermAndDateOption(
      LicenceScheduleTerm licenceScheduleTerm,
      OtherScheduleEventDateOption dateOption
  );

  List<OtherScheduleEvent> findAllByLicenceSchedulePhaseAndDateOption(
      LicenceSchedulePhase licenceSchedulePhase,
      OtherScheduleEventDateOption dateOption
  );

  List<OtherScheduleEvent> findAllByLicenceScheduleDetailAndEventDateBetween(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate from,
      LocalDate to
  );

  List<OtherScheduleEvent> findAllByLicenceScheduleDetailAndEventDateAfter(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate date
  );
}
