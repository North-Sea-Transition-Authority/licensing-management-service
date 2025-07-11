package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

}