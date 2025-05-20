package uk.co.nstauthority.template.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.params.ParameterizedTest;

@Retention(RetentionPolicy.RUNTIME)
@ParameterizedTest
public @interface ParameterizedAuthorisationSecurityTest {
}
