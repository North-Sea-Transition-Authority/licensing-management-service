package uk.co.nstauthority.licensingmanagementservice.licence.schedule.common;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class ScheduleRelativeDateValidationService {

  private static final String YEAR_FIELD_SUFFIX = ".years";
  private static final String MONTH_FIELD_SUFFIX = ".months";
  private static final String DAY_FIELD_SUFFIX = ".days";
  private static final String INVALID_ERROR_CODE = ".invalid";

  LicenceScheduleTermService licenceScheduleTermService;

  LicenceSchedulePhaseService licenceSchedulePhaseService;

  LicenceScheduleCalculationService licenceScheduleCalculationService;

  public ScheduleRelativeDateValidationService(
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceScheduleCalculationService licenceScheduleCalculationService
  ) {
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
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
}
