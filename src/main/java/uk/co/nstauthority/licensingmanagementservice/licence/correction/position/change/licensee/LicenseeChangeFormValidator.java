package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;

@Component
public class LicenseeChangeFormValidator {
  private static final String JOINING_SELECTOR_FIELD = "joiningOrganisationSelector";
  private static final String WITHDRAWING_SELECTOR_FIELD = "withdrawingOrganisationSelector";
  private static final String VALID_LICENSEE_TEXT = "Select a valid licensee";
  private final OrganisationUnitQueryService organisationUnitQueryService;

  LicenseeChangeFormValidator(OrganisationUnitQueryService organisationUnitQueryService) {
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public boolean hasErrors(
      LicenseeChangeForm form,
      Errors errors,
      List<Integer> previousLicenseeIds
  ) {
    if (form.getJoiningOrganisationIds().isEmpty() && form.getWithdrawingOrganisationIds().isEmpty()) {
      errors.rejectValue(JOINING_SELECTOR_FIELD, "joiningOrganisationSelector.required",
          "Select licensees to join or withdraw");
      errors.rejectValue(WITHDRAWING_SELECTOR_FIELD, "withdrawingOrganisationSelector.required", "");
      return true;
    }

    List<Integer> joiningLicenseeIds;
    try {
      joiningLicenseeIds = form.getJoiningOrganisationIds()
          .stream()
          .map(Integer::parseInt)
          .toList();
    } catch (NumberFormatException e) {
      errors.rejectValue(
          JOINING_SELECTOR_FIELD,
          "joiningOrganisationSelector.invalid",
          VALID_LICENSEE_TEXT);
      return true;
    }

    List<Integer> withdrawingLicenseeIds;
    try {
      withdrawingLicenseeIds = form.getWithdrawingOrganisationIds()
          .stream()
          .map(Integer::parseInt)
          .toList();
    } catch (NumberFormatException e) {
      errors.rejectValue(
          WITHDRAWING_SELECTOR_FIELD,
          "withdrawingOrganisationSelector.invalid",
          VALID_LICENSEE_TEXT);
      return true;
    }

    List<Integer> combinedOrgIds = new ArrayList<>();
    combinedOrgIds.addAll(joiningLicenseeIds);
    combinedOrgIds.addAll(withdrawingLicenseeIds);

    var foundIds = organisationUnitQueryService.getOrganisationUnitsByIds(combinedOrgIds)
        .stream()
        .map(OrganisationUnitJson::getId)
        .map(Integer::parseInt)
        .collect(Collectors.toSet());
    var joiningIdIsMissing = joiningLicenseeIds.stream().anyMatch(id -> !foundIds.contains(id));
    var withdrawingIdIsMissing = withdrawingLicenseeIds.stream().anyMatch(id -> !foundIds.contains(id));

    if (joiningIdIsMissing) {
      errors.rejectValue(
          JOINING_SELECTOR_FIELD,
          "joiningOrganisationSelector.organisationNotFound",
          VALID_LICENSEE_TEXT);
    }
    if (withdrawingIdIsMissing) {
      errors.rejectValue(
          WITHDRAWING_SELECTOR_FIELD,
          "withdrawingOrganisationSelector.organisationNotFound",
          VALID_LICENSEE_TEXT);
    }

    if (Set.copyOf(joiningLicenseeIds).size() != joiningLicenseeIds.size()) {
      errors.rejectValue(JOINING_SELECTOR_FIELD, "joiningOrganisationSelector.duplicateIds",
          "Only add a licensee a single time");
    }

    if (Set.copyOf(withdrawingLicenseeIds).size() != withdrawingLicenseeIds.size()) {
      errors.rejectValue(WITHDRAWING_SELECTOR_FIELD, "withdrawingOrganisationSelector.duplicateIds",
          "Only remove a licensee a single time");
    }

    if (!Collections.disjoint(joiningLicenseeIds, previousLicenseeIds)) {
      errors.rejectValue(JOINING_SELECTOR_FIELD, "joiningOrganisationSelector.duplicateIds",
          "Select licensees that are not already on the licence");
    }

    if (!new HashSet<>(previousLicenseeIds).containsAll(withdrawingLicenseeIds)) {
      errors.rejectValue(WITHDRAWING_SELECTOR_FIELD, "withdrawingOrganisationSelector.notFound",
          "Select licensees that are on the licence");
    }

    if (!Collections.disjoint(joiningLicenseeIds, withdrawingLicenseeIds)) {
      errors.rejectValue(JOINING_SELECTOR_FIELD, "joiningOrganisationSelector.conflict",
          "Cannot add and remove a licensee at the same position");
      errors.rejectValue(WITHDRAWING_SELECTOR_FIELD, "withdrawingOrganisationSelector.conflict",
          "");
    }

    return errors.hasErrors();
  }
}
