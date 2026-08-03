package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;

class LicencePositionChangeViewResolverTest {

  private static final int JOINING_ID = 100;
  private static final int WITHDRAWING_ID = 200;
  private static final String JOINING_NAME = "Joining Org Ltd";
  private static final String WITHDRAWING_NAME = "Withdrawing Org Ltd";

  @Test
  void getChangeViews_filtersChangesNotOnCurrentPosition() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var previousLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var previousChronologicalPosition = ChronologicalPositionTestUtil.live(
        previousLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(JOINING_ID).build()
    );
    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(currentLicencePosition);

    var chronologicalPositions = List.of(previousChronologicalPosition, currentChronologicalPosition);
    var result = LicencePositionChangeViewResolver.getChangeViews(
        currentLicencePosition.getId(),
        chronologicalPositions,
        LicencePositionStateResolver.resolveStatesByChronologicalPositionId(chronologicalPositions),
        Map.of(),
        null
    );

    assertThat(result).isEmpty();
  }

  @Test
  void buildAdministratorChange() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();
    var previousLicencePosition = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();

    var previousChronologicalPosition = ChronologicalPositionTestUtil.live(
        previousLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(WITHDRAWING_ID).build()
    );
    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(JOINING_ID).build()
    );

    var chronologicalPositions = List.of(previousChronologicalPosition, currentChronologicalPosition);
    var result = LicencePositionChangeViewResolver.getChangeViews(
        currentLicencePosition.getId(),
        chronologicalPositions,
        LicencePositionStateResolver.resolveStatesByChronologicalPositionId(chronologicalPositions),
        Map.of(JOINING_ID, JOINING_NAME, WITHDRAWING_ID, WITHDRAWING_NAME),
        null
    );

    assertThat(result)
        .hasSize(1)
        .extractingByKey(LicenceOperation.LICENCE_ADMINISTRATOR)
        .isInstanceOf(AdministratorChangeView.class)
        .extracting(
            licencePositionChangeView -> ((AdministratorChangeView) licencePositionChangeView).withdrawingOrganisationName(),
            licencePositionChangeView -> ((AdministratorChangeView) licencePositionChangeView).joiningOrganisationName()
        )
        .containsExactly(WITHDRAWING_NAME, JOINING_NAME);
  }

  @Test
  void buildAdministratorChange_noPriorChange() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(JOINING_ID).build()
    );

    var chronologicalPositions = List.of(currentChronologicalPosition);
    var result = LicencePositionChangeViewResolver.getChangeViews(
        currentLicencePosition.getId(),
        chronologicalPositions,
        LicencePositionStateResolver.resolveStatesByChronologicalPositionId(chronologicalPositions),
        Map.of(JOINING_ID, JOINING_NAME), null);

    assertThat(result)
        .hasSize(1)
        .extractingByKey(LicenceOperation.LICENCE_ADMINISTRATOR)
        .isInstanceOf(AdministratorChangeView.class)
        .extracting(
            licencePositionChangeView -> ((AdministratorChangeView) licencePositionChangeView).withdrawingOrganisationName(),
            licencePositionChangeView -> ((AdministratorChangeView) licencePositionChangeView).joiningOrganisationName()
        )
        .containsExactly(null, JOINING_NAME);
  }

  @Test
  void buildAdministratorChange_carriesChangeTypeFromCorrectionChange() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var correctionChange = new PositionChange(
        UUID.randomUUID().toString(),
        1,
        LicencePositionChangeType.ADD_CHANGE,
        List.of(LicenceOperation.newAdministratorChange().withOperator(JOINING_ID).build())
    );
    var currentChronologicalPosition = ChronologicalPosition.fromLicencePosition(
        currentLicencePosition,
        currentLicencePosition.getPositionDate(),
        currentLicencePosition.getPositionDateOrder(),
        List.of(correctionChange));

    var chronologicalPositions = List.of(currentChronologicalPosition);
    var result = LicencePositionChangeViewResolver.getChangeViews(
        currentLicencePosition.getId(),
        chronologicalPositions,
        LicencePositionStateResolver.resolveStatesByChronologicalPositionId(chronologicalPositions),
        Map.of(JOINING_ID, JOINING_NAME), null);

    assertThat(result)
        .extractingByKey(LicenceOperation.LICENCE_ADMINISTRATOR)
        .isInstanceOf(AdministratorChangeView.class)
        .extracting(LicencePositionChangeView::changeType)
        .isEqualTo(LicencePositionChangeType.ADD_CHANGE);
  }

  @Test
  void getChangeViews_buildsSetEquityChangeView() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        new SetEquityOperation(300, BigDecimal.valueOf(75))
    );

    var chronologicalPositions = List.of(currentChronologicalPosition);
    var result = LicencePositionChangeViewResolver.getChangeViews(
        currentLicencePosition.getId(),
        chronologicalPositions,
        LicencePositionStateResolver.resolveStatesByChronologicalPositionId(chronologicalPositions),
        Map.of(300, "Org"), null);

    assertThat(result)
        .extractingByKey(LicenceOperation.SET_EQUITY)
        .isInstanceOf(SetEquityChangeView.class);

    var setEquityChangeView = (SetEquityChangeView) result.get(LicenceOperation.SET_EQUITY);
    assertThat(setEquityChangeView.rows())
        .extracting(SetEquityRow::organisationName, SetEquityRow::equity)
        .containsExactly(tuple("Org", BigDecimal.valueOf(75)));
  }

  @Test
  void getChangeViews_whenChangeHasMultipleSetEquityOperations_mergesRowsIntoOneView() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        new SetEquityOperation(300, BigDecimal.valueOf(40)),
        new SetEquityOperation(400, BigDecimal.valueOf(60))
    );

    var chronologicalPositions = List.of(currentChronologicalPosition);
    var result = LicencePositionChangeViewResolver.getChangeViews(
        currentLicencePosition.getId(),
        chronologicalPositions,
        LicencePositionStateResolver.resolveStatesByChronologicalPositionId(chronologicalPositions),
        Map.of(300, "Org", 400, "Org2"), null);

    var setEquityChangeView = (SetEquityChangeView) result.get(LicenceOperation.SET_EQUITY);
    assertThat(setEquityChangeView.rows())
        .extracting(SetEquityRow::organisationName, SetEquityRow::equity)
        .containsExactly(
            tuple("Org", BigDecimal.valueOf(40)),
            tuple("Org2", BigDecimal.valueOf(60)));
  }

  @Test
  void getChangeViews_whenSetEquityOrganisationNameNotFound_usesEmptyName() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        new SetEquityOperation(300, BigDecimal.valueOf(75))
    );

    var chronologicalPositions = List.of(currentChronologicalPosition);
    var result = LicencePositionChangeViewResolver.getChangeViews(
        currentLicencePosition.getId(),
        chronologicalPositions,
        LicencePositionStateResolver.resolveStatesByChronologicalPositionId(chronologicalPositions),
        Map.of(), null);

    var setEquityChangeView = (SetEquityChangeView) result.get(LicenceOperation.SET_EQUITY);
    assertThat(setEquityChangeView.rows())
        .singleElement()
        .extracting(SetEquityRow::organisationName)
        .isEqualTo("");
  }
}