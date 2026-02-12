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

  boolean existsByLicenceSchedule_LicenceAndStatusIn(
      Licence licence,
      List<LicenceScheduleDetailStatus> licenceScheduleDetailStatus
  );

  @Query("""
      FROM LicenceScheduleDetail lsd
      JOIN LicenceStartDate sd ON sd.licenceScheduleDetail = lsd
      WHERE lsd.status = :status
      AND UPPER(lsd.licenceSchedule.licence.licenceReference) LIKE UPPER(CONCAT('%', :searchTerm, '%'))
      AND lsd.licenceSchedule.licence.type IN :licenceTypes
      AND sd.startDate <= CURRENT_TIMESTAMP
      """
  )
  List<LicenceScheduleDetail> searchByLicenceReferenceLicenceTypesAndStatus(
      String searchTerm, List<LicenceType> licenceTypes, LicenceScheduleDetailStatus status);
}
