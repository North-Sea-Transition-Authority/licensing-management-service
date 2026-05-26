package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicateThisOnUpdate;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Repository
public interface LicenceSchedulePhaseRepository
    extends JpaRepository<LicenceSchedulePhase, UUID>, DuplicationSource<LicenceScheduleDetail> {

  @DuplicateThisOnUpdate
  List<LicenceSchedulePhase> findAllByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail);

  List<LicenceSchedulePhase> findAllByLicenceScheduleTerm(LicenceScheduleTerm licenceScheduleTerm);

  boolean existsByLicenceScheduleTermId(UUID id);

  Optional<LicenceSchedulePhase> findById(UUID id);

  Optional<LicenceSchedulePhase> findByLicenceScheduleDetailAndEventReference(
      LicenceScheduleDetail licenceScheduleDetail,
      EventReference eventReference
  );
}