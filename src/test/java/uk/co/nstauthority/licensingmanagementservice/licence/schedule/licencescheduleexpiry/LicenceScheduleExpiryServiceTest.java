package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

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
  void getExpiryByIdOrThrow() {
    var licenceScheduleExpiry = new LicenceScheduleExpiry();
    licenceScheduleExpiry.setId(UUID.randomUUID());

    when(licenceScheduleExpiryRepository.findById(licenceScheduleExpiry.getId())).thenReturn(Optional.of(licenceScheduleExpiry));

    assertThat(licenceScheduleExpiryService.getExpiryByIdOrThrow(licenceScheduleExpiry.getId())).isEqualTo(licenceScheduleExpiry);
  }

  @Test
  void getExpiryByIdOrThrow_expiryNotFound() {
    when(licenceScheduleExpiryRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> licenceScheduleExpiryService.getExpiryByIdOrThrow(UUID.randomUUID()))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getAllActiveExpiryDatesByLicenceScheduleDetail() {
    licenceScheduleExpiryService.getAllActiveExpiryDatesByLicenceScheduleDetail(licenceScheduleDetail);

    verify(licenceScheduleExpiryRepository).findAllByLicenceScheduleDetailAndStatus(licenceScheduleDetail, LicenceScheduleEventStatus.ACTIVE);
  }

  @Test
  void getAllActiveExpiryDatesByDateRange() {
    var startDate = LocalDate.of(2026, 1, 1);
    var endDate = LocalDate.of(2027, 1, 1);

    licenceScheduleExpiryService.getAllActiveExpiryDatesByDateRange(
        licenceScheduleDetail,
        startDate,
        endDate
    );

    verify(licenceScheduleExpiryRepository).findAllByLicenceScheduleDetailAndStatusAndExpiryDateBetween(
        licenceScheduleDetail,
        LicenceScheduleEventStatus.ACTIVE,
        startDate,
        endDate
    );
  }

  @Test
  void getAllActiveExpiryDatesByDateRangeFor_term() {
    var term = new LicenceScheduleTerm();
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setStartDate(LocalDate.of(2026, 1, 1));
    term.setEndDate(LocalDate.of(2027, 1, 1));

    licenceScheduleExpiryService.getAllActiveExpiryDatesByDateRangeFor(term);

    verify(licenceScheduleExpiryRepository).findAllByLicenceScheduleDetailAndStatusAndExpiryDateBetween(
        licenceScheduleDetail,
        LicenceScheduleEventStatus.ACTIVE,
        term.getStartDate(),
        term.getEndDate()
    );
  }

  @Test
  void getAllActiveExpiryDatesByDateRangeFor_phase() {
    var phase = new LicenceSchedulePhase();
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setStartDate(LocalDate.of(2026, 1, 1));
    phase.setEndDate(LocalDate.of(2027, 1, 1));

    licenceScheduleExpiryService.getAllActiveExpiryDatesByDateRangeFor(phase);

    verify(licenceScheduleExpiryRepository).findAllByLicenceScheduleDetailAndStatusAndExpiryDateBetween(
        licenceScheduleDetail,
        LicenceScheduleEventStatus.ACTIVE,
        phase.getStartDate(),
        phase.getEndDate()
    );
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
        LicenceScheduleExpiry::getStatus,
        LicenceScheduleExpiry::getExpiryDate,
        LicenceScheduleExpiry::getComments
    ).containsExactly(
        licenceScheduleDetail,
        LicenceScheduleEventStatus.ACTIVE,
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