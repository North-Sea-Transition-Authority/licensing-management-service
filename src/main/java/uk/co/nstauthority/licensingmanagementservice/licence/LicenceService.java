package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.transaction.Transactional;
import java.util.Collection;
import org.springframework.stereotype.Service;

@Service
public class LicenceService {

  private final LicenceRepository licenceRepository;

  public LicenceService(LicenceRepository licenceRepository) {
    this.licenceRepository = licenceRepository;
  }

  @Transactional
  public Iterable<Licence> saveLicences(Collection<Licence> licences) {
    return licenceRepository.saveAll(licences);
  }

}
