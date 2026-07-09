package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;

@Repository
public interface LicenceContactRepository extends JpaRepository<LicenceContact, UUID>, NotDuplicationSource {

  Optional<LicenceContact> findByLicensee(LicenceResponsibleOrganisation licensee);

  @EntityGraph(attributePaths = {"licensee", "licensee.licence"})
  List<LicenceContact> findAllByLicensee_ResponsibleOrganisationIdIn(Collection<Integer> responsibleOrganisationIds);

  List<LicenceContact> findAllByLicenseeIn(Collection<LicenceResponsibleOrganisation> licensees);
}
