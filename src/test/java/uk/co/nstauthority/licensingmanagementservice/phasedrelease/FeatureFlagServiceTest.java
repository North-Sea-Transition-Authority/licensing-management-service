package uk.co.nstauthority.licensingmanagementservice.phasedrelease;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FeatureFlagServiceTest {

  private FeatureFlagService serviceWithProfiles(String... activeProfiles) {
    return FeatureFlagServiceTestUtil.withProfiles(activeProfiles);
  }

  @Test
  void isEnabled_phase_initial_alwaysOn() {
    assertThat(serviceWithProfiles().isEnabled(ReleasePhase.NOT_FLAGGED)).isTrue();
    assertThat(serviceWithProfiles("enable-lms1", "enable-lms2").isEnabled(ReleasePhase.NOT_FLAGGED)).isTrue();
  }

  @Test
  void isEnabled_phase_whenNoProfiles_thenOnlyInitialOn() {
    var service = serviceWithProfiles();

    assertThat(service.isEnabled(ReleasePhase.LMS1)).isFalse();
    assertThat(service.isEnabled(ReleasePhase.LMS2)).isFalse();
  }

  @Test
  void isEnabled_phase_whenLms1Only_thenLms1OnLms2Off() {
    var service = serviceWithProfiles("enable-lms1");

    assertThat(service.isEnabled(ReleasePhase.LMS1)).isTrue();
    assertThat(service.isEnabled(ReleasePhase.LMS2)).isFalse();
  }

  @Test
  void isEnabled_phase_whenBothProfiles_thenBothOn() {
    var service = serviceWithProfiles("enable-lms1", "enable-lms2");

    assertThat(service.isEnabled(ReleasePhase.LMS1)).isTrue();
    assertThat(service.isEnabled(ReleasePhase.LMS2)).isTrue();
  }

  @Test
  void isEnabled_feature_delegatesToPhase() {
    assertThat(serviceWithProfiles().isEnabled(ReleaseFeature.START_APPLICATION)).isFalse();
    assertThat(serviceWithProfiles("enable-lms1").isEnabled(ReleaseFeature.START_APPLICATION)).isTrue();

    assertThat(serviceWithProfiles("enable-lms1").isEnabled(ReleaseFeature.START_CORRECTION)).isFalse();
    assertThat(serviceWithProfiles("enable-lms1", "enable-lms2").isEnabled(ReleaseFeature.START_CORRECTION)).isTrue();
  }

  @Test
  void getEnabledFeatures_whenLms1Only_thenExcludesLms2Features() {
    var enabled = serviceWithProfiles("enable-lms1").getEnabledFeatures();

    assertThat(enabled)
        .contains(ReleaseFeature.START_APPLICATION, ReleaseFeature.SCHEDULE_APPLICATION)
        .doesNotContain(ReleaseFeature.START_CORRECTION);
  }

  @Test
  void getEnabledFeatures_whenNoProfiles_thenOnlyNotFlaggedFeatures() {
    assertThat(serviceWithProfiles().getEnabledFeatures())
        .allSatisfy(feature -> assertThat(feature.getReleasePhase()).isEqualTo(ReleasePhase.NOT_FLAGGED))
        .contains(ReleaseFeature.TEAM_ROLE);
  }

  @Test
  void filterEnabled_returnsOnlyMembersWhoseFeatureIsOn() {
    var lms1Option = new TestOption(ReleaseFeature.SCHEDULE_APPLICATION);
    var lms2Option = new TestOption(ReleaseFeature.START_CORRECTION);

    var service = serviceWithProfiles("enable-lms1");

    assertThat(service.filterEnabled(List.of(lms1Option, lms2Option)))
        .containsExactly(lms1Option);
  }

  @ParameterizedTest
  @EnumSource(ReleaseFeature.class)
  void everyReleaseFeature_isMappedToAPhase(ReleaseFeature feature) {
    assertThat(feature.getReleasePhase()).isNotNull();
  }

  private record TestOption(ReleaseFeature releaseFeature) implements PhaseGated {
    @Override
    public ReleaseFeature getReleaseFeature() {
      return releaseFeature;
    }
  }
}
