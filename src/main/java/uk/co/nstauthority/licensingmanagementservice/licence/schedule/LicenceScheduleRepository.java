package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;

@Repository
public interface LicenceScheduleRepository extends JpaRepository<LicenceSchedule, UUID>, NotDuplicationSource {

  Optional<LicenceSchedule> findByLicence(Licence licence);

  List<LicenceSchedule> findAllByLicence_LicenceReferenceContainingIgnoreCaseAndLicence_Type(
      String searchTerm,
      LicenceType licenceType
  );
}
