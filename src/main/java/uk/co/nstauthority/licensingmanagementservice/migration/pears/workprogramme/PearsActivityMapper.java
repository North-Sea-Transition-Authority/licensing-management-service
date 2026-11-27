package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.MappedActivities.SkippedActivity;

/**
 * Maps a work programme's activities to LMS work programme activities, following the plan's M1 (category and
 * commitment) and M2 (description). Rules waiting on the licensing team are built with the plan's provisional answers
 * ("Building with provisional answers"), and every activity one touches is noted so the report can mark it.
 */
final class PearsActivityMapper {

  static final String NONE_NOT_MIGRATED = "Activity recording \"None\": not migrated";
  static final String STUB_MIGRATED =
      "Stub activity from the 2011 LEGACY load: migrated with a comment (WP-L14)";
  static final String EMPTY_STUB_NOT_MIGRATED =
      "Stub activity from the 2011 LEGACY load, NOT_STARTED with no note by a person: not migrated (WP-L14)";
  static final String NO_COMMITMENT = "No commitment in PEARS: CONDITIONAL (provisional, WP-L9)";
  static final String COMMITMENT_FROM_TITLE = "Type holds no commitment, title says contingent: CONTINGENT";
  static final String TITLE_CONTRADICTS_COMMITMENT =
      "Title says not contingent, type says contingent: type kept, title put before the description";
  static final String SEISMIC_FROM_DESCRIPTION = "Generic seismic title: category from the description";
  static final String SEISMIC_UNCLASSIFIED = "Seismic activity the description doesn't classify: Other activity";
  static final String SEISMIC_TWO_OPERATIONS =
      "Seismic activity describing more than one operation: one activity (provisional, WP-L13)";
  static final String TITLE_LINK_REMOVED = "Title with a pasted javascript: link: link removed";
  static final String TEMPLATE_PLACEHOLDER = "Description with a template placeholder (***): migrated as it is";
  static final String DESCRIPTION_FROM_TITLE = "No description: the title used instead";
  static final String DESCRIPTION_FROM_CATEGORY = "No description or title: the category name used instead";

  static final String STUB_COMMENT =
      "PEARS stub from the 2011 legacy load: PEARS held no electronic copy of this work programme";
  static final String LEGACY_NAME = "Legacy work programme";
  static final String OTHER_NAME = "Other work programme activity";
  static final String SEISMIC_NAME = "Seismic";
  static final String SEISMIC_INVERSION_NAME = "Seismic inversion";

  private static final String SEPARATOR = " - ";

  private static final String LEGACY_TYPE = "LEGACY";
  private static final String NOT_STARTED = "NOT_STARTED";
  private static final String OTHER_TYPE = "OTHER";

  /**
   * Every activity type the mapper has a rule for. Any other fails the licence, checked before an activity is skipped
   * so an unknown type can't pass unnoticed.
   */
  private static final Set<String> KNOWN_TYPES = Set.of(
      "FIRM_WELL", "CONTINGENT_WELL", "DRILL_OR_DROP_WELL",
      "SHOOT_2D_SEISMIC", "SHOOT_2D_SEISMIC_FIRM", "SHOOT_2D_SEISMIC_CONTINGENT",
      "SHOOT_3D_SEISMIC", "SHOOT_3D_SEISMIC_FIRM", "SHOOT_3D_SEISMIC_CONTINGENT",
      "OBTAIN_2D_SEISMIC", "OBTAIN_3D_SEISMIC", "REPROCESS_2D_SEISMIC", "REPROCESS_3D_SEISMIC",
      "SEISMIC", LEGACY_TYPE, OTHER_TYPE
  );

  private static final Pattern NONE = Pattern.compile("(?i)^none\\b");
  private static final Pattern STUB = Pattern.compile("(?i)^stub activity\\b");
  private static final Pattern TITLE_LINK = Pattern.compile("(?i)javascript:.*$", Pattern.DOTALL);

  /**
   * A standard {@code PED009X} seismic title, such as "Obtain 3D Seismic Data 1": the verb and the dimension.
   */
  private static final Pattern STANDARD_SEISMIC_TITLE = Pattern.compile("(?i)^(shoot|obtain|reprocess)\\s+([23])d\\b");
  private static final Pattern REPROCESS = Pattern.compile("(?i)reprocess");
  private static final Pattern SHOOT = Pattern.compile("(?i)shoot|acquir");
  private static final Pattern OBTAIN = Pattern.compile("(?i)obtain|purchas|licen[cs]e");
  private static final Pattern INVERSION = Pattern.compile("(?i)inversion");
  private static final Pattern TWO_D = Pattern.compile("(?i)\\b2\\s*d\\b");
  private static final Pattern THREE_D = Pattern.compile("(?i)\\b3\\s*d\\b");

  /**
   * The titles that add something to the description: a {@code LEGACY} activity's original blocks, and an
   * {@code OTHER} activity's own title. Every other title is the type's default, "Other Activity 1", or a
   * programme-level label such as "Initial Work Programme" (WP-S4).
   */
  private static final Pattern LEGACY_OBLIGATIONS_TITLE = Pattern.compile("(?i)^legacy obligations\\b");
  private static final Pattern DEFAULT_OTHER_TITLE = Pattern.compile("(?i)^other activity\\s*\\d*$");

  /**
   * A hand-typed title stating a commitment (WP-S16): "Contingent Obtain 3D Seismic Data 1", "Other Activity 3 -
   * CONTINGENT COMMITMENT", and one "Shoot 3D Not-Contingent Seismic Data 1" on a contingent type.
   */
  private static final Pattern NOT_CONTINGENT_TITLE = Pattern.compile("(?i)\\bnot[\\s-]*contingent\\b");
  private static final Pattern CONTINGENT_TITLE = Pattern.compile("(?i)\\bcontingent\\b");

  private PearsActivityMapper() {
  }

  /**
   * The programme's activities, in the order PEARS holds them.
   *
   * @throws PearsWorkProgrammeMappingException if an activity has a type the migration doesn't know, or no ID
   */
  static MappedActivities activities(PearsWorkProgramme workProgramme, MigrationNotes notes) {
    var activities = new ArrayList<MappedActivity>();
    var skipped = new ArrayList<SkippedActivity>();

    for (var activity : workProgramme.activities()) {
      if (StringUtils.isBlank(activity.activityId())) {
        throw new PearsWorkProgrammeMappingException(workProgramme.licence(),
            "Activity %d of work programme %d has no ACTIVITY_ID".formatted(activity.position(), workProgramme.pedWpId()));
      }
      if (!KNOWN_TYPES.contains(activityType(activity))) {
        throw unknownType(workProgramme, activity);
      }

      var description = StringUtils.strip(activity.description());
      var skipReason = skipReason(workProgramme, activity, description);
      if (skipReason != null) {
        notes.note(skipReason);
        skipped.add(new SkippedActivity(activity, skipReason));
      } else {
        activities.add(map(workProgramme, activity, description, notes));
      }
    }

    return new MappedActivities(activities, skipped);
  }

  /**
   * Why the activity isn't migrated, or null when it is.
   */
  @Nullable
  private static String skipReason(
      PearsWorkProgramme workProgramme,
      PearsActivity activity,
      @Nullable String description
  ) {
    if (description != null && NONE.matcher(description).find()) {
      return NONE_NOT_MIGRATED;
    }
    if (isEmptyStub(workProgramme, activity, description)) {
      return EMPTY_STUB_NOT_MIGRATED;
    }
    return null;
  }

  /**
   * A stub that says nothing about the work: still not started, and with no note written by a person. The 2011 bulk
   * job's own notes don't count.
   */
  private static boolean isEmptyStub(
      PearsWorkProgramme workProgramme,
      PearsActivity activity,
      @Nullable String description
  ) {
    var stub = description != null && STUB.matcher(description).find();
    if (!stub || !NOT_STARTED.equals(StringUtils.strip(activity.status()))) {
      return false;
    }
    var activityId = activity.activityId().strip();
    return workProgramme.notes().stream()
        .filter(note -> activityId.equals(StringUtils.strip(note.activityId())))
        .allMatch(note -> PearsNoteMapper.SYSTEM_AUTHOR.equals(StringUtils.strip(note.createdBy())));
  }

  private static MappedActivity map(
      PearsWorkProgramme workProgramme,
      PearsActivity activity,
      @Nullable String description,
      MigrationNotes notes
  ) {
    var title = title(activity, notes);
    var classification = classify(workProgramme, activity, title, description, notes);

    var commitment = commitment(classification.commitment(), title, notes);

    var stub = description != null && STUB.matcher(description).find();
    if (stub) {
      notes.note(STUB_MIGRATED);
    }

    return new MappedActivity(
        activity.activityId().strip(),
        classification.category(),
        classification.otherCategoryName(),
        commitment,
        description(activity, title, description, classification, notes),
        stub,
        activity
    );
  }

  /**
   * The type's commitment. A type holding none takes contingent from a title that says so, and otherwise the default.
   */
  private static WorkProgrammeActivityCommitment commitment(
      @Nullable WorkProgrammeActivityCommitment typeCommitment,
      @Nullable String title,
      MigrationNotes notes
  ) {
    var notContingent = title != null && NOT_CONTINGENT_TITLE.matcher(title).find();
    if (typeCommitment == WorkProgrammeActivityCommitment.CONTINGENT && notContingent) {
      notes.note(TITLE_CONTRADICTS_COMMITMENT);
    }
    if (typeCommitment != null) {
      return typeCommitment;
    }
    if (title != null && !notContingent && CONTINGENT_TITLE.matcher(title).find()) {
      notes.note(COMMITMENT_FROM_TITLE);
      return WorkProgrammeActivityCommitment.CONTINGENT;
    }
    notes.note(NO_COMMITMENT);
    return WorkProgrammeActivityCommitment.CONDITIONAL;
  }

  private static Classification classify(
      PearsWorkProgramme workProgramme,
      PearsActivity activity,
      @Nullable String title,
      @Nullable String description,
      MigrationNotes notes
  ) {
    return switch (activityType(activity)) {
      case "FIRM_WELL" -> Classification.of(WorkProgrammeActivityCategory.DRILL_WELL, WorkProgrammeActivityCommitment.FIRM);
      case "CONTINGENT_WELL" ->
          Classification.of(WorkProgrammeActivityCategory.DRILL_WELL, WorkProgrammeActivityCommitment.CONTINGENT);
      case "DRILL_OR_DROP_WELL" -> Classification.of(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL, null);
      case "SHOOT_2D_SEISMIC" -> Classification.of(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA, null);
      case "SHOOT_2D_SEISMIC_FIRM" -> Classification.of(
          WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA, WorkProgrammeActivityCommitment.FIRM);
      case "SHOOT_2D_SEISMIC_CONTINGENT" -> Classification.of(
          WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA, WorkProgrammeActivityCommitment.CONTINGENT);
      case "SHOOT_3D_SEISMIC" -> Classification.of(WorkProgrammeActivityCategory.NEW_SHOOT_3_D_SEISMIC_DATA, null);
      case "SHOOT_3D_SEISMIC_FIRM" -> Classification.of(
          WorkProgrammeActivityCategory.NEW_SHOOT_3_D_SEISMIC_DATA, WorkProgrammeActivityCommitment.FIRM);
      case "SHOOT_3D_SEISMIC_CONTINGENT" -> Classification.of(
          WorkProgrammeActivityCategory.NEW_SHOOT_3_D_SEISMIC_DATA, WorkProgrammeActivityCommitment.CONTINGENT);
      case "OBTAIN_2D_SEISMIC" -> Classification.of(WorkProgrammeActivityCategory.OBTAIN_EXISTING_2_D_SEISMIC_DATA, null);
      case "OBTAIN_3D_SEISMIC" -> Classification.of(WorkProgrammeActivityCategory.OBTAIN_EXISTING_3_D_SEISMIC_DATA, null);
      case "REPROCESS_2D_SEISMIC" -> Classification.reprocess("2D");
      case "REPROCESS_3D_SEISMIC" -> Classification.reprocess("3D");
      case "SEISMIC" -> classifySeismic(title, description, notes);
      case LEGACY_TYPE -> Classification.other(LEGACY_NAME);
      case OTHER_TYPE -> Classification.other(OTHER_NAME);
      default -> throw unknownType(workProgramme, activity);
    };
  }

  private static String activityType(PearsActivity activity) {
    return StringUtils.defaultString(activity.activityType()).strip();
  }

  private static PearsWorkProgrammeMappingException unknownType(PearsWorkProgramme workProgramme, PearsActivity activity) {
    return new PearsWorkProgrammeMappingException(workProgramme.licence(),
        "Activity %s has the activity type '%s', which the migration doesn't know"
            .formatted(activity.activityId(), activityType(activity)));
  }

  /**
   * A flat-list {@code SEISMIC} activity: from a standard title, or failing that the description, with reprocess
   * checked first (WP-S4b).
   */
  private static Classification classifySeismic(
      @Nullable String title,
      @Nullable String description,
      MigrationNotes notes
  ) {
    var text = StringUtils.defaultString(description);
    if (operations(text) > 1) {
      notes.note(SEISMIC_TWO_OPERATIONS);
    }

    var fromTitle = classifySeismicTitle(title);
    if (fromTitle != null) {
      return fromTitle;
    }

    notes.note(SEISMIC_FROM_DESCRIPTION);
    return classifySeismicDescription(text, notes);
  }

  /**
   * The category a standard {@code PED009X} seismic title gives, such as "Obtain 3D Seismic Data 1"; null for any
   * other title.
   */
  @Nullable
  private static Classification classifySeismicTitle(@Nullable String title) {
    var standardTitle = title == null ? null : STANDARD_SEISMIC_TITLE.matcher(title);
    if (standardTitle == null || !standardTitle.find()) {
      return null;
    }

    var twoD = standardTitle.group(2).equals("2");
    return switch (standardTitle.group(1).toLowerCase()) {
      case "shoot" -> Classification.of(twoD
          ? WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA
          : WorkProgrammeActivityCategory.NEW_SHOOT_3_D_SEISMIC_DATA, null);
      case "obtain" -> Classification.of(twoD
          ? WorkProgrammeActivityCategory.OBTAIN_EXISTING_2_D_SEISMIC_DATA
          : WorkProgrammeActivityCategory.OBTAIN_EXISTING_3_D_SEISMIC_DATA, null);
      default -> Classification.reprocess(twoD ? "2D" : "3D");
    };
  }

  private static Classification classifySeismicDescription(String text, MigrationNotes notes) {
    if (REPROCESS.matcher(text).find()) {
      return Classification.of(WorkProgrammeActivityCategory.REPROCESS_SEISMIC_DATA, null);
    }
    var twoD = TWO_D.matcher(text).find();
    var threeD = THREE_D.matcher(text).find();
    if (SHOOT.matcher(text).find() && (twoD || threeD)) {
      return Classification.of(twoD
          ? WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA
          : WorkProgrammeActivityCategory.NEW_SHOOT_3_D_SEISMIC_DATA, null);
    }
    if (OBTAIN.matcher(text).find() && (twoD || threeD)) {
      return Classification.of(twoD
          ? WorkProgrammeActivityCategory.OBTAIN_EXISTING_2_D_SEISMIC_DATA
          : WorkProgrammeActivityCategory.OBTAIN_EXISTING_3_D_SEISMIC_DATA, null);
    }

    notes.note(SEISMIC_UNCLASSIFIED);
    return Classification.other(INVERSION.matcher(text).find() ? SEISMIC_INVERSION_NAME : SEISMIC_NAME);
  }

  private static long operations(String text) {
    return Stream.of(SHOOT, Pattern.compile("(?i)obtain|purchas"), REPROCESS)
        .filter(operation -> operation.matcher(text).find())
        .count();
  }

  /**
   * The title, without the {@code javascript:} link pasted into one of them; null when there is none.
   */
  @Nullable
  private static String title(PearsActivity activity, MigrationNotes notes) {
    var title = activity.title();
    if (title != null && TITLE_LINK.matcher(title).find()) {
      notes.note(TITLE_LINK_REMOVED);
      title = TITLE_LINK.matcher(title).replaceFirst("");
    }
    return StringUtils.stripToNull(title);
  }

  /**
   * The PEARS description, with the title before it where the title adds something, the 2D or 3D of a reprocess
   * where the description doesn't say which, and the competed blocks after it. LMS needs a description, so one with
   * none takes the title, or failing that the category's name.
   */
  private static String description(
      PearsActivity activity,
      @Nullable String title,
      @Nullable String description,
      Classification classification,
      MigrationNotes notes
  ) {
    var parts = new ArrayList<>(StringUtils.isBlank(description)
        ? List.of(fallbackDescription(title, classification, notes))
        : describedParts(activity, title, description, classification, notes));

    var competedBlocks = StringUtils.stripToNull(activity.competedBlocks());
    if (competedBlocks != null) {
      parts.add("Competed blocks: " + competedBlocks);
    }

    return String.join(SEPARATOR, parts);
  }

  private static String fallbackDescription(
      @Nullable String title,
      Classification classification,
      MigrationNotes notes
  ) {
    if (title != null) {
      notes.note(DESCRIPTION_FROM_TITLE);
      return title;
    }
    notes.note(DESCRIPTION_FROM_CATEGORY);
    return classification.otherCategoryName() != null
        ? classification.otherCategoryName()
        : classification.category().getDisplayName();
  }

  private static List<String> describedParts(
      PearsActivity activity,
      @Nullable String title,
      String description,
      Classification classification,
      MigrationNotes notes
  ) {
    var parts = new ArrayList<String>();
    if (title != null && titleAddsInformation(activity, title)) {
      parts.add(title);
    }
    var dimension = classification.reprocessDimension();
    if (dimension != null && !TWO_D.matcher(description).find() && !THREE_D.matcher(description).find()) {
      parts.add(dimension);
    }
    if (description.contains("***")) {
      notes.note(TEMPLATE_PLACEHOLDER);
    }
    parts.add(description);
    return parts;
  }

  private static boolean titleAddsInformation(PearsActivity activity, String title) {
    return switch (StringUtils.defaultString(activity.activityType()).strip()) {
      case LEGACY_TYPE -> LEGACY_OBLIGATIONS_TITLE.matcher(title).find();
      case OTHER_TYPE -> !DEFAULT_OTHER_TITLE.matcher(title).matches();
      default -> NOT_CONTINGENT_TITLE.matcher(title).find();
    };
  }

  /**
   * An activity's LMS category, its name for {@code OTHER_ACTIVITY}, and its commitment, or null where PEARS holds
   * none. {@code reprocessDimension} is the 2D or 3D of a reprocess whose type or title says which, which
   * {@code REPROCESS_SEISMIC_DATA} doesn't hold.
   */
  private record Classification(
      WorkProgrammeActivityCategory category,
      @Nullable String otherCategoryName,
      @Nullable WorkProgrammeActivityCommitment commitment,
      @Nullable String reprocessDimension
  ) {

    static Classification of(WorkProgrammeActivityCategory category, @Nullable WorkProgrammeActivityCommitment commitment) {
      return new Classification(category, null, commitment, null);
    }

    static Classification reprocess(String dimension) {
      return new Classification(WorkProgrammeActivityCategory.REPROCESS_SEISMIC_DATA, null, null, dimension);
    }

    static Classification other(String name) {
      return new Classification(WorkProgrammeActivityCategory.OTHER_ACTIVITY, name, null, null);
    }
  }
}
