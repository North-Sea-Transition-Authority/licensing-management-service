package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Repository
public interface OtherScheduleEventRepository extends JpaRepository<OtherScheduleEvent, UUID> {
  List<OtherScheduleEvent> findAllByLicenceScheduleTermAndDateOptionAndStatus(
      LicenceScheduleTerm licenceScheduleTerm,
      OtherScheduleEventDateOption dateOption,
      LicenceScheduleEventStatus licenceScheduleEventStatus
  );

  List<OtherScheduleEvent> findAllByLicenceSchedulePhaseAndDateOptionAndStatus(
      LicenceSchedulePhase licenceSchedulePhase,
      OtherScheduleEventDateOption dateOption,
      LicenceScheduleEventStatus licenceScheduleEventStatus
  );

  List<OtherScheduleEvent> findAllByLicenceScheduleDetailAndEventDateBetweenAndStatus(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate from,
      LocalDate to,
      LicenceScheduleEventStatus licenceScheduleEventStatus
  );
}