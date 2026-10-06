package uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.administrator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface LicencePositionHasNoLiveChangeOfType {
  Class<? extends LicenceOperation> value();
}
