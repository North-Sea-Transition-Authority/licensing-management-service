package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.AdministratorPositionRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.EquityPositionRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.LicenseePositionRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationRule;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.SubareaEquityPositionRule;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

public record ChronologicalPosition(
    UUID id,
    UUID transactionId,
    String reference,
    LocalDate date,
    int order,
    List<PositionChange> changes
) {

  private static final BigDecimal ONE_HUNDRED_PERCENT = new BigDecimal("100");

  private static final ChangeCountRules LICENSEE_RULES = new ChangeCountRules(
      LicenceOperation.LICENSEE,
      LicenseePositionRule.FIRST_POSITION_MUST_HAVE_LICENSEE,
      LicenseePositionRule.ONLY_ONE_LICENSEE_CHANGE
  );

  private static final ChangeCountRules ADMINISTRATOR_RULES = new ChangeCountRules(
      LicenceOperation.LICENCE_ADMINISTRATOR,
      AdministratorPositionRule.FIRST_POSITION_MUST_HAVE_ADMINISTRATOR,
      AdministratorPositionRule.ONLY_ONE_ADMINISTRATOR_CHANGE
  );

  /**
   * The reference, date and order are passed in rather than read off the position because a correction can change any
   * of them - the caller resolves the corrected values.
   */
  public static ChronologicalPosition fromLicencePosition(
      LicencePosition position,
      String reference,
      LocalDate date,
      int order,
      List<PositionChange> changes
  ) {
    return new ChronologicalPosition(
        position.getId(),
        position.getLicenceTransaction().getId(),
        reference,
        date,
        order,
        changes
    );
  }

  public static ChronologicalPosition fromPayload(CreateLicencePositionPayload payload) {
    return new ChronologicalPosition(
        UUID.fromString(payload.licencePositionId()),
        UUID.fromString(payload.licenceTransactionId()),
        payload.correctionReference(),
        payload.effectiveDate(),
        payload.effectiveDateOrder(),
        PositionChange.fromPayload(payload)
    );
  }

  public String positionName() {
    return DateUtil.formatLongDateWithOrder(date, order);
  }

  public boolean hasMovedAwayChange() {
    return changes.stream().anyMatch(PositionChange::movedAway);
  }

  /**
   * This position without the changes a correction removes or moves to another position, which no longer apply to it.
   */
  public ChronologicalPosition withoutRemovedChanges() {
    return new ChronologicalPosition(
        id,
        transactionId,
        reference,
        date,
        order,
        changes.stream()
            .filter(change -> !LicencePositionChangeType.REMOVE_CHANGE.equals(change.changeType()))
            .toList()
    );
  }

  public List<PositionValidationError> validate(PositionValidationContext positionValidationContext) {
    var positionValidationErrors = new ArrayList<PositionValidationError>();

    positionValidationErrors.addAll(validateAdministratorChange(positionValidationContext));
    positionValidationErrors.addAll(validateBeneficialInterest(positionValidationContext));
    positionValidationErrors.addAll(validateLicenseeChange(positionValidationContext));
    positionValidationErrors.addAll(validateSubareaEquityTotals(positionValidationContext));

    for (var change : changes) {
      positionValidationErrors.addAll(change.validate(positionValidationContext));
    }

    return positionValidationErrors;
  }

  public long beneficialInterestChangeCount() {
    return changes.stream()
        .filter(ChronologicalPosition::isBeneficialInterestChange)
        .count();
  }

  public List<String> subareaEquityChangeKeys() {
    return changes.stream()
        .filter(change -> !LicencePositionChangeType.REMOVE_CHANGE.equals(change.changeType()))
        .flatMap(change -> change.operations().stream()
            .filter(SubareaSetEquityOperation.class::isInstance)
            .map(SubareaSetEquityOperation.class::cast)
            .map(SubareaSetEquityOperation::subareaKey)
            .distinct()
        )
        .toList();
  }

  public List<Integer> joiningLicensees() {
    return changes.stream()
        .map(PositionChange::licenseesToAdd)
        .flatMap(Collection::stream)
        .toList();
  }

  public List<Integer> withdrawingLicensees() {
    return changes.stream()
        .map(PositionChange::licenseesToRemove)
        .flatMap(Collection::stream)
        .toList();
  }

  private List<PositionValidationError> validateLicenseeChange(PositionValidationContext positionValidationContext) {
    List<PositionValidationError> changeCountErrors = validateOperationChangeCount(
        positionValidationContext,
        LICENSEE_RULES
    );

    if (!changeCountErrors.isEmpty()) {
      return changeCountErrors;
    }

    var previousState = positionValidationContext.previousState();
    if (previousState == null) {
      return List.of();
    }

    var errors = new ArrayList<PositionValidationError>();
    if (positionValidationContext.resolvedState().licenseeIds().isEmpty()
        && (positionValidationContext.isExtantLicence() || !positionValidationContext.isLastPosition())) {
      errors.add(PositionValidationError.forPosition(
          positionValidationContext,
          LicenseePositionRule.POSITION_MUST_HAVE_LICENSEES
      ));
    }

    Set<Integer> joiningLicensees = new HashSet<>(joiningLicensees());
    Set<Integer> withdrawingLicensees = new HashSet<>(withdrawingLicensees());

    if (!Collections.disjoint(joiningLicensees, previousState.licenseeIds())) {
      errors.add(PositionValidationError.forPosition(
          positionValidationContext,
          LicenseePositionRule.CANNOT_JOIN_A_LICENCE_REPEATEDLY
      ));
    }

    boolean withdrawingLicenseesDontExist = withdrawingLicensees
        .stream()
        .anyMatch(licensee -> !previousState.licenseeIds().contains(licensee));

    if (withdrawingLicenseesDontExist) {
      errors.add(PositionValidationError.forPosition(
          positionValidationContext,
          LicenseePositionRule.MUST_EXIST_TO_WITHDRAW
      ));
    }

    return errors;
  }

  private List<PositionValidationError> validateAdministratorChange(PositionValidationContext positionValidationContext) {
    if (positionValidationContext.isCarbonStorage()) {
      return List.of();
    }

    return validateOperationChangeCount(positionValidationContext, ADMINISTRATOR_RULES);
  }

  private List<PositionValidationError> validateOperationChangeCount(
      PositionValidationContext positionValidationContext,
      ChangeCountRules rules
  ) {
    var changeCount = changes.stream()
        .filter(change -> !LicencePositionChangeType.REMOVE_CHANGE.equals(change.changeType()))
        .flatMap(change -> change.operations().stream())
        .filter(licenceOperation -> rules.operationType().equals(licenceOperation.type()))
        .count();

    if (positionValidationContext.isFirstPosition() && changeCount == 0) {
      return List.of(PositionValidationError.forPosition(
          positionValidationContext,
          rules.firstPositionMustHaveChange()
      ));
    }

    if (changeCount > 1) {
      return List.of(PositionValidationError.forPosition(
          positionValidationContext,
          rules.atMostOneChange()
      ));
    }

    return List.of();
  }

  private List<PositionValidationError> validateBeneficialInterest(PositionValidationContext positionValidationContext) {
    if (!positionValidationContext.isCarbonStorage()) {
      return List.of();
    }

    return validateEquityTotals(positionValidationContext);
  }

  private List<PositionValidationError> validateSubareaEquityTotals(PositionValidationContext positionValidationContext) {
    var subareaEquityByBlock = positionValidationContext.resolvedState().blockFeatureIdToSubareaEquity();
    var previousSubareaEquityByBlock = positionValidationContext.previousState().blockFeatureIdToSubareaEquity();

    var anyOutOfRange = false;
    var anyInvalidTotal = false;
    for (var blockEntry : subareaEquityByBlock.entrySet()) {
      var blockFeatureId = blockEntry.getKey();
      for (var subareaEntry : blockEntry.getValue().entrySet()) {
        var subarea = subareaEntry.getKey();
        var equity = subareaEntry.getValue();
        var previousEquity = previousSubareaEquityByBlock
            .getOrDefault(blockFeatureId, Map.of())
            .getOrDefault(subarea, Map.of());
        if (hasNewOutOfRangeHolders(equity, previousEquity)) {
          anyOutOfRange = true;
        }

        if (hasNewInvalidTotal(equity, previousEquity)) {
          anyInvalidTotal = true;
        }
      }
    }

    var errors = new ArrayList<PositionValidationError>();

    if (anyOutOfRange) {
      errors.add(PositionValidationError.forPosition(
          positionValidationContext,
          SubareaEquityPositionRule.EQUITY_HOLDER_OUT_OF_RANGE
      ));
    }

    if (anyInvalidTotal) {
      errors.add(PositionValidationError.forPosition(
          positionValidationContext,
          SubareaEquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED
      ));
    }

    return errors;
  }

  private List<PositionValidationError> validateEquityTotals(PositionValidationContext positionValidationContext) {
    var equityByOrganisationId = positionValidationContext.resolvedState().equityByOrganisationId();
    var hasBeneficialInterestChange = beneficialInterestChangeCount() > 0;

    if (equityByOrganisationId.isEmpty() && !hasBeneficialInterestChange) {
      return List.of();
    }

    var previousEquityByOrganisationId = positionValidationContext.previousState().equityByOrganisationId();

    var errors = new ArrayList<PositionValidationError>();

    if (hasNewOutOfRangeHolders(equityByOrganisationId, previousEquityByOrganisationId)) {
      errors.add(PositionValidationError.forPosition(
          positionValidationContext,
          EquityPositionRule.EQUITY_HOLDER_OUT_OF_RANGE
      ));
    }

    if (hasNewInvalidTotal(equityByOrganisationId, previousEquityByOrganisationId)) {
      errors.add(PositionValidationError.forPosition(
          positionValidationContext,
          EquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED
      ));
    }

    return errors;
  }

  private static boolean hasNewOutOfRangeHolders(Map<Integer, BigDecimal> equity, Map<Integer, BigDecimal> previousEquity) {
    var outOfRangeHolderIds = outOfRangeHolderIds(equity);
    return !outOfRangeHolderIds.isEmpty() && !outOfRangeHolderIds.equals(outOfRangeHolderIds(previousEquity));
  }

  private static boolean hasNewInvalidTotal(Map<Integer, BigDecimal> equity, Map<Integer, BigDecimal> previousEquity) {
    var totalEquity = totalEquity(equity);
    var previousTotalEquity = totalEquity(previousEquity);
    var previousPositionWasInvalid = !previousEquity.isEmpty()
        && previousTotalEquity.compareTo(ONE_HUNDRED_PERCENT) != 0;
    var carriesForwardSameInvalidTotal = previousPositionWasInvalid
        && totalEquity.compareTo(previousTotalEquity) == 0;

    return totalEquity.compareTo(ONE_HUNDRED_PERCENT) != 0 && !carriesForwardSameInvalidTotal;
  }

  private static BigDecimal totalEquity(Map<Integer, BigDecimal> equityByOrganisationId) {
    return equityByOrganisationId.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static Set<Integer> outOfRangeHolderIds(Map<Integer, BigDecimal> equityByOrganisationId) {
    return equityByOrganisationId.entrySet().stream()
        .filter(entry -> isOutOfRange(entry.getValue()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  private static boolean isBeneficialInterestChange(PositionChange change) {
    if (Objects.equals(change.changeType(), LicencePositionChangeType.REMOVE_CHANGE)) {
      return false;
    }
    return change.operations().stream()
        .map(LicenceOperation::type)
        .anyMatch(type -> LicenceOperation.SET_EQUITY.equals(type) || LicenceOperation.TRANSFER_EQUITY.equals(type));
  }

  private static boolean isOutOfRange(BigDecimal equity) {
    return equity.compareTo(BigDecimal.ZERO) < 0 || equity.compareTo(ONE_HUNDRED_PERCENT) > 0;
  }

  private record ChangeCountRules(
      String operationType,
      PositionValidationRule firstPositionMustHaveChange,
      PositionValidationRule atMostOneChange
  ) {}
}