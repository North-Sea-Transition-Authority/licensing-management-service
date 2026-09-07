package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;

@Repository
public interface LicencePositionRepository extends JpaRepository<LicencePosition, UUID>, NotDuplicationSource {

  @EntityGraph("licencePosition")
  List<LicencePosition> findByLicence(Licence licence);

  List<LicencePosition> findByLicenceTransactionIn(Collection<LicenceTransaction> licenceTransactions);

  @Query("""
      SELECT MAX(lp.positionDateOrder)
      FROM licence_positions lp
      WHERE lp.licence = :licence AND lp.positionDate = :positionDate
      """)
  Integer findMaxPositionDateOrder(Licence licence, LocalDate positionDate);

  @EntityGraph("licencePosition")
  Optional<LicencePosition> findByIdAndLicence(UUID id, Licence licence);
}
