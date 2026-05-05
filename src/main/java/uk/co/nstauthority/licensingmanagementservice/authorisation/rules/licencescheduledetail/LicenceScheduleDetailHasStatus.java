package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.licencescheduledetail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface LicenceScheduleDetailHasStatus {
  LicenceScheduleDetailStatus[] value();
}