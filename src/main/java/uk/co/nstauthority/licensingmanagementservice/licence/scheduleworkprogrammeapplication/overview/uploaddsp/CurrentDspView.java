package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp;

import java.util.List;

public record CurrentDspView(
    String decisionDate,
    List<DspFileView> files,
    String uploadedBy
) {

  public record DspFileView(String name, String downloadUrl) {
  }
}
