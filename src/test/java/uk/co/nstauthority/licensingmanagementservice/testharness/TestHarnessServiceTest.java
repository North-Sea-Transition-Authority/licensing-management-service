package uk.co.nstauthority.licensingmanagementservice.testharness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;

@ExtendWith(MockitoExtension.class)
class TestHarnessServiceTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 6, 10);
  private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
  private static final Licence LICENCE = LicenceTestUtil.builder().withId(1).build();
  private static final Licence SECONDARY_LICENCE = LicenceTestUtil.builder().withId(2).build();
  private static final LicenceTransaction TRANSACTION_1 = LicenceTransactionTestUtil.newBuilder().build();
  private static final LicenceTransaction TRANSACTION_2 = LicenceTransactionTestUtil.newBuilder().build();
  private static final LicenceTransaction SAME_TRANSACTION = LicenceTransactionTestUtil.newBuilder().build();
  private static final LicenceTransaction CROSS_TRANSACTION = LicenceTransactionTestUtil.newBuilder().build();

  @Mock
  private LicenceTransactionService licenceTransactionService;

  @Mock
  private LicencePositionService licencePositionService;

  private TestHarnessService testHarnessService;

  @Captor
  private ArgumentCaptor<Licence> licenceCaptor;

  @Captor
  private ArgumentCaptor<LicenceTransaction> transactionCaptor;

  @Captor
  private ArgumentCaptor<LocalDate> dateCaptor;

  @BeforeEach
  void setUp() {
    testHarnessService = new TestHarnessService(licenceTransactionService, licencePositionService, CLOCK);
  }

  @Test
  void generateLicencePositions_assertSixPositionsCoveringAllAcceptanceCriteria() {
    when(licenceTransactionService.createLicenceTransaction(anyString()))
        .thenReturn(TRANSACTION_1, TRANSACTION_2, SAME_TRANSACTION, CROSS_TRANSACTION);

    testHarnessService.generateLicencePositions(LICENCE, SECONDARY_LICENCE);

    verify(licenceTransactionService, times(4)).createLicenceTransaction(anyString());
    verify(licencePositionService, times(6))
        .createLicencePosition(licenceCaptor.capture(), transactionCaptor.capture(), dateCaptor.capture());

    assertThat(licenceCaptor.getAllValues()).containsExactly(
        LICENCE, LICENCE,            // 1. Same date pair on primary licence
        LICENCE, LICENCE,            // 2. Same transaction pair on primary licence
        LICENCE, SECONDARY_LICENCE); // 3. Cross licence reuse

    assertThat(transactionCaptor.getAllValues()).containsExactly(
        TRANSACTION_1, TRANSACTION_2,                 // 1. Same date, different transactions
        SAME_TRANSACTION, SAME_TRANSACTION,           // 2. Same transaction, different dates
        CROSS_TRANSACTION, CROSS_TRANSACTION);        // 3. Same transaction reused across licences

    assertThat(dateCaptor.getAllValues()).containsExactly(
        TODAY.minusWeeks(7), TODAY.minusWeeks(7),   // 1. Two positions same date
        TODAY.minusWeeks(5), TODAY.minusWeeks(3),   // 2. Same transaction, different dates
        TODAY.minusWeeks(2), TODAY.minusWeeks(1));
  }
}