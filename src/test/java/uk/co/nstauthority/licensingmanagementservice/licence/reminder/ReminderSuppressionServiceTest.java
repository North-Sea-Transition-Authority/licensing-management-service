package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;

@ExtendWith(MockitoExtension.class)
class ReminderSuppressionServiceTest {

  @Mock
  private LicenceStatusService licenceStatusService;

  @InjectMocks
  private ReminderSuppressionService reminderSuppressionService;

  private Licence licence;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder().withId(1).withLicenceReference("P001").build();
  }

  @Test
  void getSuppressedLicenceIds_whenNoLicences_thenNothingIsSuppressed() {
    assertThat(reminderSuppressionService.getSuppressedLicenceIds(List.of())).isEmpty();

    verifyNoInteractions(licenceStatusService);
  }

  @Test
  void getSuppressedLicenceIds_whenTheLicenceIsExtant_thenItIsNotSuppressed() {
    mockLicenceStatus(LicenceStatusType.EXTANT);

    assertThat(reminderSuppressionService.getSuppressedLicenceIds(List.of(licence))).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(value = LicenceStatusType.class, names = "EXTANT", mode = EnumSource.Mode.EXCLUDE)
  void getSuppressedLicenceIds_whenTheLicenceIsNotExtant_thenItIsSuppressed(LicenceStatusType status) {
    mockLicenceStatus(status);

    assertThat(reminderSuppressionService.getSuppressedLicenceIds(List.of(licence)))
        .containsExactly(licence.getId());
  }

  @Test
  void getSuppressedLicenceIds_whenTheLicenceHasNoCurrentStatus_thenItIsSuppressed() {
    when(licenceStatusService.getCurrentStatusesByLicenceId(List.of(licence))).thenReturn(Map.of());

    assertThat(reminderSuppressionService.getSuppressedLicenceIds(List.of(licence)))
        .containsExactly(licence.getId());
  }

  private void mockLicenceStatus(LicenceStatusType status) {
    when(licenceStatusService.getCurrentStatusesByLicenceId(List.of(licence)))
        .thenReturn(Map.of(licence.getId(), status));
  }
}
