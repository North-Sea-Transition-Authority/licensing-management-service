package uk.co.nstauthority.licensingmanagementservice.licence.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney.SelectLicenceForm;

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
  void doesLicenceScheduleExistForLicence() {
    var licence = new Licence();

    licenceScheduleService.doesLicenceScheduleExistForLicence(licence);

    verify(licenceScheduleRepository).existsByLicence(licence);
  }

  @Test
  void saveLicenceScheduleFromForm() {
    var form = new SelectLicenceForm();
    form.setLicenceId("1");

    var licence = new Licence();

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);

    licenceScheduleService.saveLicenceScheduleFromForm(form);

    verify(licenceScheduleRepository).save(licenceScheduleArgumentCaptor.capture());

    assertEquals(licence, licenceScheduleArgumentCaptor.getValue().getLicence());
  }
}