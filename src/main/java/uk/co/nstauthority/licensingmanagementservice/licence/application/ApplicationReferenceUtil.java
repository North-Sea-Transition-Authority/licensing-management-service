package uk.co.nstauthority.licensingmanagementservice.licence.application;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class ApplicationReferenceUtil {

  // Base references look like LMS/EAA/<year>/<number>; anything after that is a version suffix
  private static final int BASE_REFERENCE_SEGMENTS = 4;

  private ApplicationReferenceUtil() {
    throw new IllegalStateException("ApplicationReferenceUtil is a utility class and should not be instantiated");
  }

  /**
   * Returns the reference with the given version number as its suffix, replacing any earlier suffix.
   * LMS/EAA/2024/5 with version 2 gives LMS/EAA/2024/5/2, and LMS/EAA/2024/5/2 with version 3 gives LMS/EAA/2024/5/3.
   */
  public static String withVersionNumber(String reference, int versionNumber) {
    var baseReference = Arrays.stream(reference.split("/"))
        .limit(BASE_REFERENCE_SEGMENTS)
        .collect(Collectors.joining("/"));
    return baseReference + "/" + versionNumber;
  }
}
