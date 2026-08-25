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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.RemoveAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity.RemoveEquityChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.LicencePositionTransferEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
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
        PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
    );

    assertThat(view.urls().correct()).contains("correct-administrator-change");
    assertThat(view.urls().remove()).contains("remove-administrator-change");
    assertThat(view.urls().undo()).isNull();
  }

  @Test
  void buildAdministratorChange_whenExecutedUpdateChange_populatesCorrectAndUndoNotRemove() {
    var view = adminChangeView(
        LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS,
        PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
    );

    assertThat(view.urls().correct()).contains("correct-administrator-change");
    assertThat(view.urls().remove()).isNull();
    assertThat(view.urls().undo()).contains("undo-administrator-change");
  }

  @Test
  void buildAdministratorChange_whenAddChange_populatesUndoNotRemove() {
    var view = adminChangeView(
        LicencePositionChangeType.ADD_CHANGE,
        PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
    );

    assertThat(view.urls().correct()).contains("add-administrator-change");
    assertThat(view.urls().remove()).isNull();
    assertThat(view.urls().undo()).contains("undo-administrator-change");
  }

  @Test
  void buildAdministratorChange_whenRemoveChange_populatesUndoOnly() {
    var view = adminChangeView(
        LicencePositionChangeType.REMOVE_CHANGE,
        PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
    );

    assertThat(view.urls().correct()).isNull();
    assertThat(view.urls().remove()).isNull();
    assertThat(view.urls().undo()).contains("undo-administrator-change");
  }

  @Test
  void buildAdministratorChange_whenAddedPosition_populatesUndoNotRemove() {
    var view = adminChangeView(
        LicencePositionChangeType.ADD_CHANGE,
        PositionChangeUrlContext.forAddedPosition(UUID.randomUUID(), UUID.randomUUID())
    );

    assertThat(view.urls().remove()).isNull();
    assertThat(view.urls().undo()).contains("undo-administrator-change");
  }

  @Test
  void buildAdministratorChange_whenNoUrlContext_hasNoUrls() {
    var view = adminChangeView(LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS, null);

    assertThat(view.urls().correct()).isNull();
    assertThat(view.urls().remove()).isNull();
    assertThat(view.urls().undo()).isNull();
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

    var setEquityChangeView = (SetEquityChangeView) result.get(LicenceOperation.SET_EQUITY);
    assertThat(setEquityChangeView)
        .usingRecursiveComparison()
        .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
        .isEqualTo(new SetEquityChangeView(
            List.of(new SetEquityRow("Org", BigDecimal.valueOf(75))),
            null,
            ChangeViewUrls.none()
        ));
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
  void buildSetEquityChangeView_whenExecutedAddChange_buildsViewWithRowChangeTypeUpdateAndUndoUrls() {
    var correctionId = UUID.randomUUID();
    var licencePositionId = UUID.randomUUID();
    var changeId = UUID.randomUUID().toString();
    var urlContext = PositionChangeUrlContext.forExecutedPosition(correctionId, licencePositionId, UUID.randomUUID());

    var view = setEquityChangeView(changeId, LicencePositionChangeType.ADD_CHANGE, urlContext);

    assertThat(view).isEqualTo(new SetEquityChangeView(
        List.of(new SetEquityRow(SET_EQUITY_ORG_NAME, BigDecimal.valueOf(75))),
        LicencePositionChangeType.ADD_CHANGE,
        new ChangeViewUrls(
            ReverseRouter.route(on(LicencePositionSetEquityController.class)
                .renderSummaryForExecutedPosition(correctionId, licencePositionId, null)),
            null,
            ReverseRouter.route(on(RemoveEquityChangeController.class)
                .renderUndoEquityChange(correctionId, changeId, null)),
            null
        )
    ));
  }

  @Test
  void buildSetEquityChangeView_whenExecutedUpdateChangeOperations_buildsUpdateUrlToExecutedSummary() {
    var correctionId = UUID.randomUUID();
    var licencePositionId = UUID.randomUUID();
    var urlContext = PositionChangeUrlContext.forExecutedPosition(correctionId, licencePositionId, UUID.randomUUID());

    var view = setEquityChangeView(LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS, urlContext);

    assertThat(view.urls().correct()).isEqualTo(ReverseRouter.route(on(LicencePositionSetEquityController.class)
        .renderSummaryForExecutedPosition(correctionId, licencePositionId, null)));
  }

  @Test
  void buildSetEquityChangeView_whenAddedPositionAddChange_buildsUpdateUrlToAddedSummary() {
    var correctionId = UUID.randomUUID();
    var licencePositionCorrectionId = UUID.randomUUID();
    var urlContext = PositionChangeUrlContext.forAddedPosition(correctionId, licencePositionCorrectionId);

    var view = setEquityChangeView(LicencePositionChangeType.ADD_CHANGE, urlContext);

    assertThat(view.urls().correct()).isEqualTo(ReverseRouter.route(on(LicencePositionSetEquityController.class)
        .renderSummaryForAddedPosition(correctionId, licencePositionCorrectionId, null)));
  }

  @Test
  void buildSetEquityChangeView_whenNoUrlContext_hasNoUpdateUrl() {
    var view = setEquityChangeView(LicencePositionChangeType.ADD_CHANGE, null);
    assertThat(view.urls().correct()).isNull();
  }

  @Test
  void buildSetEquityChangeView_whenUntouchedExecutedChange_hasNoUpdateUrl() {
    var urlContext = PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID(), null);
    var view = setEquityChangeView(null, urlContext);

    assertThat(view.urls().correct()).isNull();
  }

  @Test
  void buildSetEquityChangeView_whenUntouchedExecutedChange_populatesRemoveNotUndo() {
    var urlContext = PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID(), null);
    var view = setEquityChangeView(null, urlContext);

    assertThat(view.urls().remove()).contains("remove-equity-change");
    assertThat(view.urls().undo()).isNull();
  }

  @Test
  void buildSetEquityChangeView_whenExecutedUpdateChange_populatesUndoNotRemove() {
    var urlContext = PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    var view = setEquityChangeView(LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS, urlContext);

    assertThat(view.urls().remove()).isNull();
    assertThat(view.urls().undo()).contains("undo-equity-change");
  }

  @Test
  void buildSetEquityChangeView_whenAddChange_populatesUndoNotRemove() {
    var urlContext = PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    var view = setEquityChangeView(LicencePositionChangeType.ADD_CHANGE, urlContext);

    assertThat(view.urls().remove()).isNull();
    assertThat(view.urls().undo()).contains("undo-equity-change");
  }

  @Test
  void buildSetEquityChangeView_whenRemoveChange_populatesUndoOnly() {
    var urlContext = PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    var view = setEquityChangeView(LicencePositionChangeType.REMOVE_CHANGE, urlContext);

    assertThat(view.urls().remove()).isNull();
    assertThat(view.urls().undo()).contains("undo-equity-change");
  }

  @Test
  void buildSetEquityChangeView_whenAddedPosition_populatesUndoNotRemove() {
    var urlContext = PositionChangeUrlContext.forAddedPosition(UUID.randomUUID(), UUID.randomUUID());
    var view = setEquityChangeView(LicencePositionChangeType.ADD_CHANGE, urlContext);

    assertThat(view.urls().remove()).isNull();
    assertThat(view.urls().undo()).contains("undo-equity-change");
  }

  @Test
  void buildSetEquityChangeView_whenNoUrlContext_hasNoRemoveOrUndoUrls() {
    var view = setEquityChangeView(LicencePositionChangeType.ADD_CHANGE, null);

    assertThat(view.urls().remove()).isNull();
    assertThat(view.urls().undo()).isNull();
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
        ChangeViewUrls.none()
    );

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
    var urlContext = PositionChangeUrlContext.forExecutedPosition(correctionId, licencePositionId, UUID.randomUUID());

    var view = transferEquityChangeView(LicencePositionChangeType.ADD_CHANGE, urlContext, null);

    assertThat(view.urls().correct()).isEqualTo(ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderSummaryForExecutedPosition(correctionId, licencePositionId, null)));
  }

  @Test
  void buildTransferEquityChangeView_whenAddedPositionUpdateChangeOperations_buildsUpdateUrlToAddedSummary() {
    var correctionId = UUID.randomUUID();
    var licencePositionCorrectionId = UUID.randomUUID();
    var urlContext = PositionChangeUrlContext.forAddedPosition(correctionId, licencePositionCorrectionId);

    var view = transferEquityChangeView(LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS, urlContext, null);

    assertThat(view.urls().correct()).isEqualTo(ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderSummaryForAddedPosition(correctionId, licencePositionCorrectionId, null)));
  }

  @Test
  void buildTransferEquityChangeView_whenNoUrlContext_hasNoUpdateUrl() {
    var view = transferEquityChangeView(LicencePositionChangeType.ADD_CHANGE, null, null);
    assertThat(view.urls().correct()).isNull();
  }

  @Test
  void buildTransferEquityChangeView_whenUntouchedExecutedChange_hasNoUpdateUrl() {
    var urlContext = PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID(), null);
    var view = transferEquityChangeView(null, urlContext, null);

    assertThat(view.urls().correct()).isNull();
  }

  @Test
  void buildTransferEquityChangeView_whenUntouchedExecutedChange_populatesRemoveNotUndo() {
    var urlContext = PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID(), null);
    var view = transferEquityChangeView(null, urlContext, null);

    assertThat(view.urls().remove()).contains("remove-equity-change");
    assertThat(view.urls().undo()).isNull();
  }

  @Test
  void buildTransferEquityChangeView_whenExecutedAddChange_populatesUndoNotRemove() {
    var urlContext = PositionChangeUrlContext.forExecutedPosition(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    var view = transferEquityChangeView(LicencePositionChangeType.ADD_CHANGE, urlContext, null);

    assertThat(view.urls().remove()).isNull();
    assertThat(view.urls().undo()).contains("undo-equity-change");
  }

  private SetEquityChangeView setEquityChangeView(String changeType, PositionChangeUrlContext urlContext) {
    return setEquityChangeView(UUID.randomUUID().toString(), changeType, urlContext);
  }

  private SetEquityChangeView setEquityChangeView(
      String changeId,
      String changeType,
      PositionChangeUrlContext urlContext
  ) {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder().build();

    var change = new PositionChange(
        changeId,
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
            .withBlockSurrenderTypeByFeatureId(Map.of(
                FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER,
                SECOND_FEATURE_ID, BlockSurrenderType.PARTIAL_SURRENDER))
            .build()
    );

    var result = changeViewsFor(currentLicencePosition.getId(), FEATURE_NAMES, currentChronologicalPosition);

    assertThat(result)
        .extractingByKey(LicenceOperation.PARTIAL_SURRENDER)
        .isInstanceOf(PartialSurrenderChangeView.class);

    var partialSurrenderChangeView = (PartialSurrenderChangeView) result.get(LicenceOperation.PARTIAL_SURRENDER);
    assertThat(partialSurrenderChangeView.blockRows()).containsExactly(
        new PartialSurrenderChangeView.BlockRow("30/1a", "Full surrender"),
        new PartialSurrenderChangeView.BlockRow("30/2", "Partial surrender"));
  }

  @Test
  void getChangeViews_whenBlockNameNotFound_usesNotAvailable() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.AUGUST, 1))
        .build();

    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newPartialSurrenderOperation()
            .withFeatureIds(List.of(FIRST_FEATURE_ID))
            .withBlockSurrenderTypeByFeatureId(Map.of(FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER))
            .build()
    );

    var result = changeViewsFor(currentLicencePosition.getId(), Map.of(), currentChronologicalPosition);

    var partialSurrenderChangeView = (PartialSurrenderChangeView) result.get(LicenceOperation.PARTIAL_SURRENDER);
    assertThat(partialSurrenderChangeView.blockRows())
        .containsExactly(new PartialSurrenderChangeView.BlockRow("Not available", "Full surrender"));
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
            .withBlockSurrenderTypeByFeatureId(Map.of(FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER))
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
            .withBlockSurrenderTypeByFeatureId(Map.of(FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER))
            .build()
    );

    var result = changeViewsFor(currentLicencePosition.getId(), FEATURE_NAMES, currentChronologicalPosition);

    var partialSurrenderChangeView = (PartialSurrenderChangeView) result.get(LicenceOperation.PARTIAL_SURRENDER);
    assertThat(partialSurrenderChangeView.surrenderDate()).isEqualTo("30 September 2026");
  }

  @Test
  void getChangeViews_whenABlockHasNoSurrenderTypeYet_thenTheBlockRowHasNoSurrenderType() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.AUGUST, 1))
        .build();
    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newPartialSurrenderOperation()
            .withFeatureIds(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID))
            .withBlockSurrenderTypeByFeatureId(Map.of(FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER))
            .build()
    );

    var result = changeViewsFor(currentLicencePosition.getId(), FEATURE_NAMES, currentChronologicalPosition);

    assertThat((PartialSurrenderChangeView) result.get(LicenceOperation.PARTIAL_SURRENDER))
        .extracting(PartialSurrenderChangeView::blockRows)
        .isEqualTo(List.of(
            new PartialSurrenderChangeView.BlockRow(FEATURE_NAMES.get(FIRST_FEATURE_ID), "Full surrender"),
            new PartialSurrenderChangeView.BlockRow(FEATURE_NAMES.get(SECOND_FEATURE_ID), null)));
  }

  @Test
  void getChangeViews_whenNoUrlContext_partialSurrenderHasNoCorrectUrl() {
    var currentLicencePosition = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.AUGUST, 1))
        .build();
    var currentChronologicalPosition = ChronologicalPositionTestUtil.live(
        currentLicencePosition,
        LicenceOperation.newPartialSurrenderOperation()
            .withFeatureIds(List.of(FIRST_FEATURE_ID))
            .withBlockSurrenderTypeByFeatureId(Map.of(FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER))
            .build());

    var result = changeViewsFor(currentLicencePosition.getId(), FEATURE_NAMES, currentChronologicalPosition);

    assertThat((PartialSurrenderChangeView) result.get(LicenceOperation.PARTIAL_SURRENDER))
        .extracting(view -> view.urls().correct())
        .isNull();
  }

  @Test
  void getChangeViews_whenPartialSurrenderIsAnUntouchedLiveChange_linksToTheTaskListForThatChange() {
    var correctionId = UUID.randomUUID();
    var positionId = UUID.randomUUID();
    var changeId = UUID.randomUUID().toString();

    var result = partialSurrenderChangeView(
        positionId, changeId, null,
        PositionChangeUrlContext.forExecutedPosition(correctionId, positionId, null)
    );

    assertThat(result.urls().correct()).isEqualTo(
        ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderForCorrectingChange(correctionId, positionId, changeId, null, null)));
  }

  @Test
  void getChangeViews_whenPartialSurrenderIsAlreadyCorrected_linksToTheStagedTaskList() {
    var correctionId = UUID.randomUUID();
    var positionId = UUID.randomUUID();
    var positionCorrectionId = UUID.randomUUID();

    var result = partialSurrenderChangeView(
        positionId, UUID.randomUUID().toString(),
        LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS,
        PositionChangeUrlContext.forExecutedPosition(correctionId, positionId, positionCorrectionId)
    );

    assertThat(result.urls().correct()).isEqualTo(
        ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderTaskList(correctionId, positionCorrectionId, null, null)));
  }

  @Test
  void getChangeViews_whenPartialSurrenderStagedAsAnAddChange_linksToTheStagedTaskList() {
    var correctionId = UUID.randomUUID();
    var positionId = UUID.randomUUID();
    var positionCorrectionId = UUID.randomUUID();

    var result = partialSurrenderChangeView(
        positionId, UUID.randomUUID().toString(), LicencePositionChangeType.ADD_CHANGE,
        PositionChangeUrlContext.forExecutedPosition(correctionId, positionId, positionCorrectionId)
    );

    assertThat(result.urls().correct()).isEqualTo(
        ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderTaskList(correctionId, positionCorrectionId, null, null)));
  }

  @Test
  void getChangeViews_whenPartialSurrenderIsRemoved_hasNoCorrectUrl() {
    var correctionId = UUID.randomUUID();
    var positionId = UUID.randomUUID();

    var result = partialSurrenderChangeView(
        positionId, UUID.randomUUID().toString(), LicencePositionChangeType.REMOVE_CHANGE,
        PositionChangeUrlContext.forExecutedPosition(correctionId, positionId, UUID.randomUUID())
    );

    assertThat(result.urls().correct()).isNull();
  }

  @Test
  void getChangeViews_whenPartialSurrenderOnAnAddedPosition_linksToTheStagedTaskList() {
    var correctionId = UUID.randomUUID();
    var positionId = UUID.randomUUID();
    var positionCorrectionId = UUID.randomUUID();

    var result = partialSurrenderChangeView(
        positionId, UUID.randomUUID().toString(), LicencePositionChangeType.ADD_CHANGE,
        PositionChangeUrlContext.forAddedPosition(correctionId, positionCorrectionId)
    );

    assertThat(result.urls().correct()).isEqualTo(
        ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderTaskList(correctionId, positionCorrectionId, null, null)));
  }

  private static PartialSurrenderChangeView partialSurrenderChangeView(
      UUID positionId,
      String changeId,
      String changeType,
      PositionChangeUrlContext urlContext
  ) {
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FIRST_FEATURE_ID))
        .withBlockSurrenderTypeByFeatureId(Map.of(FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER))
        .build();
    var positions = List.of(ChronologicalPositionTestUtil.newBuilder()
        .withId(positionId)
        .withDate(LocalDate.of(2026, Month.AUGUST, 1))
        .withChanges(List.of(new PositionChange(changeId, 1, changeType, List.of(operation))))
        .build());

    var result = LicencePositionChangeViewResolver.getChangeViews(
        positionId,
        positions,
        LicencePositionStateResolver.resolve(positions),
        Map.of(),
        FEATURE_NAMES,
        urlContext);

    return (PartialSurrenderChangeView) result.get(LicenceOperation.PARTIAL_SURRENDER);
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


  @Test
  void getChangeViews_whenMultipleOrderableChangeTypes_populatesCorrectChangeOrderUrlForEachChange() {
    var correctionId = UUID.randomUUID();
    var positionId = UUID.randomUUID();
    var setEquityChangeId = UUID.randomUUID();
    var partialSurrenderChangeId = UUID.randomUUID();

    var result = changeOrderChangeViews(
        positionId,
        List.of(setEquityChange(setEquityChangeId.toString(), null),
            partialSurrenderChange(partialSurrenderChangeId.toString(), null)),
        PositionChangeUrlContext.forExecutedPosition(correctionId, positionId, null)
    );

    assertThat(result)
        .extractingByKeys(LicenceOperation.SET_EQUITY, LicenceOperation.PARTIAL_SURRENDER)
        .extracting(LicencePositionChangeView::urls)
        .containsExactly(
            new ChangeViewUrls(
                null,
                ReverseRouter.route(on(RemoveEquityChangeController.class).renderRemoveExecutedEquityChange(
                    correctionId, positionId, setEquityChangeId.toString(), null)),
                null,
                ReverseRouter.route(on(CorrectChangeOrderController.class)
                    .renderCorrectChangeOrder(correctionId, positionId, setEquityChangeId, null))),
            new ChangeViewUrls(
                ReverseRouter.route(on(PartialSurrenderTaskListController.class).renderForCorrectingChange(
                    correctionId, positionId, partialSurrenderChangeId.toString(), null, null)),
                null,
                null,
                ReverseRouter.route(on(CorrectChangeOrderController.class)
                    .renderCorrectChangeOrder(correctionId, positionId, partialSurrenderChangeId, null))));
  }

  @Test
  void getChangeViews_whenOnlyOneOrderableChangeType_hasNoCorrectChangeOrderUrl() {
    var correctionId = UUID.randomUUID();
    var positionId = UUID.randomUUID();
    var changeId = UUID.randomUUID();

    var result = changeOrderChangeViews(
        positionId,
        List.of(setEquityChange(changeId.toString(), null)),
        PositionChangeUrlContext.forExecutedPosition(correctionId, positionId, null)
    );

    assertThat(result)
        .extractingByKey(LicenceOperation.SET_EQUITY)
        .extracting(LicencePositionChangeView::urls)
        .isEqualTo(new ChangeViewUrls(
            null,
            ReverseRouter.route(on(RemoveEquityChangeController.class).renderRemoveExecutedEquityChange(
                correctionId, positionId, changeId.toString(), null)),
            null,
            null));
  }

  @Test
  void getChangeViews_whenChangeIsRemoved_hasNoCorrectChangeOrderUrlForThatChange() {
    var correctionId = UUID.randomUUID();
    var positionId = UUID.randomUUID();
    var administratorChangeId = UUID.randomUUID();

    var result = changeOrderChangeViews(
        positionId,
        List.of(setEquityChange(UUID.randomUUID().toString(), null),
            partialSurrenderChange(UUID.randomUUID().toString(), null),
            administratorChange(administratorChangeId.toString(), LicencePositionChangeType.REMOVE_CHANGE)),
        PositionChangeUrlContext.forExecutedPosition(correctionId, positionId, null)
    );

    assertThat(result)
        .extractingByKey(LicenceOperation.LICENCE_ADMINISTRATOR)
        .extracting(LicencePositionChangeView::urls)
        .isEqualTo(new ChangeViewUrls(
            null,
            null,
            ReverseRouter.route(on(RemoveAdministratorChangeController.class)
                .renderUndoAdminChange(correctionId, administratorChangeId.toString(), null)),
            null));
  }

  @Test
  void getChangeViews_whenNoUrlContext_hasNoCorrectChangeOrderUrl() {
    var positionId = UUID.randomUUID();

    var result = changeOrderChangeViews(
        positionId,
        List.of(setEquityChange(UUID.randomUUID().toString(), null),
            partialSurrenderChange(UUID.randomUUID().toString(), null)),
        null
    );

    assertThat(result)
        .extractingByKeys(LicenceOperation.SET_EQUITY, LicenceOperation.PARTIAL_SURRENDER)
        .extracting(LicencePositionChangeView::urls)
        .containsOnly(ChangeViewUrls.none());
  }

  private static Map<String, LicencePositionChangeView> changeOrderChangeViews(
      UUID positionId,
      List<PositionChange> changes,
      PositionChangeUrlContext urlContext
  ) {
    var positions = List.of(ChronologicalPositionTestUtil.newBuilder()
        .withId(positionId)
        .withChanges(changes)
        .build());

    return LicencePositionChangeViewResolver.getChangeViews(
        positionId,
        positions,
        LicencePositionStateResolver.resolve(positions),
        Map.of(JOINING_ID, JOINING_NAME, SET_EQUITY_ORG_ID, SET_EQUITY_ORG_NAME),
        FEATURE_NAMES,
        urlContext);
  }

  private static PositionChange administratorChange(String changeId, String changeType) {
    return new PositionChange(changeId, 1, changeType,
        List.of(LicenceOperation.newAdministratorChange().withOperator(JOINING_ID).build()));
  }

  private static PositionChange setEquityChange(String changeId, String changeType) {
    return new PositionChange(changeId, 2, changeType,
        List.of(new SetEquityOperation(SET_EQUITY_ORG_ID, BigDecimal.valueOf(75))));
  }

  private static PositionChange partialSurrenderChange(String changeId, String changeType) {
    return new PositionChange(changeId, 3, changeType,
        List.of(LicenceOperation.newPartialSurrenderOperation()
            .withFeatureIds(List.of(FIRST_FEATURE_ID))
            .withBlockSurrenderTypeByFeatureId(Map.of(FIRST_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER))
            .build()));
  }
}
