package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleExtensionSubmissionServiceTest {

  @Mock
  LicenceScheduleExtensionService licenceScheduleExtensionFormService;

  @Mock
  LicenceScheduleExtensionFormValidator licenceScheduleExtensionFormValidator;

  @InjectMocks
  LicenceScheduleExtensionSubmissionService licenceScheduleExtensionSubmissionService;

  private final ScheduleWorkProgrammeApplicationDetail applicationDetail =
      new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID());

  private final List<LicenceScheduleExtensionRequestView> extensionRequestViews = List.of(
      new LicenceScheduleExtensionRequestView(UUID.randomUUID().toString(), "Phase A", true, false, null)
  );

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void isSectionSubmittable(boolean isValid) {
    var scheduleDetail = new LicenceScheduleDetail();
    var form = new LicenceScheduleExtensionForm();
    when(licenceScheduleExtensionFormService.getLicenceScheduleExtensionViews(applicationDetail, scheduleDetail))
        .thenReturn(extensionRequestViews);
    when(licenceScheduleExtensionFormService.buildLicenceScheduleExtensionForm(extensionRequestViews))
        .thenReturn(form);
    when(licenceScheduleExtensionFormValidator.isValidAgainstExtensionViews(
        eq(form),
        any(BindingResult.class),
        eq(extensionRequestViews)
    )).thenReturn(isValid);

    var result = licenceScheduleExtensionSubmissionService.isSectionSubmittable(applicationDetail, scheduleDetail);

    assertThat(result).isEqualTo(isValid);
  }
}
