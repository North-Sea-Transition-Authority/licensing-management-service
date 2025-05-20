package uk.co.nstauthority.template.summary;

import java.util.ArrayList;
import java.util.List;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;


public record SummaryFileView(
    String fileKey,
    List<UploadedFileView> uploadedFileViews
) {

  public static SummaryFileView newFromUploadedFile(
      String fileKey,
      UploadedFile uploadedFile,
      String downloadUrl
  ) {
    var summaryFileView = new SummaryFileView(fileKey, new ArrayList<>());
    return summaryFileView.addUploadedFile(uploadedFile, downloadUrl);
  }

  public SummaryFileView addUploadedFile(
      UploadedFile uploadedFile,
      String downloadUrl
  ) {
    uploadedFileViews.add(UploadedFileView.from(uploadedFile, downloadUrl));
    return this;
  }
}
