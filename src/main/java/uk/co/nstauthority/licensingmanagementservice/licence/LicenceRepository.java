package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenceRepository extends JpaRepository<Licence, Integer> {

  Optional<Licence> findTopByOrderByIdDesc();

  List<Licence> findAllByLicenceReferenceContainingIgnoreCase(String licenceReference);

  List<Licence> findAllByLicenceReferenceContainingIgnoreCaseAndType(String licenceReference, LicenceType type);

  boolean existsByTypeAndLicenceNumber(LicenceType type, String licenceNumber);
}
