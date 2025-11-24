package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeActivityServiceTest {

  @Mock
  private WorkProgrammeActivityRepository workProgrammeActivityRepository;

  @InjectMocks
  private WorkProgrammeActivityService workProgrammeActivityService;

  private static final UUID ACTIVITY_ID = UUID.randomUUID();

  @Test
  void isLinkedToFixedDate_returnsTrue() {
    when(workProgrammeActivityRepository.existsByIdAndDateOption(ACTIVITY_ID, WorkProgrammeActivityDateOption.FIXED_DATE))
        .thenReturn(true);

    boolean result = workProgrammeActivityService.isLinkedToFixedDate(ACTIVITY_ID);

    assertThat(result).isTrue();
    verify(workProgrammeActivityRepository).existsByIdAndDateOption(ACTIVITY_ID, WorkProgrammeActivityDateOption.FIXED_DATE);
  }

  @Test
  void isLinkedToFixedDate_returnsFalse() {
    when(workProgrammeActivityRepository.existsByIdAndDateOption(ACTIVITY_ID, WorkProgrammeActivityDateOption.FIXED_DATE))
        .thenReturn(false);

    boolean result = workProgrammeActivityService.isLinkedToFixedDate(ACTIVITY_ID);

    assertThat(result).isFalse();
    verify(workProgrammeActivityRepository).existsByIdAndDateOption(ACTIVITY_ID, WorkProgrammeActivityDateOption.FIXED_DATE);
  }
}