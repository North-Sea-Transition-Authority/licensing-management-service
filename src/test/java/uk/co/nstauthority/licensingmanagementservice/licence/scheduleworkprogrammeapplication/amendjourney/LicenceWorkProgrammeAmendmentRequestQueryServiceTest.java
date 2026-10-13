package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

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
class LicenceWorkProgrammeAmendmentRequestQueryServiceTest {

  @Mock
  private LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  @InjectMocks
  private LicenceWorkProgrammeAmendmentRequestQueryService licenceWorkProgrammeAmendmentRequestQueryService;

  @Test
  void getAllByApplicationDetail() {
    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    applicationDetail.setId(UUID.randomUUID());

    var amendmentRequest = new LicenceWorkProgrammeAmendmentRequest();
    amendmentRequest.setId(UUID.randomUUID());

    when(licenceWorkProgrammeAmendmentRepository.findAllByScheduleWorkProgrammeApplicationDetails(applicationDetail))
        .thenReturn(List.of(amendmentRequest));

    var result = licenceWorkProgrammeAmendmentRequestQueryService.getAllByApplicationDetail(applicationDetail);

    assertThat(result).containsExactly(amendmentRequest);
  }
}
