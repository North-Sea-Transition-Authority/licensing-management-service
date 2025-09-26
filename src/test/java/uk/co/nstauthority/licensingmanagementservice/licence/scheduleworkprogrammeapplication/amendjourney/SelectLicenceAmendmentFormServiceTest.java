package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class SelectLicenceAmendmentFormServiceTest {

  @Mock
  LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  @Captor
  private ArgumentCaptor<LicenceWorkProgramAmendmentRequest> licenceWorkProgramAmendmentRequestArgumentCaptor;

  @InjectMocks
  SelectLicenceAmendmentFormService selectLicenceAmendmentFormService;

  @Test
  void saveAmendmentForm() {
    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();

    var form = new SelectLicenceAmendmentForm();
    form.setSelectedWorkProgrammeActivityAmendmentId(UUID.randomUUID());

    when(licenceWorkProgrammeAmendmentRepository.findByScheduleWorkProgrammeApplicationDetailsAndWorkProgrammeActivityId(any(),any())).thenReturn(
        Optional.of(new LicenceWorkProgramAmendmentRequest()));

    selectLicenceAmendmentFormService.saveAmendmentForm(form, scheduleWorkProgrammeApplicationDetail);

    verify(licenceWorkProgrammeAmendmentRepository).save(licenceWorkProgramAmendmentRequestArgumentCaptor.capture());

    var result = licenceWorkProgramAmendmentRequestArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceWorkProgramAmendmentRequest::getWorkProgrammeActivityId,
        LicenceWorkProgramAmendmentRequest::getScheduleWorkProgrammeApplicationDetails
    ).containsExactly(
        form.getSelectedWorkProgrammeActivityAmendmentId(),
        scheduleWorkProgrammeApplicationDetail
    );
  }
}