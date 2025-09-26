package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;

@ExtendWith(MockitoExtension.class)
class LicenceStartDateServiceTest {

  @Mock
  private LicenceStartDateRepository licenceStartDateRepository;

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @InjectMocks
  private LicenceStartDateService licenceStartDateService;

  @Captor
  private ArgumentCaptor<LicenceStartDate> licenceStartDateArgumentCaptor;

  @Test
  void getByLicenceScheduleDetailOrThrow() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var licenceStartDate = new LicenceStartDate();

    when(licenceStartDateRepository.findByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(Optional.of(licenceStartDate));

    assertThat(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).isEqualTo(licenceStartDate);
  }

  @Test
  void getByLicenceScheduleDetailOrThrow_licenceStartDateNotFound() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    when(licenceStartDateRepository.findByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void saveOrUpdateLicenceStartDateFromForm() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var form = new LicenceStartDateForm();
    form.getLicenceStartDate().setDay(1);
    form.getLicenceStartDate().setMonth(1);
    form.getLicenceStartDate().setYear(2025);

    var date = LocalDate.of(2025, 1, 1);

    licenceStartDateService.saveOrUpdateLicenceStartDateFromForm(form, licenceScheduleDetail);

    verify(licenceStartDateRepository).save(licenceStartDateArgumentCaptor.capture());

    assertThat(licenceStartDateArgumentCaptor.getValue()).extracting(
        LicenceStartDate::getLicenceScheduleDetail,
        LicenceStartDate::getStartDate
    ).containsExactly(
        licenceScheduleDetail,
        date
    );
  }

  @Test
  void saveNewLicenceStartDateFromForm() {
    var licence = new Licence();
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var form = new LicenceStartDateForm();
    form.getLicenceStartDate().setDay(1);
    form.getLicenceStartDate().setMonth(1);
    form.getLicenceStartDate().setYear(2025);

    var date = LocalDate.of(2025, 1, 1);

    when(licenceScheduleDetailService.createNewLicenceScheduleEntitiesForLicence(licence)).thenReturn(licenceScheduleDetail);

    licenceStartDateService.saveNewLicenceStartDateFromForm(form, licence);

    verify(licenceStartDateRepository).save(licenceStartDateArgumentCaptor.capture());

    assertThat(licenceStartDateArgumentCaptor.getValue()).extracting(
        LicenceStartDate::getLicenceScheduleDetail,
        LicenceStartDate::getStartDate
    ).containsExactly(
        licenceScheduleDetail,
        date
    );
  }

  @Test
  void getLicenceStartDateForm() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var startDate = LocalDate.of(2025, 1, 1);

    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(startDate);

    when(licenceStartDateRepository.findByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(Optional.of(licenceStartDate));

    assertThat(licenceStartDateService.getLicenceStartDateForm(licenceScheduleDetail).getLicenceStartDate().getAsLocalDate())
        .contains(startDate);
  }
}