package uk.co.nstauthority.licensingmanagementservice.licence.transaction;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LicenceTransactionService {

  private final LicenceTransactionRepository licenceTransactionRepository;
  private final EntityManager entityManager;

  public LicenceTransactionService(
      LicenceTransactionRepository licenceTransactionRepository,
      EntityManager entityManager
  ) {
    this.licenceTransactionRepository = licenceTransactionRepository;
    this.entityManager = entityManager;
  }

  @Transactional
  public LicenceTransaction createLicenceTransaction(String regulatorReference) {
    var licenceTransaction = new LicenceTransaction();
    licenceTransaction.setRegulatorReference(regulatorReference);

    return licenceTransactionRepository.save(licenceTransaction);
  }

  @Transactional
  public LicenceTransaction createLicenceTransaction(UUID licenceTransactionId, String regulatorReference) {
    var licenceTransaction = new LicenceTransaction(licenceTransactionId);
    licenceTransaction.setRegulatorReference(regulatorReference);

    entityManager.persist(licenceTransaction);

    return licenceTransaction;
  }
}
