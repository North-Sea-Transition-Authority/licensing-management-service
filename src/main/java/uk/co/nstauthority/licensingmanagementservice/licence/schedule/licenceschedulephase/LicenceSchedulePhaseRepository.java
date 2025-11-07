package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Repository
public interface LicenceSchedulePhaseRepository extends JpaRepository<LicenceSchedulePhase, UUID> {

  List<LicenceSchedulePhase> findByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail);

  List<LicenceSchedulePhase> findByLicenceScheduleTerm(LicenceScheduleTerm licenceScheduleTerm);
}
