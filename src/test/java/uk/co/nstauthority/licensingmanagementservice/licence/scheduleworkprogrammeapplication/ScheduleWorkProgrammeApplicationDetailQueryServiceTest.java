package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.LatestApplicationDetailSummary;

@ExtendWith(MockitoExtension.class)
class ScheduleWorkProgrammeApplicationDetailQueryServiceTest {

  @Mock
  private ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;

  @InjectMocks
  private ScheduleWorkProgrammeApplicationDetailQueryService scheduleWorkProgrammeApplicationDetailQueryService;

  @Test
  void getLatestByApplication() {
    var application = new ScheduleWorkProgrammeApplication();
    application.setId(UUID.randomUUID());

    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    applicationDetail.setId(UUID.randomUUID());

    when(scheduleWorkProgrammeApplicationDetailRepository
        .getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.of(applicationDetail));

    var result = scheduleWorkProgrammeApplicationDetailQueryService.getLatestByApplication(application);

    assertThat(result).contains(applicationDetail);
  }

  @Test
  void getLatestSummariesByApplicationIds() {
    var applicationId = UUID.randomUUID();

    var summary = new LatestApplicationDetailSummary(applicationId, UUID.randomUUID(), ApplicationStatus.SUBMITTED, "LMS/EAA/2040/1");

    when(scheduleWorkProgrammeApplicationDetailRepository
        .findLatestSummariesByScheduleWorkProgrammeApplication_IdIn(Set.of(applicationId)))
        .thenReturn(List.of(summary));

    var result = scheduleWorkProgrammeApplicationDetailQueryService.getLatestSummariesByApplicationIds(Set.of(applicationId));

    assertThat(result).containsExactly(summary);
  }
}
