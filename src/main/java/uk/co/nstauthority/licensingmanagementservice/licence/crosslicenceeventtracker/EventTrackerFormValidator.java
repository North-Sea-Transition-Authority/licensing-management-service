package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Service
public class EventTrackerFormValidator {

  private static final String FROM_DATE_FIELD = "fromDate";
  private static final String TO_DATE_FIELD = "toDate";

  public void isValid(EventTrackerForm form, BindingResult bindingResult) {
    var fromDate = DateUtil.validateDateStrict(form.getFromDate(), FROM_DATE_FIELD, "Event from", bindingResult);
    var toDate = DateUtil.validateDateStrict(form.getToDate(), TO_DATE_FIELD, "Event to", bindingResult);

    if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
      bindingResult.rejectValue(FROM_DATE_FIELD, "%s.afterToDate".formatted(FROM_DATE_FIELD),
          "Event from date must not be after the event to date");
    }
  }
}
