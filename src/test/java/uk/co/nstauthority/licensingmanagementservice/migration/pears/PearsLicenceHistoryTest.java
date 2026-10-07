package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.LicenceOperationHistory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsCompanyListType;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsHistoryTestUtil;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperationType;

class PearsLicenceHistoryTest {

  private static final LocalDate FIRST_DATE = LocalDate.of(1964, Month.SEPTEMBER, 18);
  private static final LocalDate LAST_DATE = LocalDate.of(1972, Month.MARCH, 1);

  @Test
  void reconstruct_whenATransactionMadeSeveralOperations_thenTheyAreOnePosition() {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(15188, "XPT/1", FIRST_DATE, 6)
        .administratorSet(11)
        .operationWithoutPayload("PED_BLOCK_CREATE")
        .build());

    assertThat(history.positions())
        .singleElement()
        .satisfies(position -> {
          assertThat(position.transactionId()).isEqualTo(15188);
          assertThat(position.operations()).hasSize(2);
        });
  }

  @Test
  void reconstruct_whenATransactionOnlyDidWorkThisMigrationIgnores_thenItIsStillAPosition() {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(1, "XPT/1", FIRST_DATE, 6)
        .operationWithoutPayload("PED_RETENTION_AREA_CHANGE")
        .and()
        .position(2, "XPT/2", FIRST_DATE, 9)
        .companyList(PearsCompanyListType.BENEFICIAL_INTEREST, 11)
        .build());

    assertThat(history.positions())
        .extracting(PearsPosition::transactionId, PearsPosition::positionDateOrder)
        .containsExactly(tuple(1L, 1), tuple(2L, 2));
  }

  @Test
  void reconstruct_whenPositionSequencesAreSparse_thenTheOrderWithinTheDateIsTheirRank() {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(3, "XPT/3", LAST_DATE, 2)
        .administratorSet(33)
        .and()
        .position(1, "XPT/1", FIRST_DATE, 6)
        .administratorSet(11)
        .and()
        .position(2, "XPT/2", FIRST_DATE, 9000)
        .administratorSet(22)
        .build());

    assertThat(history.positions())
        .extracting(PearsPosition::regulatorReference, PearsPosition::positionDate, PearsPosition::positionDateOrder)
        .containsExactly(
            tuple("XPT/1", FIRST_DATE, 1),
            tuple("XPT/2", FIRST_DATE, 2),
            tuple("XPT/3", LAST_DATE, 1));
  }

  @Test
  void reconstruct_whenTheLicenceStartsAndEnds_thenBothArePositions() {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(1, "XPT/1", FIRST_DATE, 6)
        .operationWithoutPayload(PearsOperationType.LICENCE_CREATE)
        .and()
        .position(2, "XPT/2", LAST_DATE, 3)
        .licenceEnd()
        .build());

    assertThat(history.positions())
        .extracting(PearsPosition::transactionId, PearsPosition::positionDateOrder)
        .containsExactly(tuple(1L, 1), tuple(2L, 1));
  }

  @Test
  void reconstruct_whenPearsHoldsNothing_thenTheLicenceHoldsNoPositions() {
    var history = PearsLicenceHistory.reconstruct(new LicenceOperationHistory("P", 8, List.of()));

    assertThat(history).usingRecursiveComparison()
        .isEqualTo(new PearsLicenceHistory("P", 8, List.of()));
  }

  @Test
  void toLicencePositions_thenThePositionsAreComparableWithTheDataPoints() {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(1, "XPT/1", FIRST_DATE, 6)
        .administratorSet(11)
        .build());

    assertThat(history.toLicencePositions())
        .usingRecursiveComparison()
        .isEqualTo(new PearsLicencePositions("P", 8, List.of(
            new PearsLicencePositions.Position(FIRST_DATE, 6, 1, "XPT/1", 1))));
  }
}
