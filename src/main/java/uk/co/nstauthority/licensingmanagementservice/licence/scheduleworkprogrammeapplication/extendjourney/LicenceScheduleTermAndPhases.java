package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import java.util.List;

public record LicenceScheduleTermAndPhases(
    String termId,
    String termName,
    List<PhaseDetails> phases
) {

  public record PhaseDetails(
      String phaseId,
      String phaseName
  ) {
  }
}