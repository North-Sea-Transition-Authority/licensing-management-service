package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp;

import java.util.List;

public record CurrentDspView(
    String decisionDate,
    List<DspFileView> files,
    List<DspAddendumView> addendums,
    String uploadedBy
) {

  public record DspFileView(String name, String downloadUrl, String description) {
  }

  public record DspAddendumView(
      String name, String downloadUrl, String description, String uploadedBy, String uploadedAt) {
  }
}
