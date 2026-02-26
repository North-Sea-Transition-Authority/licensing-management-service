package uk.co.nstauthority.licensingmanagementservice.document;

public class DocumentSectionUtil {

  private DocumentSectionUtil() {
    throw new IllegalStateException("This is a utility class and cannot be instantiated");
  }

  public static String getAddSectionPageTitle(String title, AddSectionOption addSectionOption) {
    var pageTitle = switch (addSectionOption) {
      case ADD_BEFORE -> "Add section before %s";
      case ADD_AFTER -> "Add section after %s";
      case ADD_SUBSECTION -> "Add subsection";
    };
    return pageTitle.formatted(title);
  }
}
