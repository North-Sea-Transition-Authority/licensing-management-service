package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;

@ExtendWith(MockitoExtension.class)
class LicenceCorrectionServiceTest {

  private static final Clock  CLOCK = Clock.fixed(Instant.parse("2026-06-05T10:00:00Z"), ZoneId.systemDefault());
  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();
  private static final String CORRECTION_REFERENCE = "TEST-REF";
  private static final String REASON = "Test reason";

  @Mock
  private LicenceCorrectionRepository licenceCorrectionRepository;

  private LicenceCorrectionService licenceCorrectionService;

  @Captor
  private ArgumentCaptor<LicenceCorrection> licenceCorrectionCaptor;

  @BeforeEach
  void setUp() {
    licenceCorrectionService = new LicenceCorrectionService(licenceCorrectionRepository, CLOCK);
  }

  @Test
  void startCorrection_whenNoOpenCorrection() {
    when(licenceCorrectionRepository.existsByLicenceAndStatus(LICENCE, LicenceCorrectionStatus.IN_PROGRESS)).thenReturn(false);

    var expectedCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(null)
        .withLicence(LICENCE)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .withStatus(LicenceCorrectionStatus.IN_PROGRESS)
        .withAllocatedToWuaId(USER.wuaId())
        .withCreatedInstant(CLOCK.instant())
        .build();


    licenceCorrectionService.startCorrection(
        LICENCE,
        CORRECTION_REFERENCE,
        REASON,
        USER
    );

    verify(licenceCorrectionRepository).saveAndFlush(licenceCorrectionCaptor.capture());

    var result =  licenceCorrectionCaptor.getValue();

    assertThat(result).usingRecursiveComparison().isEqualTo(expectedCorrection);
  }

  @Test
  void startCorrection_whenOpenCorrection() {
    when(licenceCorrectionRepository.existsByLicenceAndStatus(LICENCE, LicenceCorrectionStatus.IN_PROGRESS)).thenReturn(true);

    assertThatThrownBy(() -> licenceCorrectionService.startCorrection(LICENCE, CORRECTION_REFERENCE, REASON, USER))
        .isInstanceOf(IllegalStateException.class);

    verify(licenceCorrectionRepository, never()).saveAndFlush(any());
  }

  @Test
  void startCorrection_whenConcurrentInsertViolatesConstraint() {
    when(licenceCorrectionRepository.existsByLicenceAndStatus(LICENCE, LicenceCorrectionStatus.IN_PROGRESS)).thenReturn(false);

    var cause = new DataIntegrityViolationException("licence_corrections_single_open_idx");
    when(licenceCorrectionRepository.saveAndFlush(any())).thenThrow(cause);

    assertThatThrownBy(() -> licenceCorrectionService.startCorrection(LICENCE, CORRECTION_REFERENCE, REASON, USER))
        .isInstanceOf(IllegalStateException.class)
        .hasCause(cause);
  }

  @Test
  void hasOpenCorrection() {
    when(licenceCorrectionRepository.existsByLicenceAndStatus(LICENCE, LicenceCorrectionStatus.IN_PROGRESS))
        .thenReturn(true);

    assertThat(licenceCorrectionService.hasOpenCorrection(LICENCE)).isTrue();
  }

  @Test
  void findByIdAndAllocatedToWuaId_whenFound() {
    var correctionId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(correctionId)
        .build();
    when(licenceCorrectionRepository.findByIdAndAllocatedToWuaId(correctionId, USER.wuaId()))
        .thenReturn(Optional.of(correction));

    var result = licenceCorrectionService.findByIdAndAllocatedToWuaId(correctionId, USER);

    assertThat(result).contains(correction);
  }

  @Test
  void findByIdAndAllocatedToWuaId_whenNotFound() {
    var correctionId = UUID.randomUUID();
    when(licenceCorrectionRepository.findByIdAndAllocatedToWuaId(correctionId, USER.wuaId()))
        .thenReturn(Optional.empty());

    var result = licenceCorrectionService.findByIdAndAllocatedToWuaId(correctionId, USER);

    assertThat(result).isEmpty();
  }
}
