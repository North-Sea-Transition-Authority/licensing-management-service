package uk.co.nstauthority.template.teams.management.access;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import uk.co.nstauthority.template.teams.Role;
import uk.co.nstauthority.template.teams.TeamType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface InvokingUserHasStaticRole {
  TeamType teamType();
  Role role();
}
