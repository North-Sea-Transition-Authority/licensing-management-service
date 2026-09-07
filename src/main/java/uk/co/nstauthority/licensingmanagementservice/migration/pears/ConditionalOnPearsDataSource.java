package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(prefix = "pears.datasource", name = {"url", "username", "password"})
@interface ConditionalOnPearsDataSource {
}
