package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import jakarta.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;

/**
 * The filters applied to a licence timeline. An empty set of change types or organisation ids applies no filter of
 * that kind.
 *
 * @param changeTypes the {@link LicenceOperation#type()}s a position must have a change of
 * @param organisationIds the organisation unit ids a position must have a change involving
 */
public record LicenceTimelineFilter(Set<String> changeTypes, Set<Integer> organisationIds) implements Serializable {

  @Serial
  private static final long serialVersionUID = 4861183262043913270L;

  public static LicenceTimelineFilter empty() {
    return new LicenceTimelineFilter(Set.of(), Set.of());
  }

  public boolean isEmpty() {
    return changeTypes.isEmpty() && organisationIds.isEmpty();
  }

  /**
   * Whether the position has a single operation that is both of a selected change type and involves a selected
   * organisation, so a change type and an organisation filter together pick out that organisation's changes of that
   * type.
   *
   * @param outgoingAdministratorId the administrator the position's administrator change replaces, which the change
   *                                involves as much as the incoming one
   */
  public boolean matches(ChronologicalPosition chronologicalPosition, @Nullable Integer outgoingAdministratorId) {
    if (isEmpty()) {
      return true;
    }
    return chronologicalPosition.changes().stream()
        .flatMap(change -> change.operations().stream())
        .anyMatch(operation -> matchesChangeType(operation) && matchesOrganisation(operation, outgoingAdministratorId));
  }

  private boolean matchesChangeType(LicenceOperation operation) {
    return changeTypes.isEmpty() || changeTypes.contains(operation.type());
  }

  private boolean matchesOrganisation(LicenceOperation operation, @Nullable Integer outgoingAdministratorId) {
    if (organisationIds.isEmpty()) {
      return true;
    }
    return involvedOrganisationIds(operation, outgoingAdministratorId).stream()
        .filter(Objects::nonNull)
        .anyMatch(organisationIds::contains);
  }

  private static Collection<Integer> involvedOrganisationIds(
      LicenceOperation operation,
      @Nullable Integer outgoingAdministratorId
  ) {
    if (operation instanceof AdministratorOperation && outgoingAdministratorId != null) {
      return Stream.concat(operation.organisationUnitIds().stream(), Stream.of(outgoingAdministratorId))
          .toList();
    }
    return operation.organisationUnitIds();
  }
}
