package uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicateThisOnUpdate;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Repository
public interface LicenceScheduleAreaEventRepository
    extends JpaRepository<LicenceScheduleAreaEvent, UUID>, DuplicationSource<LicenceScheduleDetail> {

  @DuplicateThisOnUpdate
  List<LicenceScheduleAreaEvent> findAllByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail);
}
