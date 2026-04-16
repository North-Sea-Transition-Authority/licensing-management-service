package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Repository
public interface LicenceScheduleTermRepository
    extends JpaRepository<LicenceScheduleTerm, UUID>, DuplicationSource<LicenceScheduleDetail> {

  List<LicenceScheduleTerm> findByLicenceScheduleDetailAndStatus(
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleEventStatus status
  );
}
