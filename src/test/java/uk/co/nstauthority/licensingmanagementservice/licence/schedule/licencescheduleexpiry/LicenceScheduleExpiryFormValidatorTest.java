package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleExpiryFormValidatorTest {

  @Mock
  private LicenceScheduleExpiryService licenceScheduleExpiryService;

  @Mock
  private LicenceStartDateService licenceStartDateService;

  @InjectMocks
  private LicenceScheduleExpiryFormValidator licenceScheduleExpiryFormValidator;

  private LicenceScheduleDetail licenceScheduleDetail;
  private LicenceStartDate licenceStartDate;

  @BeforeEach
  void setUp() {
    licenceScheduleDetail = new LicenceScheduleDetail();
    licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(LocalDate.of(2025, 1, 1));

    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).thenReturn(licenceStartDate);
  }

  @Test
  void isValid() {
    var form = new LicenceScheduleExpiryForm();
    form.getExpiryDate().setDate(LocalDate.of(2026, 1, 1));

    when(licenceScheduleExpiryService.getAllActiveExpiryDatesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    assertThat(licenceScheduleExpiryFormValidator.isValid(form, bindingResult, licenceScheduleDetail)).isTrue();
  }

  @Test
  void isValid_invalid_expiryAlreadyExists() {
    var form = new LicenceScheduleExpiryForm();
    form.getExpiryDate().setDate(LocalDate.of(2026, 1, 1));

    when(licenceScheduleExpiryService.getAllActiveExpiryDatesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(new LicenceScheduleExpiry()));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    assertThat(licenceScheduleExpiryFormValidator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalid_expiryDateNotProvided() {
    var form = new LicenceScheduleExpiryForm();

    when(licenceScheduleExpiryService.getAllActiveExpiryDatesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    assertThat(licenceScheduleExpiryFormValidator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalid_expiryDateBeforeLicenceStartDate() {
    var form = new LicenceScheduleExpiryForm();
    form.getExpiryDate().setDate(LocalDate.of(2024, 1, 1));

    when(licenceScheduleExpiryService.getAllActiveExpiryDatesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    assertThat(licenceScheduleExpiryFormValidator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValidUpdate() {
    var form = new LicenceScheduleExpiryForm();
    form.getExpiryDate().setDate(LocalDate.of(2026, 1, 1));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    assertThat(licenceScheduleExpiryFormValidator.isValidUpdate(form, bindingResult, licenceScheduleDetail)).isTrue();
  }

  @Test
  void isValidUpdate_invalid_expiryDateNotProvided() {
    var form = new LicenceScheduleExpiryForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    assertThat(licenceScheduleExpiryFormValidator.isValidUpdate(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValidUpdate_invalid_expiryDateBeforeLicenceStartDate() {
    var form = new LicenceScheduleExpiryForm();
    form.getExpiryDate().setDate(LocalDate.of(2024, 1, 1));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    assertThat(licenceScheduleExpiryFormValidator.isValidUpdate(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

}