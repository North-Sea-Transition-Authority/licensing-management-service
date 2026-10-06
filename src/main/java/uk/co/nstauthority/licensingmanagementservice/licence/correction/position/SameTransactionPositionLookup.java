package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import jakarta.annotation.Nullable;
import java.util.UUID;

public record SameTransactionPositionLookup(UUID transactionId, @Nullable UUID matchingPositionId) {
}