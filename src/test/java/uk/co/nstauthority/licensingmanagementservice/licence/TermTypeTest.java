package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TermTypeTest {

  @Test
  void fourth_isAHistoricProductionTermAfterTheThird() {
    assertThat(TermType.FOURTH.isHistoric()).isTrue();
    assertThat(TermType.FOURTH.getLicenceTypes())
        .containsExactlyInAnyOrder(LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION);
    assertThat(TermType.FOURTH.getDisplayName()).isEqualTo("Fourth Term");
    assertThat(TermType.FOURTH.getDisplayOrder()).isGreaterThan(TermType.THIRD.getDisplayOrder());
  }

  @Test
  void onlyTheFourthTermIsHistoric() {
    assertThat(Arrays.stream(TermType.values()).filter(TermType::isHistoric)).containsExactly(TermType.FOURTH);
  }

  @Test
  void getTermsFor_includesHistoricTerms() {
    assertThat(TermType.getTermsFor(LicenceType.SEAWARD_PRODUCTION))
        .containsExactlyInAnyOrder(TermType.INITIAL, TermType.SECOND, TermType.THIRD, TermType.FOURTH);
  }

  @Test
  void getSelectableTermsFor_leavesOutHistoricTermsNotInUse() {
    assertThat(TermType.getSelectableTermsFor(LicenceType.SEAWARD_PRODUCTION, Set.of(TermType.INITIAL)))
        .containsExactlyInAnyOrder(TermType.INITIAL, TermType.SECOND, TermType.THIRD);
  }

  @Test
  void getSelectableTermsFor_includesHistoricTermsInUse() {
    assertThat(TermType.getSelectableTermsFor(LicenceType.LANDWARD_PRODUCTION, List.of(TermType.FOURTH)))
        .containsExactlyInAnyOrder(TermType.INITIAL, TermType.SECOND, TermType.THIRD, TermType.FOURTH);
  }

  @Test
  void getSelectableTermsFor_neverOffersAProductionTermToAnotherLicenceType() {
    assertThat(TermType.getSelectableTermsFor(LicenceType.CARBON_STORAGE, Set.of(TermType.FOURTH)))
        .containsExactlyInAnyOrder(
            TermType.APPRAISAL, TermType.INITIAL_CS, TermType.OPERATIONAL, TermType.POST_CLOSURE_PERIOD);
  }
}
