package uk.co.nstauthority.licensingmanagementservice.authorisation.rules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface LogWorkAreaItemView {

  WorkAreaDataItemType itemType();

  String pathVariable();

  boolean disable() default false;
}
