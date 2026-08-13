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

@ExtendWith(MockitoExtension.class)
class LicenceScheduleServiceTest {

  @Mock
  private LicenceScheduleRepository licenceScheduleRepository;

  @InjectMocks
  private LicenceScheduleService licenceScheduleService;

  @Captor
  private ArgumentCaptor<LicenceSchedule> licenceScheduleArgumentCaptor;

  @Test
  void getOrCreateNewLicenceScheduleForLicence() {
    var licence = new Licence();

    licenceScheduleService.getOrCreateNewLicenceScheduleForLicence(licence);

    verify(licenceScheduleRepository).save(licenceScheduleArgumentCaptor.capture());

    assertEquals(licence, licenceScheduleArgumentCaptor.getValue().getLicence());
  }
}
