package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.mockito.ArgumentMatchers.any;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.ownedgisartefact.CorrectionOwnedGisArtefactService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionRepository;

@ExtendWith(MockitoExtension.class)
class LicenceCleardownServiceTest {

  private static final String DELETE_CHANGES =
      "DELETE FROM licence_position_changes c WHERE c.licencePosition IN :licencePositions";
  private static final String DELETE_POSITION_STAGED_PAYLOADS =
      "DELETE FROM licence_position_staged_payloads c WHERE c.licenceCorrection IN :licenceCorrections";

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
  private CorrectionOwnedGisArtefactService correctionOwnedGisArtefactService;

  @Mock
  private Query deleteChangesQuery;

  @Mock
  private Query deletePositionStagedPayloadsQuery;

  @InjectMocks
  private LicenceCleardownService licenceCleardownService;

  @Test
  void clear_whenTransactionsOnlyHeldByLicence_thenEverythingIsDeletedInOrder() {
    var transaction = LicenceTransactionTestUtil.newBuilder().build();
    var positions = List.of(
        LicencePositionTestUtil.newBuilder().withLicence(LICENCE).withLicenceTransaction(transaction).build(),
        LicencePositionTestUtil.newBuilder().withLicence(LICENCE).withLicenceTransaction(transaction).build()
    );
    var inProgressCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withStatus(LicenceCorrectionStatus.IN_PROGRESS)
        .build();
    var completeCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .build();
    var corrections = List.of(inProgressCorrection, completeCorrection);

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(positions);
    when(licenceCorrectionRepository.findAllByLicence(LICENCE)).thenReturn(corrections);
    when(licencePositionRepository.findByLicenceTransactionIn(List.of(transaction))).thenReturn(positions);
    when(entityManager.createQuery(DELETE_CHANGES)).thenReturn(deleteChangesQuery);
    when(deleteChangesQuery.setParameter("licencePositions", positions)).thenReturn(deleteChangesQuery);
    when(entityManager.createQuery(DELETE_POSITION_STAGED_PAYLOADS)).thenReturn(deletePositionStagedPayloadsQuery);
    when(deletePositionStagedPayloadsQuery.setParameter("licenceCorrections", corrections))
        .thenReturn(deletePositionStagedPayloadsQuery);

    licenceCleardownService.clear(LICENCE);

    var inOrder = inOrder(
        correctionOwnedGisArtefactService,
        entityManager,
        deleteChangesQuery,
        deletePositionStagedPayloadsQuery,
        licenceCorrectionRepository,
        licencePositionRepository,
        licenceTransactionRepository
    );
    inOrder.verify(correctionOwnedGisArtefactService).cleanUpOnCancel(inProgressCorrection);
    inOrder.verify(correctionOwnedGisArtefactService).cleanUpOnCancel(completeCorrection);
    inOrder.verify(entityManager).flush();
    inOrder.verify(entityManager).clear();
    inOrder.verify(deleteChangesQuery).executeUpdate();
    inOrder.verify(deletePositionStagedPayloadsQuery).executeUpdate();
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
    verify(entityManager, never()).createQuery(DELETE_POSITION_STAGED_PAYLOADS);
    verify(correctionOwnedGisArtefactService, never()).cleanUpOnCancel(any());
    verify(entityManager, never()).clear();
  }
}
