package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

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
class ScheduleWorkProgrammeApplicationDetailRequestCacheTest {

  @Mock
  private ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;

  @InjectMocks
  private ScheduleWorkProgrammeApplicationDetailRequestCache scheduleWorkProgrammeApplicationDetailRequestCache;

  @Test
  void findById() {
    var detail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder().withId(UUID.randomUUID()).build();
    when(scheduleWorkProgrammeApplicationDetailRepository.findById(detail.getId())).thenReturn(Optional.of(detail));

    var result = scheduleWorkProgrammeApplicationDetailRequestCache.findById(detail.getId());

    assertThat(result).containsSame(detail);
  }
}
