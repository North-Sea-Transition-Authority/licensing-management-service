package uk.co.nstauthority.licensingmanagementservice.teams;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;

class RoleTest {

  @ParameterizedTest
  @EnumSource(Role.class)
  void getReleaseFeature_everyRoleIsMappedToAFeature(Role role) {
    assertThat(role.getReleaseFeature()).isNotNull();
  }

  @Test
  void getReleaseFeature_industryRolesGrantingLicenceAndApplicationAccessAreFlagged() {
    var flaggedRoles = EnumSet.allOf(Role.class)
        .stream()
        .filter(role -> role.getReleaseFeature() == ReleaseFeature.INDUSTRY_LICENCE_AND_APPLICATION_ROLE)
        .toList();

    assertThat(flaggedRoles)
        .containsExactly(Role.VIEW_ORGANISATION_LICENCES, Role.APPLICATION_EDITOR, Role.APPLICATION_SUBMITTER);
  }

  @Test
  void getReleaseFeature_regulatorAndExternalContributorRolesAreNotFlagged() {
    var unflaggedTeamTypes = EnumSet.complementOf(EnumSet.of(TeamType.ORGANISATION));

    assertThat(unflaggedTeamTypes)
        .allSatisfy(teamType -> assertThat(teamType.getAllowedRoles())
            .allSatisfy(role -> assertThat(role.getReleaseFeature()).isEqualTo(ReleaseFeature.TEAM_ROLE)));
  }
}
