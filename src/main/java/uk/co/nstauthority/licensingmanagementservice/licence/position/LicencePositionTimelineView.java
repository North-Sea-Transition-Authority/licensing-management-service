package uk.co.nstauthority.licensingmanagementservice.licence.position;

import jakarta.annotation.Nullable;
import java.util.UUID;

/**
 * One row in a licence position timeline (read-only or correction view).
 *
 * @param positionId identifier of the position this row represents - the live position's id, or the
 *                   id from the correction payload for added rows; matched against the page's
 *                   to highlight the current row
 * @param url link this row's heading navigates to - the position detail page (read-only or within
 *            the correction) for live rows, or the added-position page for added rows
 * @param regulatorReference reference shown as the row's hint text - the licence transaction's
 *                           regulator reference for live positions, or the correction reference for
 *                           added positions
 * @param formattedPositionDate the row's date pre-formatted for display (e.g. "5 June 2026") — the
 *                              position date for live rows, the effective date for added rows
 * @param addedInThisCorrection if this row is a position added in the current correction
 *                              (a draft); {@code false} for existing live (executed) positions.
 *                              Drives the "Added position" tag and Undo link in the template
 * @param undoUrl null for live (executed) positions; populated only for positions added in the
 *                current correction
 * @param removedInThisCorrection {@code true} when this row is a live position marked
 *                                for removal in the current correction (a draft); {@code false}
 *                                for live positions that remain and for added rows. Drives the
 *                                "Removed" tag in the template
 * @param removeUrl link to remove live position as part of the correction; populated
 *                  only for live positions not yet removed.
 * @param reinstateUrl link to reinstate a live position previously marked for removal in the
 *                     current correction; populated only for live positions currently removed.
 *                     Drives the "Reinstate" link in the template
 * @param correctedInThisCorrection true when this row is a live position whose date has been
 *                                  corrected in the current correction.
 * @param correctDateUrl link to correct this live position's date as part of the correction;
 */
public record LicencePositionTimelineView(
    UUID positionId,
    String url,
    String regulatorReference,
    String formattedPositionDate,
    boolean addedInThisCorrection,
    @Nullable String undoUrl,
    boolean removedInThisCorrection,
    @Nullable String removeUrl,
    @Nullable String reinstateUrl,
    boolean correctedInThisCorrection,
    @Nullable String correctDateUrl,
    @Nullable String correctOrderUrl,
    boolean hasError
) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID positionId;
    private String url;
    private String regulatorReference;
    private String formattedPositionDate;
    private boolean addedInThisCorrection;
    @Nullable private String undoUrl;
    private boolean removedInThisCorrection;
    @Nullable private String removeUrl;
    @Nullable private String reinstateUrl;
    private boolean correctedInThisCorrection;
    @Nullable private String correctDateUrl;
    @Nullable private String correctOrderUrl;
    private boolean hasError;

    public Builder withPositionId(UUID positionId) {
      this.positionId = positionId;
      return this;
    }

    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder withRegulatorReference(String regulatorReference) {
      this.regulatorReference = regulatorReference;
      return this;
    }

    public Builder withFormattedPositionDate(String formattedPositionDate) {
      this.formattedPositionDate = formattedPositionDate;
      return this;
    }

    public Builder withAddedInThisCorrection(boolean addedInThisCorrection) {
      this.addedInThisCorrection = addedInThisCorrection;
      return this;
    }

    public Builder withUndoUrl(String undoUrl) {
      this.undoUrl = undoUrl;
      return this;
    }

    public Builder withRemovedInThisCorrection(boolean removedInThisCorrection) {
      this.removedInThisCorrection = removedInThisCorrection;
      return this;
    }

    public Builder withRemoveUrl(String removeUrl) {
      this.removeUrl = removeUrl;
      return this;
    }

    public Builder withReinstateUrl(String reinstateUrl) {
      this.reinstateUrl = reinstateUrl;
      return this;
    }

    public Builder withCorrectedInThisCorrection(boolean correctedInThisCorrection) {
      this.correctedInThisCorrection = correctedInThisCorrection;
      return this;
    }

    public Builder withCorrectDateUrl(String correctDateUrl) {
      this.correctDateUrl = correctDateUrl;
      return this;
    }

    public Builder withCorrectOrderUrl(String correctOrderUrl) {
      this.correctOrderUrl = correctOrderUrl;
      return this;
    }

    public Builder withHasError(boolean hasError) {
      this.hasError = hasError;
      return this;
    }

    public LicencePositionTimelineView build() {
      return new LicencePositionTimelineView(
          positionId,
          url,
          regulatorReference,
          formattedPositionDate,
          addedInThisCorrection,
          undoUrl,
          removedInThisCorrection,
          removeUrl,
          reinstateUrl,
          correctedInThisCorrection,
          correctDateUrl,
          correctOrderUrl,
          hasError
      );
    }
  }
}