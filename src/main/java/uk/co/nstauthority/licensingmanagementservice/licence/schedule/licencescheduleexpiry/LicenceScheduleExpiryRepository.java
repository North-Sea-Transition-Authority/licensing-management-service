package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Repository
public interface LicenceScheduleExpiryRepository extends JpaRepository<LicenceScheduleExpiry, UUID> {

  List<LicenceScheduleExpiry> findAllByLicenceScheduleDetailAndStatus(
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleEventStatus status
  );

  List<LicenceScheduleExpiry> findAllByLicenceScheduleDetailAndStatusAndExpiryDateBetween(
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleEventStatus status,
      LocalDate from,
      LocalDate to
  );

}
