package uk.co.nstauthority.licensingmanagementservice.licence.schedule.common;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
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

  public ScheduleRelativeDateValidationService(
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      LicenceStartDateService licenceStartDateService,
      WorkProgrammeActivityService workProgrammeActivityService,
      LicenceScheduleRateService licenceScheduleRateService,
      OtherScheduleEventService otherScheduleEventService
  ) {
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
    this.licenceStartDateService = licenceStartDateService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.licenceScheduleRateService = licenceScheduleRateService;
    this.otherScheduleEventService = otherScheduleEventService;
  }

  public void validateRelativeDateBeforeEndOfSchedule(
      LicenceScheduleDetail licenceScheduleDetail,
      ThreeFieldDurationInput duration,
      UUID relativeToId,
      Errors errors
  ) {
    var terms =  licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);

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
    if (current.years() > updated.years()) {
      return true;
    }

    if (current.years().equals(updated.years())
        && current.months() > updated.months()) {
      return true;
    }

    return current.years().equals(updated.years())
        && current.months().equals(updated.months())
        && current.days() > updated.days();
  }
}
