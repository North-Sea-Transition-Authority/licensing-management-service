package uk.co.nstauthority.licensingmanagementservice.util.enumutil;

/**
 * Interface which allows implementing classes to expose their human readable representation consistently.
 */
public interface Displayable {

  String getDisplayName();

  default int getDisplayOrder() {
    return 0;
  }

  String name();

  default String getEnumName() {
    return name();
  }
}
