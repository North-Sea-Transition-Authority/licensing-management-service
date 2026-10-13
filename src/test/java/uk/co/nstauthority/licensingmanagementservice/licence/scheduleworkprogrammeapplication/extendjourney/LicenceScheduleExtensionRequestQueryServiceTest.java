package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleExtensionRequestQueryServiceTest {

  @Mock
  private LicenceScheduleExtensionRepository licenceScheduleExtensionRepository;

  @InjectMocks
  private LicenceScheduleExtensionRequestQueryService licenceScheduleExtensionRequestQueryService;

  @Test
  void getAllByApplicationDetail() {
    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    applicationDetail.setId(UUID.randomUUID());

    var extensionRequest = new LicenceScheduleExtensionRequest();
    extensionRequest.setId(UUID.randomUUID());

    when(licenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of(extensionRequest));

    var result = licenceScheduleExtensionRequestQueryService.getAllByApplicationDetail(applicationDetail);

    assertThat(result).containsExactly(extensionRequest);
  }
}
