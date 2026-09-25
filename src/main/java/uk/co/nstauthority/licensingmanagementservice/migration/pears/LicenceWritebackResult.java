package uk.co.nstauthority.licensingmanagementservice.migration.pears;

/**
 * What a writeback did, as the actuator endpoint answers with: counts and a sentence only.
 * {@link LicenceWriteback} is what the migration itself works with.
 *
 * @param operationsIgnored PEARS operations no migrator supports, whose positions were still built
 */
record LicenceWritebackResult(
    String result,
    int positions,
    int changes,
    int operations,
    int operationsIgnored
) {

  static LicenceWritebackResult nothingToDo(String result) {
    return new LicenceWritebackResult(result, 0, 0, 0, 0);
  }
}
