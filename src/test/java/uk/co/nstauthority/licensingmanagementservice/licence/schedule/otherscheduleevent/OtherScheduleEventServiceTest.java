package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OtherScheduleEventServiceTest {

  @Mock
  private OtherScheduleEventRepository otherScheduleEventRepository;
  
  @InjectMocks
  private OtherScheduleEventService otherScheduleEventService;
  
  @Test
  void getOtherScheduleEventByIdOrThrow() {
    var event = new OtherScheduleEvent();
    event.setId(UUID.randomUUID());

    when(otherScheduleEventRepository.findById(event.getId())).thenReturn(Optional.of(event));

    assertThat(otherScheduleEventService.getOtherScheduleEventByIdOrThrow(event.getId())).isEqualTo(event);
  }
}