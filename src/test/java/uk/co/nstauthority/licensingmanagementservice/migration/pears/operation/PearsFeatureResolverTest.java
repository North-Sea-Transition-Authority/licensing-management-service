package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;

@ExtendWith(MockitoExtension.class)
class PearsFeatureResolverTest {

  private static final int RESOLVED_SI_ID = 739120;
  private static final int UNRESOLVED_SI_ID = 739277;
  private static final UUID RESOLVED_FEATURE_ID = UUID.randomUUID();

  @Mock
  private FeatureService featureService;

  private PearsFeatureResolver pearsFeatureResolver;

  private MigrationNotes notes;

  @BeforeEach
  void setUp() {
    pearsFeatureResolver = new PearsFeatureResolver(featureService);
    notes = new MigrationNotes();
  }

  @Test
  void resolve_whenAnSiIdHasNoFeature_thenTheOthersAreResolvedAndItIsNoted() {
    var siIds = Set.of(RESOLVED_SI_ID, UNRESOLVED_SI_ID);
    when(featureService.findAllByLegacyIdIn(siIds)).thenReturn(List.of(
        FeatureTestUtil.builder()
            .withId(RESOLVED_FEATURE_ID)
            .withLegacyId(RESOLVED_SI_ID)
            .build()
    ));

    var featureIdsBySiId = pearsFeatureResolver.resolve(siIds, notes);

    assertThat(featureIdsBySiId).isEqualTo(Map.of(RESOLVED_SI_ID, RESOLVED_FEATURE_ID));
    assertThat(notes.noteCountsByReason())
        .isEqualTo(Map.of("PEARS block or subarea has no migrated GIS feature, so no position can hold it", 1));
  }

  @Test
  void resolve_whenThereAreNoSiIds_thenNothingIsLookedUp() {
    var featureIdsBySiId = pearsFeatureResolver.resolve(Set.of(), notes);

    assertThat(featureIdsBySiId).isEmpty();
    verifyNoInteractions(featureService);
  }
}
