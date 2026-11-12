package uk.co.nstauthority.licensingmanagementservice.licence.assignmentapplication.startjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.teams.IndustryRoleService;

@ExtendWith(MockitoExtension.class)
class LicenceAssignmentApplicationStartJourneyServiceTest {

  private static final LicenceType LICENCE_TYPE = LicenceType.SEAWARD_PRODUCTION;

  @Mock
  private IndustryRoleService industryRoleService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private LicenceService licenceService;

  @InjectMocks
  private LicenceAssignmentApplicationStartJourneyService licenceAssignmentApplicationStartJourneyService;

  @Test
  void getOrganisationOptions() {
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    when(industryRoleService.getEditorOrSubmitterOrganisationUnitIds(user.wuaId()))
        .thenReturn(new LinkedHashSet<>(List.of(1, 2)));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(1, 2)))
        .thenReturn(Map.of(1, "beta organisation", 2, "Alpha organisation"));

    var result = licenceAssignmentApplicationStartJourneyService.getOrganisationOptions(user);

    assertThat(result).containsExactly(
        entry("2", "Alpha organisation"),
        entry("1", "beta organisation")
    );
  }

  @Test
  void getLicencesOfType_whenLicenceOfOtherType_thenExcluded() {
    var licenceOfType = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LICENCE_TYPE)
        .build();
    var licenceOfOtherType = LicenceTestUtil.builder()
        .withId(2)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();
    when(licenceService.getLicencesByIds(List.of(1, 2))).thenReturn(List.of(licenceOfType, licenceOfOtherType));

    var result = licenceAssignmentApplicationStartJourneyService.getLicencesOfType(List.of(1, 2), LICENCE_TYPE);

    assertThat(result).containsExactly(licenceOfType);
  }

  @Test
  void getLicencesOfType_whenLicencesFound_thenReturnedInSubmittedOrder() {
    var firstLicence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LICENCE_TYPE)
        .build();
    var secondLicence = LicenceTestUtil.builder()
        .withId(2)
        .withLicenceType(LICENCE_TYPE)
        .build();
    when(licenceService.getLicencesByIds(List.of(2, 1))).thenReturn(List.of(firstLicence, secondLicence));

    var result = licenceAssignmentApplicationStartJourneyService.getLicencesOfType(List.of(2, 1), LICENCE_TYPE);

    assertThat(result).containsExactly(secondLicence, firstLicence);
  }
}
