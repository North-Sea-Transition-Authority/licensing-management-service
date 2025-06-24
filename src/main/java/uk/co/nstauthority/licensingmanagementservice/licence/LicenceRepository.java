package uk.co.nstauthority.licensingmanagementservice.licence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenceRepository extends CrudRepository<Licence, Integer> {
}
