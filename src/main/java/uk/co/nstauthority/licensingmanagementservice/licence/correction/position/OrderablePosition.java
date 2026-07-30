package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.time.LocalDate;
import java.util.UUID;

public record OrderablePosition(
    UUID id,
    LocalDate effectiveDate,
    int effectiveDateOrder,
    String reference,
    boolean added
) {
}