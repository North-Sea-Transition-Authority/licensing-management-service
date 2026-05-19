package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;

@ExtendWith(MockitoExtension.class)
class EventReferenceServiceTest {

  @Mock
  private EventReferenceRepository eventReferenceRepository;

  @InjectMocks
  private EventReferenceService eventReferenceService;

  @Captor
  private ArgumentCaptor<EventReference> eventReferenceArgumentCaptor;

  @Test
  void createEventReference() {
    var licenceSchedule = new LicenceSchedule();

    eventReferenceService.createEventReference(licenceSchedule);

    verify(eventReferenceRepository).save(eventReferenceArgumentCaptor.capture());

    assertThat(eventReferenceArgumentCaptor.getValue().getLicenceSchedule()).isEqualTo(licenceSchedule);
  }
}
