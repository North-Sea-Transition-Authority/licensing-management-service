package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleServiceTest {

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceScheduleRepository licenceScheduleRepository;

  @InjectMocks
  private LicenceScheduleService licenceScheduleService;

  @Captor
  private ArgumentCaptor<LicenceSchedule> licenceScheduleArgumentCaptor;

  @Test
  void getLicenceScheduleByLicence() {
    var licence = new Licence();

    licenceScheduleService.getLicenceScheduleByLicence(licence);

    verify(licenceScheduleRepository).findByLicence(licence);
  }

  @Test
  void doesLicenceScheduleExistForLicence() {
    var licence = new Licence();

    licenceScheduleService.doesLicenceScheduleExistForLicence(licence);

    verify(licenceScheduleRepository).existsByLicence(licence);
  }

  @Test
  void createNewLicenceScheduleForLicence() {
    var licence = new Licence();

    licenceScheduleService.createNewLicenceScheduleForLicence(licence);

    verify(licenceScheduleRepository).save(licenceScheduleArgumentCaptor.capture());

    assertEquals(licence, licenceScheduleArgumentCaptor.getValue().getLicence());
  }
}