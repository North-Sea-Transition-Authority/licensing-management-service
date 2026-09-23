package uk.co.nstauthority.licensingmanagementservice.hibernate;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class AssignedOrGeneratedUuidGeneratorIntegrationTest {

  @Autowired
  private EntityManager em;

  @Test
  void generate_whenIdIsAssigned_thenTheAssignedIdIsPersisted() {
    var transactionId = UUID.randomUUID();
    var positionId = UUID.randomUUID();
    var changeId = UUID.randomUUID();

    persist(transactionId, positionId, changeId);
    em.flush();
    em.clear();

    assertThat(em.find(LicenceTransaction.class, transactionId)).isNotNull();
    assertThat(em.find(LicencePosition.class, positionId)).isNotNull();
    assertThat(em.find(LicencePositionChange.class, changeId)).isNotNull();
  }

  @Test
  void generate_whenNoIdIsAssigned_thenAnIdIsGenerated() {
    var persisted = persist(null, null, null);

    em.flush();

    assertThat(persisted.transaction().getId()).isNotNull();
    assertThat(persisted.position().getId()).isNotNull();
    assertThat(persisted.change().getId()).isNotNull();
  }

  private PersistedEntities persist(UUID transactionId, UUID positionId, UUID changeId) {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceReference("P001")
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();
    em.persist(licence);

    var transaction = LicenceTransactionTestUtil.newBuilder().withId(transactionId).build();
    em.persist(transaction);

    var position = LicencePositionTestUtil.newBuilder()
        .withId(positionId)
        .withLicence(licence)
        .withLicenceTransaction(transaction)
        .build();
    em.persist(position);

    var change = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withLicencePosition(position)
        .build();
    em.persist(change);

    return new PersistedEntities(transaction, position, change);
  }

  private record PersistedEntities(
      LicenceTransaction transaction,
      LicencePosition position,
      LicencePositionChange change
  ) {
  }
}