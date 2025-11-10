package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionService;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleSupportingInformationFormValidatorTest {

  @Mock
  private LicenceScheduleExtensionService licenceScheduleExtensionService;

  @Mock
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  @InjectMocks
  private LicenceScheduleSupportingInformationFormValidator licenceScheduleSupportingInformationFormValidator;

  private LicenceScheduleSupportingInformationForm form;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @BeforeEach
  void setUp() {
    form = new LicenceScheduleSupportingInformationForm();
    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
  }

  @Test
  void isValid_AllMandatoryFieldsPresent_NoExtensionOrAmendment() {
    when(licenceScheduleExtensionService.isExtensionRequested(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(false);
    when(licenceWorkProgrammeAmendmentService.isAmendmentRequested(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(false);

    form.setLicenceProgress("Some progress info");
    form.setReasonForAmendment("Some reason");
    form.setImpactOnDeliverables("Some impact");
    form.setPlanDuringExtension("");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleSupportingInformationFormValidator.isValid(bindingResult, scheduleWorkProgrammeApplicationDetail)).isTrue();
  }

  @Test
  void isValid_AllFieldsPresent_WithExtensionAndAmendment() {
    when(licenceScheduleExtensionService.isExtensionRequested(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(true);

    form.setLicenceProgress("Some progress info");
    form.setReasonForAmendment("Some reason");
    form.setImpactOnDeliverables("Some impact");
    form.setPlanDuringExtension("Our plan for the extension period");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(
        licenceScheduleSupportingInformationFormValidator.isValid(bindingResult, scheduleWorkProgrammeApplicationDetail)).isTrue();
  }


  @Test
  void isInvalid_MissingLicenceProgress_NoExtensionOrAmendment() {
    when(licenceScheduleExtensionService.isExtensionRequested(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(false);
    when(licenceWorkProgrammeAmendmentService.isAmendmentRequested(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(false);

    form.setReasonForAmendment("Some reason");
    form.setImpactOnDeliverables("Some impact");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleSupportingInformationFormValidator.isValid(bindingResult, scheduleWorkProgrammeApplicationDetail)).isFalse();
    assertTrue(bindingResult.hasFieldErrors("licenceProgress"));
    assertThat(bindingResult.getErrorCount()).isEqualTo(1);
  }

  @Test
  void isInvalid_MissingPlanDuringExtension_WithExtensionAndAmendment() {
    when(licenceScheduleExtensionService.isExtensionRequested(scheduleWorkProgrammeApplicationDetail)).thenReturn(true);

    form.setLicenceProgress("Some progress info");
    form.setReasonForAmendment("Some reason");
    form.setImpactOnDeliverables("Some impact");
    form.setPlanDuringExtension("");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleSupportingInformationFormValidator.isValid(bindingResult, scheduleWorkProgrammeApplicationDetail)).isFalse();
    assertTrue(bindingResult.hasFieldErrors("planDuringExtension"));
    assertThat(bindingResult.getErrorCount()).isEqualTo(1);
  }

  @Test
  void isInvalid_MissingAllFields_WithExtensionAndAmendment() {
    when(licenceScheduleExtensionService.isExtensionRequested(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(true);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleSupportingInformationFormValidator.isValid(bindingResult, scheduleWorkProgrammeApplicationDetail)).isFalse();
    assertTrue(bindingResult.hasFieldErrors("licenceProgress"));
    assertTrue(bindingResult.hasFieldErrors("planDuringExtension"));
    assertTrue(bindingResult.hasFieldErrors("reasonForAmendment"));
    assertTrue(bindingResult.hasFieldErrors("impactOnDeliverables"));
    assertThat(bindingResult.getErrorCount()).isEqualTo(4);
  }
}