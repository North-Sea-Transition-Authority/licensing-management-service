package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleDetailServiceTest {

  @Mock
  private LicenceScheduleDetailRepository licenceScheduleDetailRepository;

  @Mock
  private LicenceScheduleService licenceScheduleService;

  @InjectMocks
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @Captor
  private ArgumentCaptor<LicenceScheduleDetail> licenceScheduleDetailArgumentCaptor;

  @Test
  void getByLicenceSchedule() {
    var licenceSchedule = new LicenceSchedule();

    licenceScheduleDetailService.getByLicenceSchedule(licenceSchedule);

    verify(licenceScheduleDetailRepository).findByLicenceSchedule(licenceSchedule);
  }

  @Test
  void createNewLicenceScheduleEntitiesForLicence_noExistingEntities() {
    var licence = new Licence();
    var licenceSchedule = new LicenceSchedule();

    when(licenceScheduleService.createNewLicenceScheduleForLicence(licence)).thenReturn(licenceSchedule);

    licenceScheduleDetailService.createNewLicenceScheduleEntitiesForLicence(licence);

    verify(licenceScheduleDetailRepository).save(licenceScheduleDetailArgumentCaptor.capture());

    assertThat(licenceScheduleDetailArgumentCaptor.getValue().getLicenceSchedule()).isEqualTo(licenceSchedule);
  }

  @Test
  void createNewLicenceScheduleDetail() {
    var licenceSchedule = new LicenceSchedule();

    licenceScheduleDetailService.createNewLicenceScheduleDetail(licenceSchedule);

    verify(licenceScheduleDetailRepository).save(licenceScheduleDetailArgumentCaptor.capture());

    assertThat(licenceScheduleDetailArgumentCaptor.getValue().getLicenceSchedule()).isEqualTo(licenceSchedule);
  }
}