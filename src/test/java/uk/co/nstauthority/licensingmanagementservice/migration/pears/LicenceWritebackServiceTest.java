package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;

@ExtendWith(MockitoExtension.class)
class LicenceWritebackServiceTest {

  @Mock
  private LicenceTransactionService licenceTransactionService;

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicencePositionRepository licencePositionRepository;

  @Mock
  private LicenceCorrectionRepository licenceCorrectionRepository;

  @Mock
  private LicencePositionChangeRepository licencePositionChangeRepository;

  @Mock
  private LicencePositionCorrectionRepository licencePositionCorrectionRepository;

  @Mock
  private LicenceTransactionRepository licenceTransactionRepository;

  @Mock
  private PearsLicenceService pearsLicenceService;

  @InjectMocks
  private LicenceWritebackService licenceWritebackService;

  @Test
  void overwriteLicencePositionsFromPears_whenPearsHoldsNoPositions_thenNothingIsDeleted() {
    var licence = LicenceTestUtil.builder()
        .withLicencePrefix("P")
        .withLicenceNumber("1")
        .withLicenceReference("P1")
        .build();

    when(pearsLicenceService.livePositions("P", 1)).thenReturn(new LivePositions("P", 1, List.of()));

    var result = licenceWritebackService.overwriteLicencePositionsFromPears(licence);

    assertThat(result).isEqualTo(new LicenceWritebackResult("No positions found in PEARS for licence P1"));
    verifyNoInteractions(
        licenceTransactionService,
        licencePositionService,
        licencePositionRepository,
        licenceCorrectionRepository,
        licencePositionChangeRepository,
        licencePositionCorrectionRepository,
        licenceTransactionRepository
    );
  }
}
