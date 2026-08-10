package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;

public class LicencePositionTestUtil {

  private UUID id = UUID.randomUUID();
  private Licence licence = LicenceTestUtil.builder().build();
  private LicenceTransaction transaction = LicenceTransactionTestUtil.newBuilder().build();
  private LocalDate positionDate = LocalDate.of(2026, 1, 1);
  private int positionOrder = 1;
  private boolean isExecuted = true;
  private Set<UUID> featureIds = Set.of();

  public static LicencePositionTestUtil newBuilder() {
    return new LicencePositionTestUtil();
  }

  public LicencePositionTestUtil withId(UUID id) {
    this.id = id;
    return this;
  }

  public LicencePositionTestUtil withLicence(Licence licence) {
    this.licence = licence;
    return this;
  }

  public LicencePositionTestUtil withLicenceTransaction(LicenceTransaction transaction) {
    this.transaction = transaction;
    return this;
  }

  public LicencePositionTestUtil withPositionDate(LocalDate positionDate) {
    this.positionDate = positionDate;
    return this;
  }

  public LicencePositionTestUtil withPositionOrder(int positionOrder) {
    this.positionOrder = positionOrder;
    return this;
  }

  public LicencePositionTestUtil withIsExecuted(boolean isExecuted) {
    this.isExecuted = isExecuted;
    return this;
  }

  public LicencePositionTestUtil withFeatureIds(Set<UUID> featureIds) {
    this.featureIds = featureIds;
    return this;
  }

  public LicencePosition build() {
    var licencePosition = new LicencePosition(id);
    licencePosition.setLicence(licence);
    licencePosition.setLicenceTransaction(transaction);
    licencePosition.setPositionDate(positionDate);
    licencePosition.setPositionDateOrder(positionOrder);
    licencePosition.setExecuted(isExecuted);
    licencePosition.setFeatureIds(featureIds);

    return licencePosition;
  }
}
