package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface LicenceRepository extends JpaRepository<Licence, Integer>, NotDuplicationSource {

  Optional<Licence> findTopByOrderByIdDesc();

  List<Licence> findAllByLicenceReferenceContainingIgnoreCaseAndTypeIn(
      String licenceReference,
      List<LicenceType> licenceTypes
  );

  boolean existsByTypeAndLicenceNumber(
      LicenceType type,
      String licenceNumber
  );

  Optional<Licence> findByLicenceReference(String licenceReference);
}
