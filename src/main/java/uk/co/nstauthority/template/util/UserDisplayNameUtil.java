package uk.co.nstauthority.template.util;

public class UserDisplayNameUtil {

  private UserDisplayNameUtil() {
    throw new IllegalStateException("This is a util class and should not be instantiated");
  }

  public static String getUserDisplayName(String forename, String surname) {
    return "%s %s".formatted(forename, surname);
  }

  public static String getUserDisplayNameAndEmail(String forename, String surname, String emailAddress) {
    return "%s (%s)".formatted(getUserDisplayName(forename, surname), emailAddress);
  }

  public static String getUserDisplayNameAndEmail(String displayName, String emailAddress) {
    return "%s (%s)".formatted(displayName, emailAddress);
  }
}
