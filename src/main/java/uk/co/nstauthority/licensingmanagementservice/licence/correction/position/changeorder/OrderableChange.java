package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder;

import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.Orderable;

/**
 * A change on a licence position that can be re-ordered relative to the other changes on that position.
 *
 * @param reference the change label including its order, e.g. "Set equity" - used in the ordering list and
 *     move options where the order number matters.
 */
public record OrderableChange(UUID id, String reference) implements Orderable {
}