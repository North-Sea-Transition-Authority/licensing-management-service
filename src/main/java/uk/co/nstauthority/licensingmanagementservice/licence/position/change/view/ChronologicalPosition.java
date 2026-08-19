package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

public record ChronologicalPosition(
    UUID id,
    UUID transactionId,
    LocalDate date,
    int order,
    List<PositionChange> changes
) {

  private static final BigDecimal ONE_HUNDRED_PERCENT = new BigDecimal("100");

  public static ChronologicalPosition fromLicencePosition(
      LicencePosition position,
      LocalDate date,
      int order,
      List<PositionChange> changes
  ) {
    return new ChronologicalPosition(
        position.getId(),
        position.getLicenceTransaction().getId(),
        date,
        order,
        changes
    );
  }

  public static ChronologicalPosition fromPayload(CreateLicencePositionPayload payload) {
    return new ChronologicalPosition(
        UUID.fromString(payload.licencePositionId()),
        UUID.fromString(payload.licenceTransactionId()),
        payload.effectiveDate(),
        payload.effectiveDateOrder(),
        PositionChange.fromPayload(payload)
    );
  }

  public String positionName() {
    return DateUtil.formatLongDateWithOrder(date, order);
  }

  public List<PositionValidationError> validate(PositionValidationContext positionValidationContext) {
    var positionValidationErrors = new ArrayList<PositionValidationError>();

    positionValidationErrors.addAll(validateAdministratorChange(positionValidationContext));
    positionValidationErrors.addAll(validateBeneficialInterest(positionValidationContext));
    changes.forEach(change -> positionValidationErrors.addAll(change.validate(positionValidationContext)));

    return positionValidationErrors;
  }

  public long beneficialInterestChangeCount() {
    return changes.stream()
        .filter(ChronologicalPosition::isBeneficialInterestChange)
        .count();
  }

  private List<PositionValidationError> validateAdministratorChange(PositionValidationContext positionValidationContext) {
    if (positionValidationContext.isCarbonStorage()) {
      return List.of();
    }

    var administratorChangeCount = changes.stream()
        .filter(change -> !LicencePositionChangeType.REMOVE_CHANGE.equals(change.changeType()))
        .flatMap(change -> change.operations().stream())
        .filter(licenceOperation -> LicenceOperation.LICENCE_ADMINISTRATOR.equals(licenceOperation.type()))
        .count();

    if (positionValidationContext.isFirstPosition() && administratorChangeCount == 0) {
      return List.of(PositionValidationError.forPosition(
          positionValidationContext,
          AdministratorPositionRule.FIRST_POSITION_MUST_HAVE_ADMINISTRATOR
      ));
    }

    if (administratorChangeCount > 1) {
      return List.of(PositionValidationError.forPosition(
          positionValidationContext,
          AdministratorPositionRule.ONLY_ONE_ADMINISTRATOR_CHANGE
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

  private List<PositionValidationError> validateEquityTotals(PositionValidationContext positionValidationContext) {
    var equityByOrganisationId = positionValidationContext.resolvedState().equityByOrganisationId();
    var hasBeneficialInterestChange = beneficialInterestChangeCount() > 0;

    if (equityByOrganisationId.isEmpty() && !hasBeneficialInterestChange) {
      return List.of();
    }

    var previousEquityByOrganisationId = positionValidationContext.previousState().equityByOrganisationId();

    var errors = new ArrayList<PositionValidationError>();

    var outOfRangeHolderIds = outOfRangeHolderIds(equityByOrganisationId);
    if (!outOfRangeHolderIds.isEmpty()
        && !outOfRangeHolderIds.equals(outOfRangeHolderIds(previousEquityByOrganisationId))) {
      errors.add(PositionValidationError.forPosition(
          positionValidationContext,
          EquityPositionRule.EQUITY_HOLDER_OUT_OF_RANGE
      ));
    }

    var totalEquity = totalEquity(equityByOrganisationId);
    var previousTotalEquity = totalEquity(previousEquityByOrganisationId);
    var previousPositionWasInvalid = !previousEquityByOrganisationId.isEmpty()
        && previousTotalEquity.compareTo(ONE_HUNDRED_PERCENT) != 0;
    var carriesForwardSameInvalidTotal = previousPositionWasInvalid
        && totalEquity.compareTo(previousTotalEquity) == 0;
    if (totalEquity.compareTo(ONE_HUNDRED_PERCENT) != 0 && !carriesForwardSameInvalidTotal) {
      errors.add(PositionValidationError.forPosition(
          positionValidationContext,
          EquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED
      ));
    }

    return errors;
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
}