package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Comparator;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

public class LicenseeUtil {

  private LicenseeUtil() {
  }

  public static List<String> getLicenseeNames(
      Licence licence,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService
  ) {
    return licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(List.of(licence))
        .getOrDefault(licence, List.of())
        .stream()
        .map(OrganisationUnit::organisationUnitName)
        .sorted(Comparator.naturalOrder())
        .toList();
  }
}
