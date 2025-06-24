package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenceResponsibleOrganisationRepository extends JpaRepository<LicenceResponsibleOrganisation, UUID> {

  List<LicenceResponsibleOrganisation> findAllByManagedByLmsIsFalse();

}
