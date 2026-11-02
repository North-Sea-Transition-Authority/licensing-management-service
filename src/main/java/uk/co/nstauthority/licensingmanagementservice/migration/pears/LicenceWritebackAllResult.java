package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.util.List;
import java.util.Map;

/**
 * What a writeback of every licence PEARS holds did, as the actuator endpoint answers with. The
 * licences written back are only counted, each one is logged as it goes; the ones that were not
 * are named.
 *
 * @param licences          the licences PEARS holds
 * @param writtenBack       the licences positions were built for
 * @param nothingToDo       the licences PEARS holds no positions for, left as they were
 * @param notInLms          the licences PEARS holds that this application does not
 * @param failed            the licences whose writeback failed and was rolled back, with why
 * @param operationsIgnored PEARS operations no migrator supports, whose positions were still built
 */
record LicenceWritebackAllResult(
    int licences,
    int writtenBack,
    List<String> nothingToDo,
    List<String> notInLms,
    Map<String, String> failed,
    int positions,
    int changes,
    int operations,
    int operationsIgnored
) {
}
