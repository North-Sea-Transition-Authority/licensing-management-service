package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarbonStorageLicenceOrgMappingRepository extends CrudRepository<CarbonStorageLicenceOrgMapping, String> {

  CarbonStorageLicenceOrgMapping findByCsExtractResponsibleOrganisation(String csExtractResponsibleOrganisation);
}
