package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;

@ExtendWith(MockitoExtension.class)
class AddLicencePositionStagedPayloadFormValidatorTest {

  private static final UnaryOperator<String> REQUIRED = "%s.required"::formatted;

  private static final LocalDate TODAY = LocalDate.of(2024, Month.JUNE, 15);

  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();

  @Mock
  private Clock clock;

  @Mock
  private LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  @InjectMocks
  private AddLicencePositionStagedPayloadFormValidator addLicencePositionStagedPayloadFormValidator;

  private AddLicencePositionStagedPayloadForm addLicencePositionStagedPayloadForm;

  private BindingResult bindingResult;

  @BeforeEach
  void setUp() {
    lenient().when(clock.instant()).thenReturn(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
    lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);

    addLicencePositionStagedPayloadForm = new AddLicencePositionStagedPayloadForm();
    bindingResult = new BeanPropertyBindingResult(addLicencePositionStagedPayloadForm, "form");
  }

  @Test
  void hasErrors_emptyForm() {
    var hasErrors = addLicencePositionStagedPayloadFormValidator
        .hasErrors(addLicencePositionStagedPayloadForm, LICENCE_CORRECTION, bindingResult);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField)
        .containsExactly(
            "correctionReference.inputValue",
            "positionDate.dayInput.inputValue",
            "positionDate.monthInput.inputValue",
            "positionDate.yearInput.inputValue"
        );

    assertThat(bindingResult.getFieldErrors())
        .filteredOn(fieldError -> "correctionReference.inputValue".equals(fieldError.getField()))
        .extracting(FieldError::getDefaultMessage, FieldError::getCode)
        .containsExactly(
            tuple("Enter a correction reference", REQUIRED.apply("correctionReference"))
        );

    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_positionDateInFuture() {
    addLicencePositionStagedPayloadForm.getCorrectionReference().setInputValue("TEST-REF");
    addLicencePositionStagedPayloadForm.getPositionDate().setDate(TODAY.plusDays(1));

    when(licencePositionStagedPayloadService.isCorrectionReferenceInUse(LICENCE_CORRECTION, "TEST-REF"))
        .thenReturn(false);

    var hasErrors = addLicencePositionStagedPayloadFormValidator
        .hasErrors(addLicencePositionStagedPayloadForm, LICENCE_CORRECTION, bindingResult);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField)
        .containsExactly(
            "positionDate.dayInput.inputValue",
            "positionDate.monthInput.inputValue",
            "positionDate.yearInput.inputValue"
        );

    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_duplicateCorrectionReference() {
    addLicencePositionStagedPayloadForm.getCorrectionReference().setInputValue("TEST-REF");
    addLicencePositionStagedPayloadForm.getPositionDate().setDate(TODAY);

    when(licencePositionStagedPayloadService.isCorrectionReferenceInUse(LICENCE_CORRECTION, "TEST-REF"))
        .thenReturn(true);

    var hasErrors = addLicencePositionStagedPayloadFormValidator
        .hasErrors(addLicencePositionStagedPayloadForm, LICENCE_CORRECTION, bindingResult);

    assertThat(bindingResult.getFieldErrors())
        .filteredOn(fieldError -> "correctionReference.inputValue".equals(fieldError.getField()))
        .extracting(FieldError::getDefaultMessage, FieldError::getCode)
        .containsExactly(
            tuple("Enter a correction reference that has not already been used", "correctionReference.duplicate")
        );

    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_validForm() {
    addLicencePositionStagedPayloadForm.getCorrectionReference().setInputValue("TEST-REF");
    addLicencePositionStagedPayloadForm.getPositionDate().setDate(TODAY);

    when(licencePositionStagedPayloadService.isCorrectionReferenceInUse(LICENCE_CORRECTION, "TEST-REF"))
        .thenReturn(false);

    var hasErrors = addLicencePositionStagedPayloadFormValidator
        .hasErrors(addLicencePositionStagedPayloadForm, LICENCE_CORRECTION, bindingResult);

    assertThat(hasErrors).isFalse();
  }
}