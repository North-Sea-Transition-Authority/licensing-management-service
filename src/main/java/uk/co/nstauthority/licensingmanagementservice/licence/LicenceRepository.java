package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenceRepository extends JpaRepository<Licence, Integer> {

  Optional<Licence> findTopByOrderByIdDesc();
}
