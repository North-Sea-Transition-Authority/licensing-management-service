package uk.co.nstauthority.licensingmanagementservice.licence.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LicenceTransactionService {

  private final LicenceTransactionRepository licenceTransactionRepository;

  public LicenceTransactionService(LicenceTransactionRepository licenceTransactionRepository) {
    this.licenceTransactionRepository = licenceTransactionRepository;
  }

  @Transactional
  public LicenceTransaction createLicenceTransaction(String regulatorReference) {
    var licenceTransaction = new LicenceTransaction();
    licenceTransaction.setRegulatorReference(regulatorReference);

    return licenceTransactionRepository.save(licenceTransaction);
  }
}
