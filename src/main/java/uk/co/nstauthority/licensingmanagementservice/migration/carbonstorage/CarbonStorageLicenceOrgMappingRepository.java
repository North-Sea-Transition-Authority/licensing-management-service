package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface CarbonStorageLicenceOrgMappingRepository
    extends CrudRepository<CarbonStorageLicenceOrgMapping, String>, NotDuplicationSource {

  CarbonStorageLicenceOrgMapping findByCsExtractResponsibleOrganisation(String csExtractResponsibleOrganisation);
}
