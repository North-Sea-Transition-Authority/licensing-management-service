package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
  void isValid_valid_dateProvided() {
    var form = new LicenceScheduleExpiryForm();
    form.getExpiryDate().setDate(LocalDate.of(2026, 1, 1));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    assertThat(licenceScheduleExpiryFormValidator.isValid(form, bindingResult, licenceScheduleDetail)).isTrue();
  }


  @Test
  void isValid_valid_dateNotProvided() {
    var form = new LicenceScheduleExpiryForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    assertThat(licenceScheduleExpiryFormValidator.isValid(form, bindingResult, licenceScheduleDetail)).isTrue();
  }

  @Test
  void isValid_invalid_expiryDateBeforeLicenceStartDate() {
    var form = new LicenceScheduleExpiryForm();
    form.getExpiryDate().setDate(LocalDate.of(2024, 1, 1));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    assertThat(licenceScheduleExpiryFormValidator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }



}