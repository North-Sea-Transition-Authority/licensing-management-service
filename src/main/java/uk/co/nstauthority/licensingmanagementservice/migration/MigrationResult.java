package uk.co.nstauthority.licensingmanagementservice.migration;

/**
 * What a migration did: how many items it migrated, and how many it skipped because they had already been migrated or
 * could not be migrated. Reported back from the migration endpoints so that re-running a migration is visibly a no-op
 * rather than being indistinguishable from a first run.
 */
public record MigrationResult(int migrated, int skipped) {

  public static MigrationResult nothingToMigrate() {
    return new MigrationResult(0, 0);
  }

  public String describe(String itemName) {
    return "%d %s migrated, %d skipped".formatted(migrated, itemName, skipped);
  }
}
