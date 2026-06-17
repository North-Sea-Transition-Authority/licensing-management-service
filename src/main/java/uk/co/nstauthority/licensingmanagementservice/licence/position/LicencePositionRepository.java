package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Repository
public interface LicencePositionRepository extends JpaRepository<LicencePosition, UUID>, NotDuplicationSource {

  List<LicencePosition> findByLicence(Licence licence);

  @Query("""
      SELECT MAX(lp.positionDateOrder)
      FROM licence_positions lp
      WHERE lp.licence = :licence AND lp.positionDate = :positionDate
      """)
  Integer findMaxPositionDateOrder(Licence licence, LocalDate positionDate);
}
