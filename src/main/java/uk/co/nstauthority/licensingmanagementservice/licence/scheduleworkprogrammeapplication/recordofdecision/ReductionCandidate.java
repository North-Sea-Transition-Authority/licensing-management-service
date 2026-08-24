package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.time.LocalDate;

record ReductionCandidate(String id, String displayName, LocalDate endDate, boolean isPhase) {
}
