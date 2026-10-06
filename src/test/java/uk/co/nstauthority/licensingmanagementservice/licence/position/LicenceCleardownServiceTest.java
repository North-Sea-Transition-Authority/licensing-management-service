package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionRepository;

@ExtendWith(MockitoExtension.class)
class LicenceCleardownServiceTest {

  private static final String DELETE_CHANGES =
      "DELETE FROM licence_position_changes c WHERE c.licencePosition IN :licencePositions";
  private static final String DELETE_POSITION_CORRECTIONS =
      "DELETE FROM licence_position_corrections c WHERE c.licenceCorrection IN :licenceCorrections";

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final Licence OTHER_LICENCE = LicenceTestUtil.builder().build();

  @Mock
  private LicencePositionRepository licencePositionRepository;

  @Mock
  private LicenceCorrectionRepository licenceCorrectionRepository;

  @Mock
  private LicenceTransactionRepository licenceTransactionRepository;

  @Mock
  private EntityManager entityManager;

  @Mock
  private Query deleteChangesQuery;

  @Mock
  private Query deletePositionCorrectionsQuery;

  @InjectMocks
  private LicenceCleardownService licenceCleardownService;

  @Test
  void clear_whenTransactionsOnlyHeldByLicence_thenEverythingIsDeletedInOrder() {
    var transaction = LicenceTransactionTestUtil.newBuilder().build();
    var positions = List.of(
        LicencePositionTestUtil.newBuilder().withLicence(LICENCE).withLicenceTransaction(transaction).build(),
        LicencePositionTestUtil.newBuilder().withLicence(LICENCE).withLicenceTransaction(transaction).build()
    );
    var corrections = List.of(LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build());

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(positions);
    when(licenceCorrectionRepository.findAllByLicence(LICENCE)).thenReturn(corrections);
    when(licencePositionRepository.findByLicenceTransactionIn(List.of(transaction))).thenReturn(positions);
    when(entityManager.createQuery(DELETE_CHANGES)).thenReturn(deleteChangesQuery);
    when(deleteChangesQuery.setParameter("licencePositions", positions)).thenReturn(deleteChangesQuery);
    when(entityManager.createQuery(DELETE_POSITION_CORRECTIONS)).thenReturn(deletePositionCorrectionsQuery);
    when(deletePositionCorrectionsQuery.setParameter("licenceCorrections", corrections))
        .thenReturn(deletePositionCorrectionsQuery);

    licenceCleardownService.clear(LICENCE);

    var inOrder = inOrder(
        deleteChangesQuery,
        deletePositionCorrectionsQuery,
        licenceCorrectionRepository,
        licencePositionRepository,
        licenceTransactionRepository
    );
    inOrder.verify(deleteChangesQuery).executeUpdate();
    inOrder.verify(deletePositionCorrectionsQuery).executeUpdate();
    inOrder.verify(licenceCorrectionRepository).deleteAll(corrections);
    inOrder.verify(licencePositionRepository).deleteAll(positions);
    inOrder.verify(licenceTransactionRepository).deleteAll(List.of(transaction));
  }

  @Test
  void clear_whenTransactionHeldByAnotherLicence_thenTransactionIsKept() {
    var sharedTransaction = LicenceTransactionTestUtil.newBuilder().build();
    var unsharedTransaction = LicenceTransactionTestUtil.newBuilder().build();
    var sharedPosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(sharedTransaction)
        .build();
    var unsharedPosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(unsharedTransaction)
        .build();
    var otherLicencePosition = LicencePositionTestUtil.newBuilder()
        .withLicence(OTHER_LICENCE)
        .withLicenceTransaction(sharedTransaction)
        .build();
    var positions = List.of(sharedPosition, unsharedPosition);

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(positions);
    when(licenceCorrectionRepository.findAllByLicence(LICENCE)).thenReturn(List.of());
    when(licencePositionRepository.findByLicenceTransactionIn(List.of(sharedTransaction, unsharedTransaction)))
        .thenReturn(List.of(sharedPosition, unsharedPosition, otherLicencePosition));
    when(entityManager.createQuery(DELETE_CHANGES)).thenReturn(deleteChangesQuery);
    when(deleteChangesQuery.setParameter("licencePositions", positions)).thenReturn(deleteChangesQuery);

    licenceCleardownService.clear(LICENCE);

    verify(licencePositionRepository).deleteAll(positions);
    verify(licenceTransactionRepository).deleteAll(List.of(unsharedTransaction));
  }

  @Test
  void clear_whenLicenceHoldsNothing_thenNoBulkDeleteIsRun() {
    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(List.of());
    when(licenceCorrectionRepository.findAllByLicence(LICENCE)).thenReturn(List.of());
    when(licencePositionRepository.findByLicenceTransactionIn(List.of())).thenReturn(List.of());

    licenceCleardownService.clear(LICENCE);

    verify(entityManager, never()).createQuery(DELETE_CHANGES);
    verify(entityManager, never()).createQuery(DELETE_POSITION_CORRECTIONS);
  }
}
