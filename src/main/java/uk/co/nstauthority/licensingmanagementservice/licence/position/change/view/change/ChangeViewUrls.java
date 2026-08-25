package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;

public record ChangeViewUrls(
    @Nullable String correct,
    @Nullable String remove,
    @Nullable String undo,
    @Nullable String correctChangeOrder
) {

  public static ChangeViewUrls none() {
    return new ChangeViewUrls(null, null, null, null);
  }

  public ChangeViewUrls merge(ChangeViewUrls other) {
    return new ChangeViewUrls(
        correct != null ? correct : other.correct(),
        remove != null ? remove : other.remove(),
        undo != null ? undo : other.undo(),
        correctChangeOrder != null ? correctChangeOrder : other.correctChangeOrder()
    );
  }
}