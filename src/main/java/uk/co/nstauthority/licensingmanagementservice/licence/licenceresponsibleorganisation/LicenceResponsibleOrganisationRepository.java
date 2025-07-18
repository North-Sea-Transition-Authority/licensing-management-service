package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Repository
public interface LicenceResponsibleOrganisationRepository extends JpaRepository<LicenceResponsibleOrganisation, UUID> {

  List<LicenceResponsibleOrganisation> findAllByManagedByLmsIsFalse();

  List<LicenceResponsibleOrganisation> findAllByLicence(Licence licence);

  List<LicenceResponsibleOrganisation> findAllByLicenceIn(Collection<Licence> licences);
}
