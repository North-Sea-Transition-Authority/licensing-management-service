package uk.co.nstauthority.licensingmanagementservice.licence.schedule.common;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeFeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseForm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateForm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateRelativeDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class ScheduleRelativeDateValidationService {

  private static final String YEAR_FIELD_SUFFIX = ".years";
  private static final String MONTH_FIELD_SUFFIX = ".months";
  private static final String DAY_FIELD_SUFFIX = ".days";
  private static final String INVALID_ERROR_CODE = ".invalid";

  private final LicenceStartDateService licenceStartDateService;

  private final LicenceScheduleTermService licenceScheduleTermService;

  private final LicenceSchedulePhaseService licenceSchedulePhaseService;

  private final LicenceScheduleCalculationService licenceScheduleCalculationService;

  private final WorkProgrammeActivityService workProgrammeActivityService;

  private final LicenceScheduleRateService licenceScheduleRateService;

  private final OtherScheduleEventService otherScheduleEventService;
  private final LicenceTypeFeatureService licenceTypeFeatureService;
  private final LicenceScheduleExpiryService licenceScheduleExpiryService;

  public ScheduleRelativeDateValidationService(
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      LicenceStartDateService licenceStartDateService,
      WorkProgrammeActivityService workProgrammeActivityService,
      LicenceScheduleRateService licenceScheduleRateService,
      OtherScheduleEventService otherScheduleEventService,
      LicenceTypeFeatureService licenceTypeFeatureService,
      LicenceScheduleExpiryService licenceScheduleExpiryService
  ) {
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
    this.licenceStartDateService = licenceStartDateService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.licenceScheduleRateService = licenceScheduleRateService;
    this.otherScheduleEventService = otherScheduleEventService;
    this.licenceTypeFeatureService = licenceTypeFeatureService;
    this.licenceScheduleExpiryService = licenceScheduleExpiryService;
  }

  public void validateRelativeDateBeforeEndOfSchedule(
      LicenceScheduleDetail licenceScheduleDetail,
      ThreeFieldDurationInput duration,
      UUID relativeToId,
      Errors errors
  ) {
    var terms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);

    var finalTerm = terms.stream()
        .max(Comparator.comparing(term -> term.getTermType().getDisplayOrder()));

    if (finalTerm.isEmpty()) {
      return;
    }

    var finalTermEndDate = finalTerm.get().getEndDate();

    var termMap = terms.stream()
        .collect(StreamUtil.toLinkedHashMap(LicenceScheduleTerm::getId, Function.identity()));

    LocalDate durationStartDate;
    if (termMap.containsKey(relativeToId)) {
      durationStartDate = termMap.get(relativeToId).getStartDate();
    } else {
      durationStartDate = licenceSchedulePhaseService.getPhaseByIdOrThrow(relativeToId).getStartDate();
    }

    var relativeDate = licenceScheduleCalculationService.calculateRelativeStartDueDate(
        durationStartDate,
        duration.toThreeFieldDuration()
    );

    if (relativeDate.isAfter(finalTermEndDate) || relativeDate.isEqual(finalTermEndDate)) {
      var fieldName = duration.getFieldName();

      errors.rejectValue(
            fieldName + YEAR_FIELD_SUFFIX,
            INVALID_ERROR_CODE,
            "Relative event date cannot occur after the end of the schedule"
      );

      errors.rejectValue(
          fieldName + MONTH_FIELD_SUFFIX,
              INVALID_ERROR_CODE,
              ""
      );

      errors.rejectValue(
          fieldName + DAY_FIELD_SUFFIX,
              INVALID_ERROR_CODE,
              ""
      );
    }
  }

  public void validateTermLengthUpdate(
      LicenceScheduleTerm licenceScheduleTerm,
      ThreeFieldDurationInput threeFieldDurationInput,
      Errors errors
  ) {
    var licenceScheduleDetail = licenceScheduleTerm.getLicenceScheduleDetail();
    var currentDuration = licenceScheduleTerm.getTermDuration();
    var updatedDuration = threeFieldDurationInput.toThreeFieldDuration();

    if (!isDurationShortened(currentDuration, updatedDuration)) {
      return;
    }

    var updatedFinalTermEndDate = calculateUpdatedFinalTermEndDate(
        licenceScheduleTerm,
        licenceScheduleDetail,
        updatedDuration
    );

    var invalidActivities = workProgrammeActivityService.getWorkProgrammeActivitiesAfterDate(
        licenceScheduleDetail,
        updatedFinalTermEndDate
    );

    if (!invalidActivities.isEmpty()) {
      applyTermLengthValidation(threeFieldDurationInput, errors);

      return;
    }

    var invalidRates = licenceScheduleRateService.getRatesAfterDate(
        licenceScheduleDetail,
        updatedFinalTermEndDate
    );

    if (!invalidRates.isEmpty()) {
      applyTermLengthValidation(threeFieldDurationInput, errors);

      return;
    }

    var invalidEvents = otherScheduleEventService.getEventsAfterDate(
        licenceScheduleDetail,
        updatedFinalTermEndDate
    );

    if (!invalidEvents.isEmpty()) {
      applyTermLengthValidation(threeFieldDurationInput, errors);
    }
  }

  private void applyTermLengthValidation(
      ThreeFieldDurationInput durationInput,
      Errors errors
  ) {
    var fieldName = durationInput.getFieldName();

    errors.rejectValue(
        fieldName + YEAR_FIELD_SUFFIX,
        INVALID_ERROR_CODE,
        "The term duration cannot be reduced as this would cause events to occur after the end of the final term"
    );

    errors.rejectValue(
        fieldName + MONTH_FIELD_SUFFIX,
        INVALID_ERROR_CODE,
        ""
    );

    errors.rejectValue(
        fieldName + DAY_FIELD_SUFFIX,
        INVALID_ERROR_CODE,
        ""
    );
  }

  private LocalDate calculateUpdatedFinalTermEndDate(
      LicenceScheduleTerm licenceScheduleTerm,
      LicenceScheduleDetail licenceScheduleDetail,
      ThreeFieldDuration updatedDuration
  ) {
    var termMap = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)
        .stream()
        .collect(StreamUtil.toLinkedHashMap(LicenceScheduleTerm::getId, Function.identity()));

    termMap.get(licenceScheduleTerm.getId()).setTermDuration(updatedDuration);

    var terms = termMap.values();
    var nextStartDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail).getStartDate();

    for (var term : terms) {
      var endDate = licenceScheduleCalculationService.calculateDurationEndDate(nextStartDate, term.getTermDuration());

      term.setStartDate(nextStartDate);
      term.setEndDate(endDate);

      nextStartDate = endDate.plusDays(1);
    }

    var finalTerm = terms.stream()
        .max(Comparator.comparing(term -> term.getTermType().getDisplayOrder()));

    return finalTerm.map(LicenceScheduleTerm::getEndDate)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "Final term for licenceScheduleDetail with id %s cannot be found".formatted(licenceScheduleDetail.getId())));
  }

  private boolean isDurationShortened(
      ThreeFieldDuration current,
      ThreeFieldDuration updated
  ) {
    return isDurationGreaterThan(current, updated);
  }

  private boolean isDurationGreaterThan(
      ThreeFieldDuration first,
      ThreeFieldDuration second
  ) {
    if (first.years() > second.years()) {
      return true;
    }

    if (first.years().equals(second.years())
        && first.months() > second.months()) {
      return true;
    }

    return first.years().equals(second.years())
        && first.months().equals(second.months())
        && first.days() > second.days();
  }

  public void validateTermRateOverlap(
      LicenceScheduleRate rate,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleRateForm form,
      Errors errors
  ) {
    var linkedToTerm = licenceScheduleTermService.getTermByIdOrThrow(UUID.fromString(form.getLicenceScheduleTermId()));
    validateRateOverlapForDateRange(
        rate,
        licenceScheduleDetail,
        linkedToTerm.getStartDate(),
        linkedToTerm.getEndDate(),
        "licenceScheduleTermId",
        "licenceScheduleTermId.invalid",
        "A rate cannot be added for this term as there are already rates that exist within it",
        errors
    );
  }

  public void validatePhaseRateOverlap(
      LicenceScheduleRate rate,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleRateForm form,
      Errors errors
  ) {
    var linkedToPhase = licenceSchedulePhaseService.getPhaseByIdOrThrow(UUID.fromString(form.getLicenceSchedulePhaseId()));
    validateRateOverlapForDateRange(
        rate,
        licenceScheduleDetail,
        linkedToPhase.getStartDate(),
        linkedToPhase.getEndDate(),
        "licenceSchedulePhaseId",
        "licenceSchedulePhaseId.invalid",
        "A rate cannot be added for this phase as it would overlap existing rates",
        errors
    );
  }

  private void validateRateOverlapForDateRange(
      LicenceScheduleRate rate,
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate startDate,
      LocalDate endDate,
      String fieldName,
      String errorCode,
      String errorMessage,
      Errors errors
  ) {
    var rates = licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail);

    var overlappingRates = rates.entrySet().stream()
        .filter(startEndDates ->
            startEndDates.getValue().startDate().isBefore(startDate)
                || startEndDates.getValue().startDate().isEqual(startDate))
        .filter(startEndDates ->
            startEndDates.getValue().endDate().isAfter(endDate)
                || startEndDates.getValue().endDate().isEqual(endDate))
        .collect(StreamUtil.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

    if (overlappingRates.isEmpty() || rate != null && overlappingRates.containsKey(rate.getId())) {
      return;
    }

    errors.rejectValue(fieldName, errorCode, errorMessage);
  }

  public void validateRelativeRateOverlap(
      LicenceScheduleRate rate,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleRateForm form,
      Errors errors
  ) {
    var ratesDatesMap = licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail);

    var rateStartDate = calculateRateStartDate(licenceScheduleDetail, form);

    var ratesStartingOnDate = ratesDatesMap.entrySet().stream()
        .filter(startEndDates -> startEndDates.getValue().startDate().isEqual(rateStartDate))
        .collect(StreamUtil.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

    if (!ratesStartingOnDate.isEmpty()) {
      if (rate == null || !ratesStartingOnDate.containsKey(rate.getId())) {
        applyRelativeRateValidation(
            "A rate cannot be added on this date as there is already a rate on the schedule with the same start date",
            form,
            errors
        );
        return;
      }
    }

    var rates = licenceScheduleRateService.getLicenceScheduleRates(licenceScheduleDetail).stream()
        .collect(StreamUtil.toLinkedHashMap(LicenceScheduleRate::getId, Function.identity()));

    var overlappingRates = ratesDatesMap.entrySet().stream()
        .filter(startEndDates ->
            rates.get(startEndDates.getKey()).getRateDefinitionOption() != RateDefinitionOption.CUSTOM_PERIOD)
        .filter(startEndDates ->
            startEndDates.getValue().startDate().isBefore(rateStartDate))
        .filter(startEndDates ->
            startEndDates.getValue().endDate().isAfter(rateStartDate)
                || startEndDates.getValue().endDate().isEqual(rateStartDate))

        .collect(StreamUtil.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

    if (overlappingRates.isEmpty()) {
      return;
    }

    applyRelativeRateValidation(
        "A rate cannot be added on this date as there is already a rate defined for the term/phase the start date is within",
        form,
        errors
    );
  }

  private void applyRelativeRateValidation(
      String message,
      LicenceScheduleRateForm form,
      Errors errors
  ) {
    if (form.getRateRelativeDateOption().equals(RateRelativeDateOption.ON_START_DATE)) {
      errors.rejectValue("rateRelativeDateOption",
          "rateRelativeDateOption.invalid",
          message
      );
    } else {
      var fieldName = form.getRelativeDuration().getFieldName();
      errors.rejectValue(
          fieldName + YEAR_FIELD_SUFFIX,
          INVALID_ERROR_CODE,
          message
      );

      errors.rejectValue(
          fieldName + MONTH_FIELD_SUFFIX,
          INVALID_ERROR_CODE,
          ""
      );

      errors.rejectValue(
          fieldName + DAY_FIELD_SUFFIX,
          INVALID_ERROR_CODE,
          ""
      );
    }
  }

  private LocalDate calculateRateStartDate(
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleRateForm form
  ) {
    var termMap = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .collect(StreamUtil.toLinkedHashMap(LicenceScheduleTerm::getId, Function.identity()));

    var relativeToId = UUID.fromString(form.getRelativeEventId());

    LocalDate durationStartDate;
    if (termMap.containsKey(relativeToId)) {
      durationStartDate = termMap.get(relativeToId).getStartDate();
    } else {
      durationStartDate = licenceSchedulePhaseService.getPhaseByIdOrThrow(relativeToId).getStartDate();
    }

    if (form.getRateRelativeDateOption().equals(RateRelativeDateOption.ON_START_DATE)) {
      return durationStartDate;
    }

    return licenceScheduleCalculationService.calculateRelativeStartDueDate(
        durationStartDate,
        form.getRelativeDuration().toThreeFieldDuration()
    );
  }

  public void validatePhaseLengthUpdate(
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceSchedulePhaseForm form,
      Errors errors
  ) {
    var phaseTypeMap = licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .sorted(Comparator.comparing(phase -> phase.getPhaseType().getDisplayOrder()))
        .collect(StreamUtil.toLinkedHashMap(LicenceSchedulePhase::getPhaseType, Function.identity()));

    var newPhase = !phaseTypeMap.containsKey(form.getPhaseType());

    if (!newPhase && !isPhaseLengthened(form, phaseTypeMap)) {
      return;
    }

    var initialTerm = licenceScheduleTermService.getTermByLicenceScheduleDetailAndTermTypeOrThrow(
        licenceScheduleDetail,
        TermType.INITIAL
    );

    var updatedPhaseEndDate = calculateUpdatedFinalPhaseEndDate(initialTerm, phaseTypeMap, form);

    var initialTermEndDate = initialTerm.getEndDate();

    if (updatedPhaseEndDate.isAfter(initialTermEndDate)) {
      var fieldName = form.getPhaseDuration().getFieldName();
      var errormessage = newPhase
          ? "A phase cannot be added with this duration as this would cause a phase to end after the end of the initial term"
          : "The phase duration cannot be increased as this would cause a phase to end after the end of the initial term";

      errors.rejectValue(
          fieldName + YEAR_FIELD_SUFFIX,
          INVALID_ERROR_CODE,
          errormessage
      );

      errors.rejectValue(
          fieldName + MONTH_FIELD_SUFFIX,
          INVALID_ERROR_CODE,
          ""
      );

      errors.rejectValue(
          fieldName + DAY_FIELD_SUFFIX,
          INVALID_ERROR_CODE,
          ""
      );
    }
  }

  private LocalDate calculateUpdatedFinalPhaseEndDate(
      LicenceScheduleTerm initialTerm,
      Map<PhaseType, LicenceSchedulePhase> phaseTypeMap,
      LicenceSchedulePhaseForm form
  ) {
    Collection<LicenceSchedulePhase> phases;

    if (phaseTypeMap.containsKey(form.getPhaseType())) {
      phaseTypeMap.get(form.getPhaseType()).setPhaseDuration(form.getPhaseDuration().toThreeFieldDuration());

      phases = phaseTypeMap.values();
    } else {
      var simulatedPhase = new LicenceSchedulePhase();
      simulatedPhase.setPhaseDuration(form.getPhaseDuration().toThreeFieldDuration());
      simulatedPhase.setPhaseType(form.getPhaseType());

      phaseTypeMap.put(form.getPhaseType(), simulatedPhase);

      phases = phaseTypeMap.entrySet().stream()
          .sorted(Comparator.comparing(entry -> entry.getKey().getDisplayOrder()))
          .map(Map.Entry::getValue)
          .toList();
    }

    var nextStartDate = initialTerm.getStartDate();

    for (var phase : phases) {
      var endDate = licenceScheduleCalculationService.calculateDurationEndDate(nextStartDate, phase.getPhaseDuration());

      phase.setStartDate(nextStartDate);
      phase.setEndDate(endDate);

      nextStartDate = endDate.plusDays(1);
    }

    var finalPhase = phases.stream()
        .max(Comparator.comparing(phase -> phase.getPhaseType().getDisplayOrder()));

    return finalPhase.map(LicenceSchedulePhase::getEndDate)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "Final phase for licenceScheduleTerm with id %s cannot be found".formatted(initialTerm.getId())));
  }

  private boolean isPhaseLengthened(
      LicenceSchedulePhaseForm form,
      Map<PhaseType, LicenceSchedulePhase> phases
  ) {
    var currentDuration = phases.get(form.getPhaseType()).getPhaseDuration();
    var updatedDuration = form.getPhaseDuration().toThreeFieldDuration();

    return isDurationGreaterThan(updatedDuration, currentDuration);
  }

  public boolean doesFinalPhaseEndDateMatchEndOfInitialTerm(LicenceScheduleDetail licenceScheduleDetail) {
    if (!licenceTypeFeatureService.arePhasesCaptured(licenceScheduleDetail.getLicenceSchedule().getLicence().getType())) {
      return true;
    }

    var phases = licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(licenceScheduleDetail);

    if (phases.isEmpty()) {
      return true;
    }

    var initialTerm = licenceScheduleTermService.getTermByLicenceScheduleDetailAndTermTypeOrThrow(
        licenceScheduleDetail,
        TermType.INITIAL
    );

    var initialTermEndDate = initialTerm.getEndDate();

    var finalPhaseEndDate = phases.stream()
        .max(Comparator.comparing(LicenceSchedulePhase::getEndDate))
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "licenceSchedulePhase not found for term with id: %s".formatted(initialTerm.getId()))
        ).getEndDate();

    return initialTermEndDate.isEqual(finalPhaseEndDate);
  }

  public boolean doesExpiryDateMatchEndOfFinalTerm(LicenceScheduleDetail licenceScheduleDetail) {
    var expiry = licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail);

    if (expiry.isEmpty()) {
      return true;
    }

    var terms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);

    var finalTerm = terms.stream()
        .max(Comparator.comparing(term -> term.getTermType().getDisplayOrder()));

    if (finalTerm.isEmpty()) {
      return true;
    }

    var expiryDate = expiry.get().getExpiryDate();
    var finalTermEndDate = finalTerm.get().getEndDate();

    return finalTermEndDate.equals(expiryDate);
  }
}
