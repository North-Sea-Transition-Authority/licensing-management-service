package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

class PartialSurrenderDetailsFormTest {

  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();

  @Test
  void from_whenNoCommittedPartialSurrender_thenTheFormIsEmpty() {
    var form = PartialSurrenderDetailsForm.from(null);

    assertThat(form.getFeatureIds()).isEmpty();
  }

  @Test
  void from_whenCommittedPartialSurrender_thenTheSurrenderedBlocksArePrefilled() {
    var committedPartialSurrender = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
        .build();

    var form = PartialSurrenderDetailsForm.from(committedPartialSurrender);

    assertThat(form.getFeatureIds())
        .isEqualTo(new LinkedHashSet<>(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID)));
  }
}
