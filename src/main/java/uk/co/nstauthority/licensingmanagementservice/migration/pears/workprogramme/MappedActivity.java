package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.annotation.Nullable;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;

/**
 * A PEARS activity mapped to what its LMS work programme activity will hold. Its date and status are mapped
 * separately.
 *
 * @param otherCategoryName for {@code OTHER_ACTIVITY}, the name it is shown under; otherwise null
 * @param stub              whether it is a placeholder from the 2011 {@code LEGACY} load, which gets a comment saying so
 */
public record MappedActivity(
    String pearsActivityId,
    WorkProgrammeActivityCategory category,
    @Nullable String otherCategoryName,
    WorkProgrammeActivityCommitment commitment,
    String description,
    boolean stub,
    PearsActivity source
) {
}
