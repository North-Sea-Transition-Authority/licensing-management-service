package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Repository
public interface LicenceCorrectionRepository extends JpaRepository<LicenceCorrection, UUID>, NotDuplicationSource {

  boolean existsByLicenceAndStatus(Licence licence, LicenceCorrectionStatus status);

  List<LicenceCorrection> findAllByStatusAndAllocatedToWuaId(LicenceCorrectionStatus status, long wuaId);

  List<LicenceCorrection> findAllByLicence(Licence licence);

  @EntityGraph(attributePaths = "licence")
  List<LicenceCorrection> findAllByStatusIn(Collection<LicenceCorrectionStatus> statuses);

  Optional<LicenceCorrection> findByIdAndAllocatedToWuaId(UUID id, long wuaId);

  Optional<LicenceCorrection> findByLicenceAndStatus(Licence licence, LicenceCorrectionStatus status);
}
