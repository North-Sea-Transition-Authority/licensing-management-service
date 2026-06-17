package uk.co.nstauthority.licensingmanagementservice.licence.position.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;

@ExtendWith(MockitoExtension.class)
class LicenceTransactionServiceTest {

  @Mock
  private LicenceTransactionRepository licenceTransactionRepository;

  @InjectMocks
  private LicenceTransactionService licenceTransactionService;

  @Captor
  private ArgumentCaptor<LicenceTransaction> licenceTransactionArgumentCaptor;

  @Test
  void createLicenceTransaction() {
    var expectedLicenceTransaction = LicenceTransactionTestUtil.newBuilder()
        .withId(null)
        .withRegulatorReference("TEST-REF")
        .build();

    licenceTransactionService.createLicenceTransaction("TEST-REF");

    verify(licenceTransactionRepository).save(licenceTransactionArgumentCaptor.capture());

    assertThat(licenceTransactionArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedLicenceTransaction);
  }
}