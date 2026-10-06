package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import jakarta.annotation.Nullable;
import java.util.UUID;

record MovedChange(
    String changeId,
    UUID fromPositionId,
    String movedFrom,
    String movedTo,
    @Nullable Integer liveChangeOrder
) {
}