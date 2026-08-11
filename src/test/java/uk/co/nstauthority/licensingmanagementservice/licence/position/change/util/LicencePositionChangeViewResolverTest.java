package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.LicencePositionTransferEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityChangeHoldingView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityChangeView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

class LicencePositionChangeViewResolverTest {

  private static final int JOINING_ID = 100;
  private static final int WITHDRAWING_ID = 200;
  private static final String JOINING_NAME = "Joining Org Ltd";
  private static final String WITHDRAWING_NAME = "Withdrawing Org Ltd";
  private static final UUID FIRST_FEATURE_ID = UUID.randomUUID();
  private static final UUID SECOND_FEATURE_ID = UUID.randomUUID();
  private static final Map<UUID, String> FEATURE_NAMES = Map.of(
      FIRST_FEATURE_ID, "30/1a",
      SECOND_FEATURE_ID, "30/2");

  private static final int SET_EQUITY_ORG_ID = 300;
  private static final String SET_EQUITY_ORG_NAME = "Set Equity Org Ltd";
  private static final int TRANSFER_FROM_ID = 500;
  private static final int TRANSFER_TO_ID = 600;
  private static final String TRANSFER_FROM_NAME = "Transfer From Org Ltd";
  private static final String TRANSFER_TO_NAME = "Transfer To Org Ltd";

  @Test
  void getChangeViews_filtersChangesNotOnCurrentPosition() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().withPositionOrder(2).build();
    var previousLicencePosition = LicencePositionTestUtil.newBuilder().withPositionOrder(1).build();

    var previousChronologicalPosition = ChronologicalPositionTestUtil.live(
        previousLicencePosition,
        LicenceOperation.newAdministratorChange().withOperator(JOINING_ID).build()
    );
    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(currentLicencePosition);

    var chronologicalPositions = List.of(previousChronologicalPosition, currentChronologicalPosition);
    var result = LicencePositionChangeViewResolver.getChangeViews(
        currentLicencePosition.getId(),
        chronologicalPositions,
        LicencePositionStateResolver.resolve(chronologicalPositions),
        Map.of(),
        Map.of(),
        null
    );

    assertThat(result).isEmpty();
  }

  @Test
  void buildAdministratorChange() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().withPositionOrder(2).build();
    var previousLicencePosition = LicencePositionTestUtil.newBuilder().withPositionOrder(1).build();

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
        LicencePositionStateResolver.resolve(chronologicalPositions),
        Map.of(JOINING_ID, JOINING_NAME, WITHDRAWING_ID, WITHDRAWING_NAME),
        Map.of(),
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
        LicencePositionStateResolver.resolve(chronologicalPositions),
        Map.of(JOINING_ID, JOINING_NAME),
        Map.of(),
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
        LicencePositionStateResolver.resolve(chronologicalPositions),
        Map.of(JOINING_ID, JOINING_NAME),
        Map.of(),
        null
    );

    assertThat(result)
        .extractingByKey(LicenceOperation.LICENCE_ADMINISTRATOR)
        .isInstanceOf(AdministratorChangeView.class)
        .extracting(LicencePositionChangeView::changeType)
        .isEqualTo(LicencePositionChangeType.ADD_CHANGE);
  }

  @Test
  void buildAdministratorChange_whenUntouchedExecutedChange_populatesRemoveNotUndo() {
    var view = adminChangeView(
        null,
        PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID())
    );

    assertThat(view.correctUrl()).contains("correct-administrator-change");
    assertThat(view.removeUrl()).contains("remove-administrator-change");
    assertThat(view.undoUrl()).isNull();
  }

  @Test
  void buildAdministratorChange_whenExecutedUpdateChange_populatesCorrectAndUndoNotRemove() {
    var view = adminChangeView(
        LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS,
        PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID())
    );

    assertThat(view.correctUrl()).contains("correct-administrator-change");
    assertThat(view.removeUrl()).isNull();
    assertThat(view.undoUrl()).contains("undo-administrator-change");
  }

  @Test
  void buildAdministratorChange_whenAddChange_populatesUndoNotRemove() {
    var view = adminChangeView(
        LicencePositionChangeType.ADD_CHANGE,
        PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID())
    );

    assertThat(view.correctUrl()).contains("add-administrator-change");
    assertThat(view.removeUrl()).isNull();
    assertThat(view.undoUrl()).contains("undo-administrator-change");
  }

  @Test
  void buildAdministratorChange_whenRemoveChange_populatesUndoOnly() {
    var view = adminChangeView(
        LicencePositionChangeType.REMOVE_CHANGE,
        PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID())
    );

    assertThat(view.correctUrl()).isNull();
    assertThat(view.removeUrl()).isNull();
    assertThat(view.undoUrl()).contains("undo-administrator-change");
  }

  @Test
  void buildAdministratorChange_whenAddedPosition_populatesUndoNotRemove() {
    var view = adminChangeView(
        LicencePositionChangeType.ADD_CHANGE,
        PositionChangeUrlContext.forAddedPosition(UUID.randomUUID(), UUID.randomUUID())
    );

    assertThat(view.removeUrl()).isNull();
    assertThat(view.undoUrl()).contains("undo-administrator-change");
  }

  @Test
  void buildAdministratorChange_whenNoUrlContext_hasNoUrls() {
    var view = adminChangeView(LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS, null);

    assertThat(view.correctUrl()).isNull();
    assertThat(view.removeUrl()).isNull();
    assertThat(view.undoUrl()).isNull();
  }

  private AdministratorChangeView adminChangeView(String changeType, PositionChangeUrlContext urlContext) {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var change = new PositionChange(
        UUID.randomUUID().toString(),
        1,
        changeType,
        List.of(LicenceOperation.newAdministratorChange().withOperator(JOINING_ID).build())
    );
    var currentChronologicalPosition = ChronologicalPosition.fromLicencePosition(
        currentLicencePosition,
        currentLicencePosition.getPositionDate(),
        currentLicencePosition.getPositionDateOrder(),
        List.of(change));

    var chronologicalPositions = List.of(currentChronologicalPosition);
    var result = LicencePositionChangeViewResolver.getChangeViews(
        currentLicencePosition.getId(),
        chronologicalPositions,
        LicencePositionStateResolver.resolve(chronologicalPositions),
        Map.of(JOINING_ID, JOINING_NAME),
        Map.of(),
        urlContext
    );

    return (AdministratorChangeView) result.get(LicenceOperation.LICENCE_ADMINISTRATOR);
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
        LicencePositionStateResolver.resolve(chronologicalPositions),
        Map.of(300, "Org"),
        Map.of(),
        null
    );

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
        LicencePositionStateResolver.resolve(chronologicalPositions),
        Map.of(300, "Org", 400, "Org2"),
        Map.of(),
        null
    );

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
        LicencePositionStateResolver.resolve(chronologicalPositions),
        Map.of(),
        Map.of(),
        null
    );

    var setEquityChangeView = (SetEquityChangeView) result.get(LicenceOperation.SET_EQUITY);
    assertThat(setEquityChangeView.rows())
        .singleElement()
        .extracting(SetEquityRow::organisationName)
        .isEqualTo("Not available");
  }

  @Test
  void buildSetEquityChangeView_whenExecutedAddChange_buildsViewWithRowChangeTypeAndUpdateUrl() {
    var correctionId = UUID.randomUUID();
    var licencePositionId = UUID.randomUUID();
    var urlContext = PositionChangeUrlContext.forExecutedPosition(correctionId, licencePositionId);

    var view = setEquityChangeView(LicencePositionChangeType.ADD_CHANGE, urlContext);

    var expected = new SetEquityChangeView(
        List.of(new SetEquityRow(SET_EQUITY_ORG_NAME, BigDecimal.valueOf(75))),
        LicencePositionChangeType.ADD_CHANGE,
        ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForExecutedPosition(correctionId, licencePositionId, null)));

    assertThat(view).isEqualTo(expected);
  }

  @Test
  void buildSetEquityChangeView_whenExecutedUpdateChangeOperations_buildsUpdateUrlToExecutedSummary() {
    var correctionId = UUID.randomUUID();
    var licencePositionId = UUID.randomUUID();
    var urlContext = PositionChangeUrlContext.forExecutedPosition(correctionId, licencePositionId);

    var view = setEquityChangeView(LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS, urlContext);

    assertThat(view.updateUrl()).isEqualTo(ReverseRouter.route(on(LicencePositionSetEquityController.class)
        .renderSummaryForExecutedPosition(correctionId, licencePositionId, null)));
  }

  @Test
  void buildSetEquityChangeView_whenAddedPositionAddChange_buildsUpdateUrlToAddedSummary() {
    var correctionId = UUID.randomUUID();
    var licencePositionCorrectionId = UUID.randomUUID();
    var urlContext = PositionChangeUrlContext.forAddedPosition(correctionId, licencePositionCorrectionId);

    var view = setEquityChangeView(LicencePositionChangeType.ADD_CHANGE, urlContext);

    assertThat(view.updateUrl()).isEqualTo(ReverseRouter.route(on(LicencePositionSetEquityController.class)
        .renderSummaryForAddedPosition(correctionId, licencePositionCorrectionId, null)));
  }

  @Test
  void buildSetEquityChangeView_whenNoUrlContext_hasNoUpdateUrl() {
    var view = setEquityChangeView(LicencePositionChangeType.ADD_CHANGE, null);
    assertThat(view.updateUrl()).isNull();
  }

  @Test
  void buildSetEquityChangeView_whenUntouchedExecutedChange_hasNoUpdateUrl() {
    var urlContext = PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID());
    var view = setEquityChangeView(null, urlContext);

    assertThat(view.updateUrl()).isNull();
  }

  @Test
  void getChangeViews_buildsTransferEquityChangeView() {
    var previousPosition = LicencePositionTestUtil.newBuilder().withPositionOrder(1).build();
    var currentPosition = LicencePositionTestUtil.newBuilder().withPositionOrder(2).build();
    var urlContext = PositionChangeUrlContext.forAddedPosition(previousPosition.getId(), currentPosition.getId());

    var previousChronological = ChronologicalPositionTestUtil.live(
        previousPosition,
        new SetEquityOperation(TRANSFER_FROM_ID, BigDecimal.valueOf(100)));
    var currentChronological = ChronologicalPositionTestUtil.live(
        currentPosition,
        LicenceOperation.newTransferEquityOperation()
            .withTransferFrom(TRANSFER_FROM_ID)
            .withTransferTo(TRANSFER_TO_ID)
            .withEquity(BigDecimal.valueOf(30))
            .build());

    var chronologicalPositions = List.of(previousChronological, currentChronological);
    var result = LicencePositionChangeViewResolver.getChangeViews(
        currentPosition.getId(),
        chronologicalPositions,
        LicencePositionStateResolver.resolve(chronologicalPositions),
        Map.of(TRANSFER_FROM_ID, TRANSFER_FROM_NAME, TRANSFER_TO_ID, TRANSFER_TO_NAME),
        null,
        urlContext
    );

    var view = (TransferEquityChangeView) result.get(LicenceOperation.TRANSFER_EQUITY);

    var expected = new TransferEquityChangeView(
        List.of(new TransferEquityChangeHoldingView(
            TRANSFER_FROM_NAME, BigDecimal.valueOf(100), BigDecimal.valueOf(70),
            TRANSFER_TO_NAME, BigDecimal.ZERO, BigDecimal.valueOf(30),
            BigDecimal.valueOf(30), null)),
        null,
        null);

    assertThat(view).isEqualTo(expected);
  }

  @Test
  void getChangeViews_whenTransferEquityRetainsBeneficialInterest_passesFlagThrough() {
    var view = transferEquityChangeView(null, null, true);
    assertThat(view.holdings().getFirst().retainBeneficialInterest()).isTrue();
  }

  @Test
  void getChangeViews_whenTransferEquityOrganisationNamesNotFound_usesEmptyNames() {
    var previousPosition = LicencePositionTestUtil.newBuilder().withPositionOrder(1).build();
    var currentPosition = LicencePositionTestUtil.newBuilder().withPositionOrder(2).build();
    var urlContext = PositionChangeUrlContext.forAddedPosition(previousPosition.getId(), currentPosition.getId());

    var previousChronological = ChronologicalPositionTestUtil.live(
        previousPosition,
        new SetEquityOperation(TRANSFER_FROM_ID, BigDecimal.valueOf(100)));
    var currentChronological = ChronologicalPositionTestUtil.live(
        currentPosition,
        LicenceOperation.newTransferEquityOperation()
            .withTransferFrom(TRANSFER_FROM_ID)
            .withTransferTo(TRANSFER_TO_ID)
            .withEquity(BigDecimal.valueOf(30))
            .build());

    var chronologicalPositions = List.of(previousChronological, currentChronological);
    var result = LicencePositionChangeViewResolver.getChangeViews(
        currentPosition.getId(),
        chronologicalPositions,
        LicencePositionStateResolver.resolve(chronologicalPositions),
        Map.of(),
        null,
        urlContext
    );

    var view = (TransferEquityChangeView) result.get(LicenceOperation.TRANSFER_EQUITY);

    var holding = view.holdings().getFirst();
    assertThat(holding.transferFromOrganisationName()).isEqualTo("Not available");
    assertThat(holding.transferToOrganisationName()).isEqualTo("Not available");
  }

  @Test
  void buildTransferEquityChangeView_whenExecutedAddChange_buildsUpdateUrlToExecutedSummary() {
    var correctionId = UUID.randomUUID();
    var licencePositionId = UUID.randomUUID();
    var urlContext = PositionChangeUrlContext.forExecutedPosition(correctionId, licencePositionId);

    var view = transferEquityChangeView(LicencePositionChangeType.ADD_CHANGE, urlContext, null);

    assertThat(view.updateUrl()).isEqualTo(ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderSummaryForExecutedPosition(correctionId, licencePositionId, null)));
  }

  @Test
  void buildTransferEquityChangeView_whenAddedPositionUpdateChangeOperations_buildsUpdateUrlToAddedSummary() {
    var correctionId = UUID.randomUUID();
    var licencePositionCorrectionId = UUID.randomUUID();
    var urlContext = PositionChangeUrlContext.forAddedPosition(correctionId, licencePositionCorrectionId);

    var view = transferEquityChangeView(LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS, urlContext, null);

    assertThat(view.updateUrl()).isEqualTo(ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderSummaryForAddedPosition(correctionId, licencePositionCorrectionId, null)));
  }

  @Test
  void buildTransferEquityChangeView_whenNoUrlContext_hasNoUpdateUrl() {
    var view = transferEquityChangeView(LicencePositionChangeType.ADD_CHANGE, null, null);
    assertThat(view.updateUrl()).isNull();
  }

  @Test
  void buildTransferEquityChangeView_whenUntouchedExecutedChange_hasNoUpdateUrl() {
    var urlContext = PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID());
    var view = transferEquityChangeView(null, urlContext, null);

    assertThat(view.updateUrl()).isNull();
  }

  private SetEquityChangeView setEquityChangeView(String changeType, PositionChangeUrlContext urlContext) {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var change = new PositionChange(
        UUID.randomUUID().toString(),
        1,
        changeType,
        List.of(new SetEquityOperation(SET_EQUITY_ORG_ID, BigDecimal.valueOf(75))));
    var currentChronologicalPosition = ChronologicalPosition.fromLicencePosition(
        currentLicencePosition,
        currentLicencePosition.getPositionDate(),
        currentLicencePosition.getPositionDateOrder(),
        List.of(change));

    var chronologicalPositions = List.of(currentChronologicalPosition);
    var result = LicencePositionChangeViewResolver.getChangeViews(
        currentLicencePosition.getId(),
        chronologicalPositions,
        LicencePositionStateResolver.resolve(chronologicalPositions),
        Map.of(SET_EQUITY_ORG_ID, SET_EQUITY_ORG_NAME),
        Map.of(),
        urlContext
    );

    return (SetEquityChangeView) result.get(LicenceOperation.SET_EQUITY);
  }

  private TransferEquityChangeView transferEquityChangeView(
      String changeType,
      PositionChangeUrlContext urlContext,
      Boolean retainBeneficialInterest
  ) {
    var previousPosition = LicencePositionTestUtil.newBuilder().withPositionOrder(1).build();
    var currentPosition = LicencePositionTestUtil.newBuilder().withPositionOrder(2).build();

    var previousChronological = ChronologicalPositionTestUtil.live(
        previousPosition,
        new SetEquityOperation(TRANSFER_FROM_ID, BigDecimal.valueOf(100)));

    var transferChange = new PositionChange(
        UUID.randomUUID().toString(),
        1,
        changeType,
        List.of(LicenceOperation.newTransferEquityOperation()
            .withTransferFrom(TRANSFER_FROM_ID)
            .withTransferTo(TRANSFER_TO_ID)
            .withEquity(BigDecimal.valueOf(30))
            .withRetainBeneficialInterest(retainBeneficialInterest)
            .build()));
    var currentChronological = ChronologicalPosition.fromLicencePosition(
        currentPosition,
        currentPosition.getPositionDate(),
        currentPosition.getPositionDateOrder(),
        List.of(transferChange));

    var chronologicalPositions = List.of(previousChronological, currentChronological);
    var result = LicencePositionChangeViewResolver.getChangeViews(
        currentPosition.getId(),
        chronologicalPositions,
        LicencePositionStateResolver.resolve(chronologicalPositions),
        Map.of(TRANSFER_FROM_ID, TRANSFER_FROM_NAME, TRANSFER_TO_ID, TRANSFER_TO_NAME),
        null,
        urlContext
    );

    return (TransferEquityChangeView) result.get(LicenceOperation.TRANSFER_EQUITY);
  }

  @Test
  void getChangeViews_buildsPartialSurrenderChangeView() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.AUGUST, 1))
        .build();

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newPartialSurrenderOperation()
            .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
            .build()
    );

    var result = changeViewsFor(currentLicencePosition.getId(), FEATURE_NAMES, currentChronologicalPosition);

    assertThat(result)
        .extractingByKey(LicenceOperation.PARTIAL_SURRENDER)
        .isInstanceOf(PartialSurrenderChangeView.class);

    var partialSurrenderChangeView = (PartialSurrenderChangeView) result.get(LicenceOperation.PARTIAL_SURRENDER);
    assertThat(partialSurrenderChangeView.blockLabels()).containsExactly("30/1a", "30/2");
  }

  @Test
  void getChangeViews_whenBlockNameNotFound_usesEmptyLabel() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.AUGUST, 1))
        .build();

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newPartialSurrenderOperation()
            .withFeatureIds(List.of(FIRST_FEATURE_ID))
            .build()
    );

    var result = changeViewsFor(currentLicencePosition.getId(), Map.of(), currentChronologicalPosition);

    var partialSurrenderChangeView = (PartialSurrenderChangeView) result.get(LicenceOperation.PARTIAL_SURRENDER);
    assertThat(partialSurrenderChangeView.blockLabels()).containsExactly("");
  }

  @Test
  void getChangeViews_whenPartialSurrenderHasNoOwnDate_usesThePositionDate() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.AUGUST, 1))
        .build();

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newPartialSurrenderOperation()
            .withFeatureIds(List.of(FIRST_FEATURE_ID))
            .build()
    );

    var result = changeViewsFor(currentLicencePosition.getId(), FEATURE_NAMES, currentChronologicalPosition);

    var partialSurrenderChangeView = (PartialSurrenderChangeView) result.get(LicenceOperation.PARTIAL_SURRENDER);
    assertThat(partialSurrenderChangeView.surrenderDate()).isEqualTo("1 August 2026");
  }

  @Test
  void getChangeViews_whenPartialSurrenderHasItsOwnDate_usesThatDate() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.AUGUST, 1))
        .build();

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newPartialSurrenderOperation()
            .withSurrenderDate(LocalDate.of(2026, Month.SEPTEMBER, 30))
            .withFeatureIds(List.of(FIRST_FEATURE_ID))
            .build()
    );

    var result = changeViewsFor(currentLicencePosition.getId(), FEATURE_NAMES, currentChronologicalPosition);

    var partialSurrenderChangeView = (PartialSurrenderChangeView) result.get(LicenceOperation.PARTIAL_SURRENDER);
    assertThat(partialSurrenderChangeView.surrenderDate()).isEqualTo("30 September 2026");
  }

  private static Map<String, LicencePositionChangeView> changeViewsFor(
      UUID currentPositionId,
      Map<UUID, String> featureNames,
      ChronologicalPosition... chronologicalPositions
  ) {
    var positions = List.of(chronologicalPositions);

    return LicencePositionChangeViewResolver.getChangeViews(
        currentPositionId,
        positions,
        LicencePositionStateResolver.resolve(positions),
        Map.of(),
        featureNames,
        null
    );
  }
}