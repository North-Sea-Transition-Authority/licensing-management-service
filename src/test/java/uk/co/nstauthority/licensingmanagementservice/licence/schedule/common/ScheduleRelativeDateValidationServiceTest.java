package uk.co.nstauthority.licensingmanagementservice.licence.schedule.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.MapBindingResult;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceSchedulePhaseTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleTermTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class ScheduleRelativeDateValidationServiceTest {

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @InjectMocks
  private ScheduleRelativeDateValidationService service;

  private final LicenceScheduleDetail licenceScheduleDetail = new LicenceScheduleDetail();

  @Test
  void validateRelativeDateBeforeEndOfSchedule_whenNoTerms_noErrors() {
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of());

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateRelativeDateBeforeEndOfSchedule(
        licenceScheduleDetail,
        createDuration("relativeDuration", new ThreeFieldDuration(1, 0, 0)),
        UUID.randomUUID(),
        errors
    );

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validateRelativeDateBeforeEndOfSchedule_relativeDateBeforeFinalTermEnd_relativeToTerm_noErrors() {
    var termId = UUID.randomUUID();
    var termStartDate = LocalDate.of(2020, 1, 1);
    var finalTermEndDate = LocalDate.of(2030, 12, 31);

    var initialTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)
        .withStartDate(LocalDate.of(2015, 1, 1))
        .withEndDate(LocalDate.of(2019, 12, 31))
        .build();

    var finalTerm = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.SECOND)
        .withStartDate(termStartDate)
        .withEndDate(finalTermEndDate)
        .build();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(initialTerm, finalTerm));

    when(licenceScheduleCalculationService.calculateRelativeStartDueDate(eq(termStartDate), any(ThreeFieldDuration.class)))
        .thenReturn(LocalDate.of(2025, 1, 1));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateRelativeDateBeforeEndOfSchedule(
        licenceScheduleDetail,
        createDuration("relativeDuration", new ThreeFieldDuration(5, 0, 0)),
        termId,
        errors
    );

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validateRelativeDateBeforeEndOfSchedule_relativeDateOnFinalTermEnd_relativeToTerm_rejectsFields() {
    var termId = UUID.randomUUID();
    var termStartDate = LocalDate.of(2020, 1, 1);
    var finalTermEndDate = LocalDate.of(2030, 12, 31);

    var term = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.INITIAL)
        .withStartDate(termStartDate)
        .withEndDate(finalTermEndDate)
        .build();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(term));

    when(licenceScheduleCalculationService.calculateRelativeStartDueDate(eq(termStartDate), any(ThreeFieldDuration.class)))
        .thenReturn(finalTermEndDate);

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateRelativeDateBeforeEndOfSchedule(
        licenceScheduleDetail,
        createDuration("relativeDuration", new ThreeFieldDuration(10, 11, 30)),
        termId,
        errors
    );

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("relativeDuration.years"))
        .contains("Relative event date cannot occur after the end of the schedule");
    assertThat(errorMessages).containsKey("relativeDuration.months");
    assertThat(errorMessages).containsKey("relativeDuration.days");
  }

  @Test
  void validateRelativeDateBeforeEndOfSchedule_relativeDateAfterFinalTermEnd_relativeToTerm_rejectsFields() {
    var termId = UUID.randomUUID();
    var termStartDate = LocalDate.of(2020, 1, 1);
    var finalTermEndDate = LocalDate.of(2025, 12, 31);

    var term = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.INITIAL)
        .withStartDate(termStartDate)
        .withEndDate(finalTermEndDate)
        .build();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(term));

    when(licenceScheduleCalculationService.calculateRelativeStartDueDate(eq(termStartDate), any(ThreeFieldDuration.class)))
        .thenReturn(LocalDate.of(2026, 1, 1));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateRelativeDateBeforeEndOfSchedule(
        licenceScheduleDetail,
        createDuration("relativeDuration", new ThreeFieldDuration(6, 0, 0)),
        termId,
        errors
    );

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("relativeDuration.years"))
        .contains("Relative event date cannot occur after the end of the schedule");
    assertThat(errorMessages).containsKey("relativeDuration.months");
    assertThat(errorMessages).containsKey("relativeDuration.days");
  }

  @Test
  void validateRelativeDateBeforeEndOfSchedule_relativeDateBeforeFinalTermEnd_relativeToPhase_noErrors() {
    var phaseId = UUID.randomUUID();
    var phaseStartDate = LocalDate.of(2021, 6, 1);
    var finalTermEndDate = LocalDate.of(2030, 12, 31);

    var term = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)
        .withStartDate(LocalDate.of(2020, 1, 1))
        .withEndDate(finalTermEndDate)
        .build();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(term));

    var phase = LicenceSchedulePhaseTestUtil.builder()
        .withId(phaseId)
        .withStartDate(phaseStartDate)
        .build();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);
    when(licenceScheduleCalculationService.calculateRelativeStartDueDate(eq(phaseStartDate), any(ThreeFieldDuration.class)))
        .thenReturn(LocalDate.of(2025, 1, 1));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateRelativeDateBeforeEndOfSchedule(
        licenceScheduleDetail,
        createDuration("relativeDuration", new ThreeFieldDuration(1, 0, 0)),
        phaseId,
        errors
    );

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validateRelativeDateBeforeEndOfSchedule_relativeDateAfterFinalTermEnd_relativeToPhase_rejectsFields() {
    var phaseId = UUID.randomUUID();
    var phaseStartDate = LocalDate.of(2021, 6, 1);
    var finalTermEndDate = LocalDate.of(2025, 12, 31);

    var term = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)
        .withStartDate(LocalDate.of(2020, 1, 1))
        .withEndDate(finalTermEndDate)
        .build();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(term));

    var phase = LicenceSchedulePhaseTestUtil.builder()
        .withId(phaseId)
        .withStartDate(phaseStartDate)
        .build();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);
    when(licenceScheduleCalculationService.calculateRelativeStartDueDate(eq(phaseStartDate), any(ThreeFieldDuration.class)))
        .thenReturn(LocalDate.of(2026, 6, 1));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateRelativeDateBeforeEndOfSchedule(
        licenceScheduleDetail,
        createDuration("relativeDuration", new ThreeFieldDuration(5, 0, 0)),
        phaseId,
        errors
    );

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("relativeDuration.years"))
        .contains("Relative event date cannot occur after the end of the schedule");
    assertThat(errorMessages).containsKey("relativeDuration.months");
    assertThat(errorMessages).containsKey("relativeDuration.days");
  }

  @Test
  void validateRelativeDateBeforeEndOfSchedule_withMultipleTerms_usesFinalTermEndDate() {
    var termId = UUID.randomUUID();
    var finalTermStartDate = LocalDate.of(2025, 1, 1);
    var finalTermEndDate = LocalDate.of(2035, 12, 31);

    var initialTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)  // displayOrder 10
        .withStartDate(LocalDate.of(2015, 1, 1))
        .withEndDate(LocalDate.of(2019, 12, 31))
        .build();

    var secondTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.SECOND)  // displayOrder 20
        .withStartDate(LocalDate.of(2020, 1, 1))
        .withEndDate(LocalDate.of(2024, 12, 31))
        .build();

    var thirdTerm = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.THIRD)  // displayOrder 30 — highest, so final
        .withStartDate(finalTermStartDate)
        .withEndDate(finalTermEndDate)
        .build();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(initialTerm, secondTerm, thirdTerm));

    // Relative date falls after the non-final terms' end dates but before the final term end
    when(licenceScheduleCalculationService.calculateRelativeStartDueDate(eq(finalTermStartDate), any(ThreeFieldDuration.class)))
        .thenReturn(LocalDate.of(2030, 1, 1));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateRelativeDateBeforeEndOfSchedule(
        licenceScheduleDetail,
        createDuration("relativeDuration", new ThreeFieldDuration(5, 0, 0)),
        termId,
        errors
    );

    assertThat(errors.hasErrors()).isFalse();
  }

  private ThreeFieldDurationInput createDuration(String fieldName, ThreeFieldDuration duration) {
    var input = new ThreeFieldDurationInput(fieldName, fieldName);
    input.setFromThreeFieldDuration(duration);
    return input;
  }
}
