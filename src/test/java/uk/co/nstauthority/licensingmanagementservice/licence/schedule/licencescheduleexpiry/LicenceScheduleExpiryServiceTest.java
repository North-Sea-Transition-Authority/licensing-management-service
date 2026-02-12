package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleExpiryServiceTest {

  @Mock
  private LicenceScheduleExpiryRepository licenceScheduleExpiryRepository;

  @InjectMocks
  private LicenceScheduleExpiryService licenceScheduleExpiryService;

  @Captor
  private ArgumentCaptor<LicenceScheduleExpiry> licenceScheduleExpiryArgumentCaptor;

  private final LicenceScheduleDetail licenceScheduleDetail = new LicenceScheduleDetail();

  @Test
  void getExpiryForLicenceScheduleDetail() {
    licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail);

    verify(licenceScheduleExpiryRepository).findByLicenceScheduleDetail(licenceScheduleDetail);
  }

  @Test
  void getOrCreateExpiry() {
    var expiry = new LicenceScheduleExpiry();
    when(licenceScheduleExpiryRepository.findByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(Optional.of(expiry));

    assertThat(licenceScheduleExpiryService.getOrCreateExpiry(licenceScheduleDetail)).isEqualTo(expiry);
  }

  @Test
  void saveExpiryFromForm() {
    var form = new LicenceScheduleExpiryForm();
    form.getExpiryDate().setDate(LocalDate.of(2026, 1, 1));
    form.setComments("Comments");

    licenceScheduleExpiryService.saveExpiryFromForm(form, licenceScheduleDetail, new LicenceScheduleExpiry());

    verify(licenceScheduleExpiryRepository).save(licenceScheduleExpiryArgumentCaptor.capture());

    assertThat(licenceScheduleExpiryArgumentCaptor.getValue()).extracting(
        LicenceScheduleExpiry::getLicenceScheduleDetail,
        LicenceScheduleExpiry::getExpiryDate,
        LicenceScheduleExpiry::getComments
    ).containsExactly(
        licenceScheduleDetail,
        form.getExpiryDate().getAsLocalDate().orElse(null),
        form.getComments()
    );
  }

  @Test
  void saveExpiryFromForm_dateNotProvided() {
    var form = new LicenceScheduleExpiryForm();
    form.setComments("Comments");

    licenceScheduleExpiryService.saveExpiryFromForm(form, licenceScheduleDetail, new LicenceScheduleExpiry());

    verify(licenceScheduleExpiryRepository).save(licenceScheduleExpiryArgumentCaptor.capture());

    assertThat(licenceScheduleExpiryArgumentCaptor.getValue()).extracting(
        LicenceScheduleExpiry::getLicenceScheduleDetail,
        LicenceScheduleExpiry::getExpiryDate,
        LicenceScheduleExpiry::getComments
    ).containsExactly(
        licenceScheduleDetail,
        form.getExpiryDate().getAsLocalDate().orElse(null),
        form.getComments()
    );
  }

  @Test
  void getExpiryForm() {
    var expiry = new LicenceScheduleExpiry();
    expiry.setExpiryDate(LocalDate.of(2026, 1, 1));
    expiry.setComments("Comments");

    assertThat(licenceScheduleExpiryService.getExpiryForm(expiry)).extracting(
        form -> form.getExpiryDate().getAsLocalDate().orElse(null),
        LicenceScheduleExpiryForm::getComments
    ).containsExactly(
        expiry.getExpiryDate(),
        expiry.getComments()
    );
  }
}