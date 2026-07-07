package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

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
public interface LicenceResponsibleOrganisationRepository
    extends JpaRepository<LicenceResponsibleOrganisation, UUID>, NotDuplicationSource {

  List<LicenceResponsibleOrganisation> findAllByManagedByLmsIsFalse();

  List<LicenceResponsibleOrganisation> findAllByLicence(Licence licence);

  @EntityGraph(attributePaths = "licence")
  List<LicenceResponsibleOrganisation> findAllByLicenceIn(Collection<Licence> licences);

  @EntityGraph(attributePaths = "licence")
  List<LicenceResponsibleOrganisation> findAllByResponsibleOrganisationIdIn(Collection<Integer> responsibleOrganisationIds);

  Optional<LicenceResponsibleOrganisation> findByLicence_IdAndResponsibleOrganisationId(
      Integer licenceId,
      Integer responsibleOrganisationId
  );
}
