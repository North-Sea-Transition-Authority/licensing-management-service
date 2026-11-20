package uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleAreaEventServiceTest {

  @Mock
  private LicenceScheduleAreaEventRepository licenceScheduleAreaEventRepository;

  @InjectMocks
  private LicenceScheduleAreaEventService licenceScheduleAreaEventService;

  private final LicenceScheduleDetail licenceScheduleDetail = new LicenceScheduleDetail();

  @Test
  void getAreaEvents() {
    var areaEvent = new LicenceScheduleAreaEvent();
    when(licenceScheduleAreaEventRepository.findAllByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(areaEvent));

    assertThat(licenceScheduleAreaEventService.getAreaEvents(licenceScheduleDetail)).containsExactly(areaEvent);
  }
}
