package uk.co.nstauthority.licensingmanagementservice.licence.schedule.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeFeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.StartEndDates;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseForm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateForm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateRelativeDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class ScheduleRelativeDateValidationServiceTest {

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @Mock
  private LicenceStartDateService licenceStartDateService;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;

  @Mock
  private LicenceScheduleRateService licenceScheduleRateService;

  @Mock
  private OtherScheduleEventService otherScheduleEventService;

  @Mock
  private LicenceTypeFeatureService licenceTypeFeatureService;

  @Mock
  private LicenceScheduleExpiryService licenceScheduleExpiryService;

  @InjectMocks
  private ScheduleRelativeDateValidationService service;

  private final LicenceScheduleDetail licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
      LicenceScheduleTestUtil.createLicenceSchedule(LicenceTestUtil.builder().build())
  );

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

  @Test
  void validateTermLengthUpdate_whenDurationNotShortened_sameDuration_noErrors() {
    var licenceScheduleTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.INITIAL)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateTermLengthUpdate(
        licenceScheduleTerm,
        createDuration("termDuration", new ThreeFieldDuration(5, 0, 0)),
        errors
    );

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validateTermLengthUpdate_whenDurationIncreased_noErrors() {
    var licenceScheduleTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.INITIAL)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateTermLengthUpdate(
        licenceScheduleTerm,
        createDuration("termDuration", new ThreeFieldDuration(6, 0, 0)),
        errors
    );

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validateTermLengthUpdate_whenDurationShortened_noInvalidEntities_noErrors() {
    var termId = UUID.randomUUID();

    var licenceScheduleTerm = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.INITIAL)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var newEndDate = setupShortenedTermMocks(licenceScheduleTerm);

    when(workProgrammeActivityService.getWorkProgrammeActivitiesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of());
    when(licenceScheduleRateService.getRatesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of());
    when(otherScheduleEventService.getEventsAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of());

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateTermLengthUpdate(
        licenceScheduleTerm,
        createDuration("termDuration", new ThreeFieldDuration(4, 0, 0)),
        errors
    );

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validateTermLengthUpdate_whenDurationShortened_withInvalidActivities_rejectsFields() {
    var termId = UUID.randomUUID();

    var licenceScheduleTerm = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.INITIAL)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var newEndDate = setupShortenedTermMocks(licenceScheduleTerm);

    when(workProgrammeActivityService.getWorkProgrammeActivitiesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of(new WorkProgrammeActivity()));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateTermLengthUpdate(
        licenceScheduleTerm,
        createDuration("termDuration", new ThreeFieldDuration(4, 0, 0)),
        errors
    );

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("termDuration.years"))
        .contains("The term duration cannot be reduced as this would cause events to occur after the end of the final term");
    assertThat(errorMessages).containsKey("termDuration.months");
    assertThat(errorMessages).containsKey("termDuration.days");
  }

  @Test
  void validateTermLengthUpdate_whenDurationShortened_withInvalidRates_rejectsFields() {
    var termId = UUID.randomUUID();

    var licenceScheduleTerm = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.INITIAL)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var newEndDate = setupShortenedTermMocks(licenceScheduleTerm);

    when(workProgrammeActivityService.getWorkProgrammeActivitiesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of());
    when(licenceScheduleRateService.getRatesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of(new LicenceScheduleRate()));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateTermLengthUpdate(
        licenceScheduleTerm,
        createDuration("termDuration", new ThreeFieldDuration(4, 0, 0)),
        errors
    );

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("termDuration.years"))
        .contains("The term duration cannot be reduced as this would cause events to occur after the end of the final term");
    assertThat(errorMessages).containsKey("termDuration.months");
    assertThat(errorMessages).containsKey("termDuration.days");
  }

  @Test
  void validateTermLengthUpdate_whenDurationShortened_withInvalidEvents_rejectsFields() {
    var termId = UUID.randomUUID();

    var licenceScheduleTerm = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.INITIAL)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var newEndDate = setupShortenedTermMocks(licenceScheduleTerm);

    when(workProgrammeActivityService.getWorkProgrammeActivitiesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of());
    when(licenceScheduleRateService.getRatesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of());
    when(otherScheduleEventService.getEventsAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of(new OtherScheduleEvent()));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateTermLengthUpdate(
        licenceScheduleTerm,
        createDuration("termDuration", new ThreeFieldDuration(4, 0, 0)),
        errors
    );

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("termDuration.years"))
        .contains("The term duration cannot be reduced as this would cause events to occur after the end of the final term");
    assertThat(errorMessages).containsKey("termDuration.months");
    assertThat(errorMessages).containsKey("termDuration.days");
  }

  @Test
  void validateTermLengthUpdate_whenMonthsDecreased_sameYears_withInvalidActivities_rejectsFields() {
    var termId = UUID.randomUUID();

    var licenceScheduleTerm = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.INITIAL)
        .withTermDuration(new ThreeFieldDuration(5, 6, 0))
        .build();

    var newEndDate = setupShortenedTermMocks(licenceScheduleTerm);

    when(workProgrammeActivityService.getWorkProgrammeActivitiesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of(new WorkProgrammeActivity()));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    // Same years (5), reduced months (6 → 5)
    service.validateTermLengthUpdate(
        licenceScheduleTerm,
        createDuration("termDuration", new ThreeFieldDuration(5, 5, 0)),
        errors
    );

    assertThat(errors.hasErrors()).isTrue();
  }

  @Test
  void validateTermLengthUpdate_whenDaysDecreased_sameYearsAndMonths_withInvalidActivities_rejectsFields() {
    var termId = UUID.randomUUID();

    var licenceScheduleTerm = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.INITIAL)
        .withTermDuration(new ThreeFieldDuration(5, 0, 10))
        .build();

    var newEndDate = setupShortenedTermMocks(licenceScheduleTerm);

    when(workProgrammeActivityService.getWorkProgrammeActivitiesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of(new WorkProgrammeActivity()));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    // Same years (5) and months (0), reduced days (10 → 9)
    service.validateTermLengthUpdate(
        licenceScheduleTerm,
        createDuration("termDuration", new ThreeFieldDuration(5, 0, 9)),
        errors
    );

    assertThat(errors.hasErrors()).isTrue();
  }

  @Test
  void isTermRemovalValid_whenNoOtherTermsRemain_returnsTrue() {
    var licenceScheduleTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.INITIAL)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(licenceScheduleTerm));

    assertThat(service.isTermRemovalValid(licenceScheduleTerm)).isTrue();
  }

  @Test
  void isTermRemovalValid_whenNoInvalidEntities_returnsTrue() {
    var termToRemove = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.INITIAL)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var remainingTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.SECOND)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var newEndDate = setupTermRemovalMocks(termToRemove, remainingTerm);

    when(workProgrammeActivityService.getWorkProgrammeActivitiesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of());
    when(licenceScheduleRateService.getRatesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of());
    when(otherScheduleEventService.getEventsAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of());

    assertThat(service.isTermRemovalValid(termToRemove)).isTrue();
  }

  @Test
  void isTermRemovalValid_whenInvalidActivities_returnsFalse() {
    var termToRemove = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.INITIAL)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var remainingTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.SECOND)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var newEndDate = setupTermRemovalMocks(termToRemove, remainingTerm);

    when(workProgrammeActivityService.getWorkProgrammeActivitiesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of(new WorkProgrammeActivity()));

    assertThat(service.isTermRemovalValid(termToRemove)).isFalse();
  }

  @Test
  void isTermRemovalValid_whenInvalidRates_returnsFalse() {
    var termToRemove = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.INITIAL)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var remainingTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.SECOND)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var newEndDate = setupTermRemovalMocks(termToRemove, remainingTerm);

    when(workProgrammeActivityService.getWorkProgrammeActivitiesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of());
    when(licenceScheduleRateService.getRatesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of(new LicenceScheduleRate()));

    assertThat(service.isTermRemovalValid(termToRemove)).isFalse();
  }

  @Test
  void isTermRemovalValid_whenInvalidEvents_returnsFalse() {
    var termToRemove = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.INITIAL)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var remainingTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withLicenceScheduleDetail(licenceScheduleDetail)
        .withTermType(TermType.SECOND)
        .withTermDuration(new ThreeFieldDuration(5, 0, 0))
        .build();

    var newEndDate = setupTermRemovalMocks(termToRemove, remainingTerm);

    when(workProgrammeActivityService.getWorkProgrammeActivitiesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of());
    when(licenceScheduleRateService.getRatesAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of());
    when(otherScheduleEventService.getEventsAfterDate(licenceScheduleDetail, newEndDate))
        .thenReturn(List.of(new OtherScheduleEvent()));

    assertThat(service.isTermRemovalValid(termToRemove)).isFalse();
  }

  @Test
  void validateTermRateOverlap_emptyRatesMap_noErrors() {
    var termId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setLicenceScheduleTermId(termId.toString());

    var term = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.INITIAL)
        .withStartDate(LocalDate.of(2020, 1, 1))
        .withEndDate(LocalDate.of(2025, 12, 31))
        .build();

    when(licenceScheduleTermService.getTermByIdOrThrow(termId)).thenReturn(term);
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(new LinkedHashMap<>());

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateTermRateOverlap(null, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validateTermRateOverlap_rateDoesNotEncompassTerm_noErrors() {
    var termId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setLicenceScheduleTermId(termId.toString());

    var term = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.INITIAL)
        .withStartDate(LocalDate.of(2020, 1, 1))
        .withEndDate(LocalDate.of(2025, 12, 31))
        .build();

    when(licenceScheduleTermService.getTermByIdOrThrow(termId)).thenReturn(term);

    var ratesMap = new LinkedHashMap<UUID, StartEndDates>();
    ratesMap.put(UUID.randomUUID(), new StartEndDates(LocalDate.of(2021, 1, 1), LocalDate.of(2030, 12, 31)));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(ratesMap);

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateTermRateOverlap(null, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validateTermRateOverlap_rateEncompassesTerm_noExistingRate_rejectsField() {
    var termId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setLicenceScheduleTermId(termId.toString());

    var term = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.INITIAL)
        .withStartDate(LocalDate.of(2020, 1, 1))
        .withEndDate(LocalDate.of(2025, 12, 31))
        .build();

    when(licenceScheduleTermService.getTermByIdOrThrow(termId)).thenReturn(term);

    var ratesMap = new LinkedHashMap<UUID, StartEndDates>();
    ratesMap.put(UUID.randomUUID(), new StartEndDates(LocalDate.of(2019, 1, 1), LocalDate.of(2026, 12, 31)));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(ratesMap);

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateTermRateOverlap(null, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("licenceScheduleTermId"))
        .contains("A rate cannot be added for this term as there are already rates that exist within it");
  }

  @Test
  void validateTermRateOverlap_rateEncompassesTerm_existingRateIsOverlapping_noErrors() {
    var termId = UUID.randomUUID();
    var existingRateId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setLicenceScheduleTermId(termId.toString());

    var term = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.INITIAL)
        .withStartDate(LocalDate.of(2020, 1, 1))
        .withEndDate(LocalDate.of(2025, 12, 31))
        .build();

    when(licenceScheduleTermService.getTermByIdOrThrow(termId)).thenReturn(term);

    var ratesMap = new LinkedHashMap<UUID, StartEndDates>();
    ratesMap.put(existingRateId, new StartEndDates(LocalDate.of(2019, 1, 1), LocalDate.of(2026, 12, 31)));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(ratesMap);

    var existingRate = new LicenceScheduleRate();
    existingRate.setId(existingRateId);

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateTermRateOverlap(existingRate, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validateTermRateOverlap_rateEncompassesTerm_existingRateIsDifferent_rejectsField() {
    var termId = UUID.randomUUID();
    var overlappingRateId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setLicenceScheduleTermId(termId.toString());

    var term = LicenceScheduleTermTestUtil.builder()
        .withId(termId)
        .withTermType(TermType.INITIAL)
        .withStartDate(LocalDate.of(2020, 1, 1))
        .withEndDate(LocalDate.of(2025, 12, 31))
        .build();

    when(licenceScheduleTermService.getTermByIdOrThrow(termId)).thenReturn(term);

    var ratesMap = new LinkedHashMap<UUID, StartEndDates>();
    ratesMap.put(overlappingRateId, new StartEndDates(LocalDate.of(2019, 1, 1), LocalDate.of(2026, 12, 31)));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(ratesMap);

    var editingRate = new LicenceScheduleRate();
    editingRate.setId(UUID.randomUUID());

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateTermRateOverlap(editingRate, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isTrue();
  }

  @Test
  void validatePhaseRateOverlap_emptyRatesMap_noErrors() {
    var phaseId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setLicenceSchedulePhaseId(phaseId.toString());

    var phase = LicenceSchedulePhaseTestUtil.builder()
        .withId(phaseId)
        .withStartDate(LocalDate.of(2021, 1, 1))
        .withEndDate(LocalDate.of(2023, 12, 31))
        .build();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(new LinkedHashMap<>());

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validatePhaseRateOverlap(null, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validatePhaseRateOverlap_rateDoesNotEncompassPhase_noErrors() {
    var phaseId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setLicenceSchedulePhaseId(phaseId.toString());

    var phase = LicenceSchedulePhaseTestUtil.builder()
        .withId(phaseId)
        .withStartDate(LocalDate.of(2021, 1, 1))
        .withEndDate(LocalDate.of(2023, 12, 31))
        .build();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);

    var ratesMap = new LinkedHashMap<UUID, StartEndDates>();
    ratesMap.put(UUID.randomUUID(), new StartEndDates(LocalDate.of(2022, 1, 1), LocalDate.of(2025, 12, 31)));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(ratesMap);

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validatePhaseRateOverlap(null, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validatePhaseRateOverlap_rateEncompassesPhase_noExistingRate_rejectsField() {
    var phaseId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setLicenceSchedulePhaseId(phaseId.toString());

    var phase = LicenceSchedulePhaseTestUtil.builder()
        .withId(phaseId)
        .withStartDate(LocalDate.of(2021, 1, 1))
        .withEndDate(LocalDate.of(2023, 12, 31))
        .build();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);

    var ratesMap = new LinkedHashMap<UUID, StartEndDates>();
    ratesMap.put(UUID.randomUUID(), new StartEndDates(LocalDate.of(2020, 1, 1), LocalDate.of(2025, 12, 31)));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(ratesMap);

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validatePhaseRateOverlap(null, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("licenceSchedulePhaseId"))
        .contains("A rate cannot be added for this phase as it would overlap existing rates");
  }

  @Test
  void validatePhaseRateOverlap_rateEncompassesPhase_existingRateIsOverlapping_noErrors() {
    var phaseId = UUID.randomUUID();
    var existingRateId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setLicenceSchedulePhaseId(phaseId.toString());

    var phase = LicenceSchedulePhaseTestUtil.builder()
        .withId(phaseId)
        .withStartDate(LocalDate.of(2021, 1, 1))
        .withEndDate(LocalDate.of(2023, 12, 31))
        .build();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);

    var ratesMap = new LinkedHashMap<UUID, StartEndDates>();
    ratesMap.put(existingRateId, new StartEndDates(LocalDate.of(2020, 1, 1), LocalDate.of(2025, 12, 31)));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(ratesMap);

    var existingRate = new LicenceScheduleRate();
    existingRate.setId(existingRateId);

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validatePhaseRateOverlap(existingRate, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validatePhaseRateOverlap_rateEncompassesPhase_existingRateIsDifferent_rejectsField() {
    var phaseId = UUID.randomUUID();
    var overlappingRateId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setLicenceSchedulePhaseId(phaseId.toString());

    var phase = LicenceSchedulePhaseTestUtil.builder()
        .withId(phaseId)
        .withStartDate(LocalDate.of(2021, 1, 1))
        .withEndDate(LocalDate.of(2023, 12, 31))
        .build();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);

    var ratesMap = new LinkedHashMap<UUID, StartEndDates>();
    ratesMap.put(overlappingRateId, new StartEndDates(LocalDate.of(2020, 1, 1), LocalDate.of(2025, 12, 31)));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(ratesMap);

    var editingRate = new LicenceScheduleRate();
    editingRate.setId(UUID.randomUUID());

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validatePhaseRateOverlap(editingRate, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isTrue();
  }

  @Test
  void validateRelativeRateOverlap_emptyRatesMap_noErrors() {
    var relativeEventId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);
    form.setRelativeEventId(relativeEventId.toString());

    var term = LicenceScheduleTermTestUtil.builder()
        .withId(relativeEventId)
        .withTermType(TermType.INITIAL)
        .withStartDate(LocalDate.of(2020, 1, 1))
        .withEndDate(LocalDate.of(2025, 12, 31))
        .build();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(term));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(new LinkedHashMap<>());
    when(licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail))
        .thenReturn(List.of());

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateRelativeRateOverlap(null, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validateRelativeRateOverlap_sameStartDate_noExistingRate_onStartDate_rejectsRelativeDateOption() {
    var relativeEventId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);
    form.setRelativeEventId(relativeEventId.toString());

    var termStartDate = LocalDate.of(2020, 1, 1);
    var term = LicenceScheduleTermTestUtil.builder()
        .withId(relativeEventId)
        .withTermType(TermType.INITIAL)
        .withStartDate(termStartDate)
        .withEndDate(LocalDate.of(2025, 12, 31))
        .build();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(term));

    var ratesMap = new LinkedHashMap<UUID, StartEndDates>();
    ratesMap.put(UUID.randomUUID(), new StartEndDates(termStartDate, LocalDate.of(2025, 12, 31)));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(ratesMap);

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateRelativeRateOverlap(null, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("rateRelativeDateOption"))
        .contains("A rate cannot be added on this date as there is already a rate on the schedule with the same start date");
  }

  @Test
  void validateRelativeRateOverlap_sameStartDate_noExistingRate_relativeToStartDate_rejectsDurationFields() {
    var relativeEventId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setRateRelativeDateOption(RateRelativeDateOption.RELATIVE_TO_START_DATE);
    form.setRelativeEventId(relativeEventId.toString());
    form.getRelativeDuration().setFromThreeFieldDuration(new ThreeFieldDuration(2, 0, 0));

    var termStartDate = LocalDate.of(2020, 1, 1);
    var term = LicenceScheduleTermTestUtil.builder()
        .withId(relativeEventId)
        .withTermType(TermType.INITIAL)
        .withStartDate(termStartDate)
        .withEndDate(LocalDate.of(2025, 12, 31))
        .build();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(term));

    var calculatedStartDate = LocalDate.of(2022, 1, 1);
    when(licenceScheduleCalculationService.calculateRelativeStartDueDate(eq(termStartDate), any(ThreeFieldDuration.class)))
        .thenReturn(calculatedStartDate);

    var ratesMap = new LinkedHashMap<UUID, StartEndDates>();
    ratesMap.put(UUID.randomUUID(), new StartEndDates(calculatedStartDate, LocalDate.of(2025, 12, 31)));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(ratesMap);

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateRelativeRateOverlap(null, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("relativeDuration.years"))
        .contains("A rate cannot be added on this date as there is already a rate on the schedule with the same start date");
    assertThat(errorMessages).containsKey("relativeDuration.months");
    assertThat(errorMessages).containsKey("relativeDuration.days");
  }

  @Test
  void validateRelativeRateOverlap_sameStartDate_existingRateHasSameStartDate_noErrors() {
    var relativeEventId = UUID.randomUUID();
    var existingRateId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);
    form.setRelativeEventId(relativeEventId.toString());

    var termStartDate = LocalDate.of(2020, 1, 1);
    var term = LicenceScheduleTermTestUtil.builder()
        .withId(relativeEventId)
        .withTermType(TermType.INITIAL)
        .withStartDate(termStartDate)
        .withEndDate(LocalDate.of(2025, 12, 31))
        .build();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(term));

    var ratesMap = new LinkedHashMap<UUID, StartEndDates>();
    ratesMap.put(existingRateId, new StartEndDates(termStartDate, LocalDate.of(2025, 12, 31)));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(ratesMap);

    var existingRate = new LicenceScheduleRate();
    existingRate.setId(existingRateId);
    existingRate.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);

    when(licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail))
        .thenReturn(List.of(existingRate));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateRelativeRateOverlap(existingRate, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validateRelativeRateOverlap_startDateWithinExistingTermOrPhaseRate_noExistingRate_rejectsField() {
    var relativeEventId = UUID.randomUUID();
    var existingRateId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);
    form.setRelativeEventId(relativeEventId.toString());

    var termStartDate = LocalDate.of(2022, 6, 1);
    var term = LicenceScheduleTermTestUtil.builder()
        .withId(relativeEventId)
        .withTermType(TermType.INITIAL)
        .withStartDate(termStartDate)
        .withEndDate(LocalDate.of(2025, 12, 31))
        .build();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(term));

    // Existing rate covers 2020-2030; the proposed start date (2022-06-01) falls within it
    var ratesMap = new LinkedHashMap<UUID, StartEndDates>();
    ratesMap.put(existingRateId, new StartEndDates(LocalDate.of(2020, 1, 1), LocalDate.of(2030, 12, 31)));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(ratesMap);

    var existingRate = new LicenceScheduleRate();
    existingRate.setId(existingRateId);
    existingRate.setRateDefinitionOption(RateDefinitionOption.TERM);

    when(licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail))
        .thenReturn(List.of(existingRate));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateRelativeRateOverlap(null, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("rateRelativeDateOption"))
        .contains("A rate cannot be added on this date as there is already a rate defined for the term/phase the start date is within");
  }

  @Test
  void validateRelativeRateOverlap_startDateWithinExistingCustomPeriodRate_noErrors() {
    var relativeEventId = UUID.randomUUID();
    var existingRateId = UUID.randomUUID();
    var form = new LicenceScheduleRateForm();
    form.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);
    form.setRelativeEventId(relativeEventId.toString());

    var termStartDate = LocalDate.of(2022, 6, 1);
    var term = LicenceScheduleTermTestUtil.builder()
        .withId(relativeEventId)
        .withTermType(TermType.INITIAL)
        .withStartDate(termStartDate)
        .withEndDate(LocalDate.of(2025, 12, 31))
        .build();

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(term));

    var ratesMap = new LinkedHashMap<UUID, StartEndDates>();
    ratesMap.put(existingRateId, new StartEndDates(LocalDate.of(2020, 1, 1), LocalDate.of(2030, 12, 31)));
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(ratesMap);

    var existingRate = new LicenceScheduleRate();
    existingRate.setId(existingRateId);
    existingRate.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);

    when(licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail))
        .thenReturn(List.of(existingRate));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validateRelativeRateOverlap(null, licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validatePhaseLengthUpdate_newPhase_endsWithinInitialTerm_noErrors() {
    var initialTermStartDate = LocalDate.of(2020, 1, 1);
    var initialTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)
        .withStartDate(initialTermStartDate)
        .withEndDate(LocalDate.of(2025, 12, 31))
        .build();

    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of());
    when(licenceScheduleTermService.getTermByLicenceScheduleDetailAndTermTypeOrThrow(licenceScheduleDetail, TermType.INITIAL))
        .thenReturn(initialTerm);
    when(licenceScheduleCalculationService.calculateDurationEndDate(eq(initialTermStartDate), any(ThreeFieldDuration.class)))
        .thenReturn(LocalDate.of(2020, 12, 31));

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    form.getPhaseDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1, 0, 0));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validatePhaseLengthUpdate(licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validatePhaseLengthUpdate_newPhase_endsAfterInitialTerm_rejectsFields() {
    var initialTermStartDate = LocalDate.of(2020, 1, 1);
    var initialTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)
        .withStartDate(initialTermStartDate)
        .withEndDate(LocalDate.of(2020, 12, 31))
        .build();

    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of());
    when(licenceScheduleTermService.getTermByLicenceScheduleDetailAndTermTypeOrThrow(licenceScheduleDetail, TermType.INITIAL))
        .thenReturn(initialTerm);
    when(licenceScheduleCalculationService.calculateDurationEndDate(eq(initialTermStartDate), any(ThreeFieldDuration.class)))
        .thenReturn(LocalDate.of(2021, 1, 1));

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    form.getPhaseDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1, 0, 0));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validatePhaseLengthUpdate(licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("phaseDuration.years"))
        .contains("A phase cannot be added with this duration as this would cause a phase to end after the end "
            + "of the initial term");
    assertThat(errorMessages).containsKey("phaseDuration.months");
    assertThat(errorMessages).containsKey("phaseDuration.days");
  }

  @Test
  void validatePhaseLengthUpdate_existingPhase_notLengthened_noErrors() {
    var existingPhase = LicenceSchedulePhaseTestUtil.builder()
        .withId(UUID.randomUUID())
        .withPhaseType(PhaseType.PHASE_A)
        .withPhaseDuration(new ThreeFieldDuration(2, 0, 0))
        .build();

    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(existingPhase));

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    form.getPhaseDuration().setFromThreeFieldDuration(new ThreeFieldDuration(2, 0, 0));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validatePhaseLengthUpdate(licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validatePhaseLengthUpdate_existingPhase_shortened_noErrors() {
    var existingPhase = LicenceSchedulePhaseTestUtil.builder()
        .withId(UUID.randomUUID())
        .withPhaseType(PhaseType.PHASE_A)
        .withPhaseDuration(new ThreeFieldDuration(2, 0, 0))
        .build();

    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(existingPhase));

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    form.getPhaseDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1, 0, 0));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validatePhaseLengthUpdate(licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validatePhaseLengthUpdate_existingPhase_lengthenedByYearsOnly_endsAfterInitialTerm_rejectsFields() {
    var initialTermStartDate = LocalDate.of(2020, 1, 1);
    var initialTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)
        .withStartDate(initialTermStartDate)
        .withEndDate(LocalDate.of(2020, 12, 31))
        .build();

    var existingPhase = LicenceSchedulePhaseTestUtil.builder()
        .withId(UUID.randomUUID())
        .withPhaseType(PhaseType.PHASE_A)
        .withPhaseDuration(new ThreeFieldDuration(1, 0, 0))
        .build();

    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(existingPhase));
    when(licenceScheduleTermService.getTermByLicenceScheduleDetailAndTermTypeOrThrow(licenceScheduleDetail, TermType.INITIAL))
        .thenReturn(initialTerm);
    when(licenceScheduleCalculationService.calculateDurationEndDate(eq(initialTermStartDate), any(ThreeFieldDuration.class)))
        .thenReturn(LocalDate.of(2022, 1, 1));

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    // Lengthened by a whole year, months/days unchanged
    form.getPhaseDuration().setFromThreeFieldDuration(new ThreeFieldDuration(2, 0, 0));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validatePhaseLengthUpdate(licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("phaseDuration.years"))
        .contains("The phase duration cannot be increased as this would cause a phase to end after the end "
            + "of the initial term");
  }

  @Test
  void validatePhaseLengthUpdate_existingPhase_lengthened_endsWithinInitialTerm_noErrors() {
    var initialTermStartDate = LocalDate.of(2020, 1, 1);
    var initialTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)
        .withStartDate(initialTermStartDate)
        .withEndDate(LocalDate.of(2021, 12, 31))
        .build();

    var existingPhase = LicenceSchedulePhaseTestUtil.builder()
        .withId(UUID.randomUUID())
        .withPhaseType(PhaseType.PHASE_A)
        .withPhaseDuration(new ThreeFieldDuration(1, 0, 0))
        .build();

    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(existingPhase));
    when(licenceScheduleTermService.getTermByLicenceScheduleDetailAndTermTypeOrThrow(licenceScheduleDetail, TermType.INITIAL))
        .thenReturn(initialTerm);
    when(licenceScheduleCalculationService.calculateDurationEndDate(eq(initialTermStartDate), any(ThreeFieldDuration.class)))
        .thenReturn(LocalDate.of(2021, 1, 1));

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    // Lengthened by a single day, keeping years/months unchanged
    form.getPhaseDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1, 0, 1));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validatePhaseLengthUpdate(licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validatePhaseLengthUpdate_existingPhase_lengthened_endsAfterInitialTerm_rejectsFields() {
    var initialTermStartDate = LocalDate.of(2020, 1, 1);
    var initialTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)
        .withStartDate(initialTermStartDate)
        .withEndDate(LocalDate.of(2020, 12, 31))
        .build();

    var existingPhase = LicenceSchedulePhaseTestUtil.builder()
        .withId(UUID.randomUUID())
        .withPhaseType(PhaseType.PHASE_A)
        .withPhaseDuration(new ThreeFieldDuration(1, 0, 0))
        .build();

    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(existingPhase));
    when(licenceScheduleTermService.getTermByLicenceScheduleDetailAndTermTypeOrThrow(licenceScheduleDetail, TermType.INITIAL))
        .thenReturn(initialTerm);
    when(licenceScheduleCalculationService.calculateDurationEndDate(eq(initialTermStartDate), any(ThreeFieldDuration.class)))
        .thenReturn(LocalDate.of(2021, 1, 1));

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    // Lengthened by a single day, keeping years/months unchanged
    form.getPhaseDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1, 0, 1));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validatePhaseLengthUpdate(licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("phaseDuration.years"))
        .contains("The phase duration cannot be increased as this would cause a phase to end after the end of the initial term");
    assertThat(errorMessages).containsKey("phaseDuration.months");
    assertThat(errorMessages).containsKey("phaseDuration.days");
  }

  @Test
  void validatePhaseLengthUpdate_multiplePhases_usesFinalPhaseEndDate_rejectsFields() {
    var initialTermStartDate = LocalDate.of(2020, 1, 1);
    var initialTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)
        .withStartDate(initialTermStartDate)
        // Ends after phase A but before the (updated) phase B end date
        .withEndDate(LocalDate.of(2021, 6, 30))
        .build();

    var phaseA = LicenceSchedulePhaseTestUtil.builder()
        .withId(UUID.randomUUID())
        .withPhaseType(PhaseType.PHASE_A)
        .withPhaseDuration(new ThreeFieldDuration(1, 0, 0))
        .build();

    var phaseB = LicenceSchedulePhaseTestUtil.builder()
        .withId(UUID.randomUUID())
        .withPhaseType(PhaseType.PHASE_B)
        .withPhaseDuration(new ThreeFieldDuration(1, 0, 0))
        .build();

    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(phaseA, phaseB));
    when(licenceScheduleTermService.getTermByLicenceScheduleDetailAndTermTypeOrThrow(licenceScheduleDetail, TermType.INITIAL))
        .thenReturn(initialTerm);

    var phaseAEndDate = LocalDate.of(2020, 12, 31);
    var phaseBStartDate = phaseAEndDate.plusDays(1);
    var phaseBEndDate = LocalDate.of(2022, 1, 1);

    when(licenceScheduleCalculationService.calculateDurationEndDate(
        initialTermStartDate, new ThreeFieldDuration(1, 0, 0)))
        .thenReturn(phaseAEndDate);
    when(licenceScheduleCalculationService.calculateDurationEndDate(
        phaseBStartDate, new ThreeFieldDuration(1, 0, 1)))
        .thenReturn(phaseBEndDate);

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_B);
    // Lengthened by a single day, keeping years/months unchanged
    form.getPhaseDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1, 0, 1));

    var errors = new MapBindingResult(new HashMap<>(), "form");

    service.validatePhaseLengthUpdate(licenceScheduleDetail, form, errors);

    assertThat(errors.hasErrors()).isTrue();
    var errorMessages = ValidatorTestingUtil.extractErrorMessages(errors);
    assertThat(errorMessages.get("phaseDuration.years"))
        .contains("The phase duration cannot be increased as this would cause a phase to end after the end of the initial term");
  }

  @Test
  void doesFinalPhaseEndDateMatchEndOfInitialTerm_whenPhasesNotCapturedForLicenceType_returnsTrue() {
    licenceScheduleDetail.getLicenceSchedule().getLicence().setType(LicenceType.CARBON_STORAGE);

    when(licenceTypeFeatureService.arePhasesCaptured(LicenceType.CARBON_STORAGE)).thenReturn(false);

    var result = service.doesFinalPhaseEndDateMatchEndOfInitialTerm(licenceScheduleDetail);

    assertThat(result).isTrue();
  }

  @Test
  void doesFinalPhaseEndDateMatchEndOfInitialTerm_whenNoPhases_returnsTrue() {
    licenceScheduleDetail.getLicenceSchedule().getLicence().setType(LicenceType.SEAWARD_PRODUCTION);

    when(licenceTypeFeatureService.arePhasesCaptured(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);
    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of());

    var result = service.doesFinalPhaseEndDateMatchEndOfInitialTerm(licenceScheduleDetail);

    assertThat(result).isTrue();
  }

  @Test
  void doesFinalPhaseEndDateMatchEndOfInitialTerm_whenFinalPhaseEndDateMatchesInitialTermEndDate_returnsTrue() {
    licenceScheduleDetail.getLicenceSchedule().getLicence().setType(LicenceType.SEAWARD_PRODUCTION);

    var initialTermEndDate = LocalDate.of(2021, 12, 31);
    var initialTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)
        .withStartDate(LocalDate.of(2020, 1, 1))
        .withEndDate(initialTermEndDate)
        .build();

    var phaseA = LicenceSchedulePhaseTestUtil.builder()
        .withId(UUID.randomUUID())
        .withPhaseType(PhaseType.PHASE_A)
        .withEndDate(LocalDate.of(2020, 12, 31))
        .build();

    var phaseB = LicenceSchedulePhaseTestUtil.builder()
        .withId(UUID.randomUUID())
        .withPhaseType(PhaseType.PHASE_B)
        .withEndDate(initialTermEndDate)
        .build();

    when(licenceTypeFeatureService.arePhasesCaptured(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);
    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(phaseA, phaseB));
    when(licenceScheduleTermService.getTermByLicenceScheduleDetailAndTermTypeOrThrow(licenceScheduleDetail, TermType.INITIAL))
        .thenReturn(initialTerm);

    var result = service.doesFinalPhaseEndDateMatchEndOfInitialTerm(licenceScheduleDetail);

    assertThat(result).isTrue();
  }

  @Test
  void doesFinalPhaseEndDateMatchEndOfInitialTerm_whenFinalPhaseEndDateDoesNotMatchInitialTermEndDate_returnsFalse() {
    licenceScheduleDetail.getLicenceSchedule().getLicence().setType(LicenceType.SEAWARD_PRODUCTION);

    var initialTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)
        .withStartDate(LocalDate.of(2020, 1, 1))
        .withEndDate(LocalDate.of(2021, 12, 31))
        .build();

    var finalPhase = LicenceSchedulePhaseTestUtil.builder()
        .withId(UUID.randomUUID())
        .withPhaseType(PhaseType.PHASE_A)
        .withEndDate(LocalDate.of(2021, 6, 30))
        .build();

    when(licenceTypeFeatureService.arePhasesCaptured(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);
    when(licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(finalPhase));
    when(licenceScheduleTermService.getTermByLicenceScheduleDetailAndTermTypeOrThrow(licenceScheduleDetail, TermType.INITIAL))
        .thenReturn(initialTerm);

    var result = service.doesFinalPhaseEndDateMatchEndOfInitialTerm(licenceScheduleDetail);

    assertThat(result).isFalse();
  }

  @Test
  void doesExpiryDateMatchEndOfFinalTerm_whenNoExpiry_returnsTrue() {
    when(licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(Optional.empty());

    var result = service.doesExpiryDateMatchEndOfFinalTerm(licenceScheduleDetail);

    assertThat(result).isTrue();
  }

  @Test
  void doesExpiryDateMatchEndOfFinalTerm_whenNoTerms_returnsTrue() {
    var expiry = new LicenceScheduleExpiry();
    expiry.setExpiryDate(LocalDate.of(2025, 12, 31));

    when(licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(Optional.of(expiry));
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of());

    var result = service.doesExpiryDateMatchEndOfFinalTerm(licenceScheduleDetail);

    assertThat(result).isTrue();
  }

  @Test
  void doesExpiryDateMatchEndOfFinalTerm_whenExpiryDateMatchesFinalTermEndDate_returnsTrue() {
    var finalTermEndDate = LocalDate.of(2025, 12, 31);

    var expiry = new LicenceScheduleExpiry();
    expiry.setExpiryDate(finalTermEndDate);

    var initialTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)
        .withStartDate(LocalDate.of(2020, 1, 1))
        .withEndDate(LocalDate.of(2024, 12, 31))
        .build();

    var finalTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.SECOND)
        .withStartDate(LocalDate.of(2025, 1, 1))
        .withEndDate(finalTermEndDate)
        .build();

    when(licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(Optional.of(expiry));
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(initialTerm, finalTerm));

    var result = service.doesExpiryDateMatchEndOfFinalTerm(licenceScheduleDetail);

    assertThat(result).isTrue();
  }

  @Test
  void doesExpiryDateMatchEndOfFinalTerm_whenExpiryDateDoesNotMatchFinalTermEndDate_returnsFalse() {
    var expiry = new LicenceScheduleExpiry();
    expiry.setExpiryDate(LocalDate.of(2026, 6, 30));

    var finalTerm = LicenceScheduleTermTestUtil.builder()
        .withId(UUID.randomUUID())
        .withTermType(TermType.INITIAL)
        .withStartDate(LocalDate.of(2020, 1, 1))
        .withEndDate(LocalDate.of(2025, 12, 31))
        .build();

    when(licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(Optional.of(expiry));
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(finalTerm));

    var result = service.doesExpiryDateMatchEndOfFinalTerm(licenceScheduleDetail);

    assertThat(result).isFalse();
  }

  private LocalDate setupShortenedTermMocks(LicenceScheduleTerm licenceScheduleTerm) {
    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(LocalDate.of(2020, 1, 1));
    var newEndDate = LocalDate.of(2023, 12, 31);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(licenceScheduleTerm));
    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail))
        .thenReturn(licenceStartDate);
    when(licenceScheduleCalculationService.calculateDurationEndDate(any(), any()))
        .thenReturn(newEndDate);

    return newEndDate;
  }

  private LocalDate setupTermRemovalMocks(LicenceScheduleTerm termToRemove, LicenceScheduleTerm remainingTerm) {
    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(LocalDate.of(2020, 1, 1));
    var newEndDate = LocalDate.of(2023, 12, 31);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(termToRemove, remainingTerm));
    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail))
        .thenReturn(licenceStartDate);
    when(licenceScheduleCalculationService.calculateDurationEndDate(any(), any()))
        .thenReturn(newEndDate);

    return newEndDate;
  }

  private ThreeFieldDurationInput createDuration(String fieldName, ThreeFieldDuration duration) {
    var input = new ThreeFieldDurationInput(fieldName, fieldName);
    input.setFromThreeFieldDuration(duration);
    return input;
  }
}
