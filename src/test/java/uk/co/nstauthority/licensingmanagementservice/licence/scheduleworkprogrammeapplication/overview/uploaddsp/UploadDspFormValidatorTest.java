package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;
import uk.co.nstauthority.licensingmanagementservice.file.FileUploadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class UploadDspFormValidatorTest {

  private static final LocalDate TODAY = LocalDate.of(2024, Month.MARCH, 15);

  @Mock
  private Clock clock;

  @InjectMocks
  private UploadDspFormValidator validator;

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
  }

  private UploadDspForm buildValidForm() {
    var form = new UploadDspForm();
    form.getDecisionDate().setDate(TODAY);
    form.setFinalDecisionSupportPapers(List.of(
        FileUploadTestUtil.getUploadedFileFormWithDescription("decision.pdf", "Final decision paper")));
    return form;
  }

  @Test
  void isValid_whenValidDateAndOneFile_returnsTrue() {
    var form = buildValidForm();
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenDateEmpty_returnsFalse() {
    var form = buildValidForm();
    form.setDecisionDate(new ThreeFieldDateInput("decisionDate", "decision date"));
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
    assertThat(bindingResult.hasFieldErrors("decisionDate.dayInput.inputValue")).isTrue();
  }

  @Test
  void isValid_whenNoFiles_returnsFalse() {
    var form = buildValidForm();
    form.setFinalDecisionSupportPapers(List.of());
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
    assertThat(bindingResult.hasFieldErrors("finalDecisionSupportPapers")).isTrue();
  }

  @Test
  void isValid_whenDateInFuture_returnsFalse() {
    var form = buildValidForm();
    form.getDecisionDate().setDate(TODAY.plusDays(1));
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
    assertThat(bindingResult.hasFieldErrors("decisionDate.dayInput.inputValue")).isTrue();
  }
}
