package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.List;
import java.util.stream.Collectors;

public class LicenceTypeUtil {

  private LicenceTypeUtil() {
  }

  public static String getUrlSlugList(List<LicenceType> licenceTypes) {
    return licenceTypes.stream()
        .map(LicenceType::getUrlSlug)
        .collect(Collectors.joining(","));
  }
}
