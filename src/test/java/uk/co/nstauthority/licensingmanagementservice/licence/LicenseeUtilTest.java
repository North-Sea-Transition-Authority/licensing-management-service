package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@ExtendWith(MockitoExtension.class)
class LicenseeUtilTest {

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Test
  void getLicenseeNames_assertSortedAlphabetically() {
    var licence = LicenceTestUtil.builder().withId(1).build();

    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(List.of(licence)))
        .thenReturn(Map.of(licence, List.of(
            new OrganisationUnit(2, "SHELL U.K. LIMITED"),
            new OrganisationUnit(1, "BP EXPLORATION (ALPHA) LIMITED")
        )));

    var result = LicenseeUtil.getLicenseeNames(licence, licenceResponsibleOrganisationService);

    assertThat(result).containsExactly("BP EXPLORATION (ALPHA) LIMITED", "SHELL U.K. LIMITED");
  }

  @Test
  void getLicenseeNames_whenNoLicensees_assertEmptyList() {
    var licence = LicenceTestUtil.builder().withId(1).build();

    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(List.of(licence)))
        .thenReturn(Map.of());

    var result = LicenseeUtil.getLicenseeNames(licence, licenceResponsibleOrganisationService);

    assertThat(result).isEmpty();
  }
}
