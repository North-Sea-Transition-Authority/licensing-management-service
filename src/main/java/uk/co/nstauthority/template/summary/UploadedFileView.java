package uk.co.nstauthority.template.summary;

import java.time.Instant;
import uk.co.fivium.fileuploadlibrary.FileUploadLibraryUtils;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;

public record UploadedFileView(
    String fileId,
    String fileName,
    String fileSize,
    String fileDescription,
    Instant fileUploadedTime,
    String downloadUrl
) {
  public static UploadedFileView from(
      UploadedFile uploadedFile,
      String downloadUrl
  ) {
    return new UploadedFileView(
        uploadedFile.getId().toString(),
        uploadedFile.getName(),
        FileUploadLibraryUtils.formatSize(uploadedFile.getContentLength()),
        uploadedFile.getDescription(),
        uploadedFile.getUploadedAt(),
        downloadUrl
    );
  }
}
