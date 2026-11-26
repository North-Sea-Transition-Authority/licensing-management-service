package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ConditionalOnPearsDataSource;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceComparisons;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * Compares chosen licences' current PEARS work programmes with their LMS work programmes, for checking the migration
 * before and after it runs (plan, design section 6). Reads PEARS and maps it as the migration does, reads LMS as stored,
 * and changes nothing.
 */
@Service
@Profile("migration")
@ConditionalOnPearsDataSource
public class PearsWorkProgrammeComparisonService {

  static final String NO_PEARS_WORK_PROGRAMME = "PEARS holds no work programme for the licence";

  private final PearsWorkProgrammeService pearsWorkProgrammeService;
  private final LicenceService licenceService;
  private final MigratedScheduleReader migratedScheduleReader;
  private final LmsWorkProgrammeReader lmsWorkProgrammeReader;
  private final PearsWorkProgrammeMigratedLicenceRepository migratedLicenceRepository;
  private final EntityManager entityManager;
  private final PearsLicenceComparisons licenceComparisons;

  PearsWorkProgrammeComparisonService(
      PearsWorkProgrammeService pearsWorkProgrammeService,
      LicenceService licenceService,
      MigratedScheduleReader migratedScheduleReader,
      LmsWorkProgrammeReader lmsWorkProgrammeReader,
      PearsWorkProgrammeMigratedLicenceRepository migratedLicenceRepository,
      EntityManager entityManager,
      PearsLicenceComparisons licenceComparisons
  ) {
    this.pearsWorkProgrammeService = pearsWorkProgrammeService;
    this.licenceService = licenceService;
    this.migratedScheduleReader = migratedScheduleReader;
    this.lmsWorkProgrammeReader = lmsWorkProgrammeReader;
    this.migratedLicenceRepository = migratedLicenceRepository;
    this.entityManager = entityManager;
    this.licenceComparisons = licenceComparisons;
  }

  /**
   * The licences the migration wrote a work programme for, in licence order.
   */
  public List<PearsLicenceReference> migratedLicences() {
    return migratedLicenceRepository.findAllLicenceReferences().stream()
        .map(PearsLicenceReference::parse)
        .toList();
  }

  /**
   * One comparison per licence, in the order given. Each licence's LMS rows are let go of once it is compared, so
   * comparing every migrated licence doesn't hold them all.
   */
  @Transactional(readOnly = true)
  public List<PearsWorkProgrammeComparison> compare(List<PearsLicenceReference> licences) {
    var comparisons = new ArrayList<PearsWorkProgrammeComparison>();
    for (var licence : licences) {
      comparisons.add(compare(licence));
      entityManager.clear();
    }
    return comparisons;
  }

  /**
   * As {@link #compare(List)}, but several licences at a time, each in a transaction of its own, so it only sees what is
   * committed: for the report, not for a caller comparing what its own transaction wrote.
   */
  public List<PearsWorkProgrammeComparison> compareInParallel(List<PearsLicenceReference> licences) {
    return licenceComparisons.compare(licences, this::compare);
  }

  PearsWorkProgrammeComparison compare(PearsLicenceReference reference) {
    var licenceReference = reference.licenceReference();
    var licence = licenceService.findLicenceByReference(licenceReference);
    if (licence.isEmpty()) {
      return PearsWorkProgrammeComparison.problem(licenceReference, PearsWorkProgrammeMigrationService.NOT_IN_LMS);
    }

    var workProgrammes = pearsWorkProgrammeService.licenceWorkProgrammes(reference);
    if (workProgrammes.isEmpty()) {
      return PearsWorkProgrammeComparison.problem(licenceReference, NO_PEARS_WORK_PROGRAMME);
    }

    var active = migratedScheduleReader.read(licence.get());
    if (active.isEmpty()) {
      return PearsWorkProgrammeComparer.compare(licenceReference, workProgrammes, null,
          PearsWorkProgrammeMigrationService.NO_MIGRATED_SCHEDULE, LmsWorkProgramme.none());
    }

    MappedWorkProgramme mapped = null;
    String migrationFailure = null;
    try {
      mapped = PearsWorkProgrammeMapper.map(reference, workProgrammes, active.get().schedule());
    } catch (PearsWorkProgrammeMappingException e) {
      migrationFailure = e.reason();
    }
    return PearsWorkProgrammeComparer.compare(licenceReference, workProgrammes, mapped, migrationFailure,
        lmsWorkProgrammeReader.read(active.get()));
  }
}
