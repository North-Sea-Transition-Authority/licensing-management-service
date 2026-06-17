package uk.co.nstauthority.licensingmanagementservice.licence.position.transaction;

import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;

public class LicenceTransactionTestUtil {

  private UUID id =  UUID.randomUUID();
  private String regulatorReference = "REF-" + id.toString().substring(0, 8).toUpperCase();

  public static LicenceTransactionTestUtil newBuilder() {
    return new LicenceTransactionTestUtil();
  }

  public LicenceTransactionTestUtil withId(UUID id) {
    this.id = id;
    return this;
  }

  public LicenceTransactionTestUtil withRegulatorReference(String regulatorReference) {
    this.regulatorReference = regulatorReference;
    return this;
  }

  public LicenceTransaction build() {
    var licenceTransaction = new LicenceTransaction(id);
    licenceTransaction.setRegulatorReference(regulatorReference);

    return licenceTransaction;
  }
}
