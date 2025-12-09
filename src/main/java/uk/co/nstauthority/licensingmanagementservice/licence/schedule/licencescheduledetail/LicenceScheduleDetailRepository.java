package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;

@Repository
public interface LicenceScheduleDetailRepository extends JpaRepository<LicenceScheduleDetail, UUID> {

  Optional<LicenceScheduleDetail> findByLicenceSchedule_LicenceAndStatus(
      Licence licence,
      LicenceScheduleDetailStatus licenceScheduleDetailStatus
  );

  @EntityGraph(attributePaths = "licenceSchedule.licence")
  List<LicenceScheduleDetail> findAllByStatus(LicenceScheduleDetailStatus licenceScheduleDetailStatus);

  boolean existsByLicenceSchedule_LicenceAndStatus(Licence licence, LicenceScheduleDetailStatus licenceScheduleDetailStatus);

  @Query(
      """
        SELECT lsd FROM licence_schedule_details lsd
        WHERE lsd.status = (:status)
        AND upper(lsd.licenceSchedule.licence.licenceReference) LIKE '%' || upper(:searchTerm) || '%'
        AND lsd.licenceSchedule.licence.type IN (:licenceTypes)
        """
  )
  List<LicenceScheduleDetail> searchByLicenceReferenceLicenceTypesAndStatus(
      String searchTerm, List<LicenceType> licenceTypes, LicenceScheduleDetailStatus status);
}
