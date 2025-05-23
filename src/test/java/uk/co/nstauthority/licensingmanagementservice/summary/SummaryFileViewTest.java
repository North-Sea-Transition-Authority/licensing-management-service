package uk.co.nstauthority.licensingmanagementservice.summary;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;

class SummaryFileViewTest {

  private static final String KEY_1 = "key1";
  private static final UUID FILE_ID_1 = UUID.randomUUID();
  private static final String FILE_NAME_1 = "file name1";
  private static final long FILE_SIZE_1 = 2048L;
  private static final String FILE_SIZE_STRING_1 = "2.0 KB";
  private static final String FILE_DESCRIPTION_1 = "file description";
  private static final Instant FILE_UPLOAD_INSTANT_1 = Instant.parse("2025-04-10T10:15:00Z");
  private static final String DOWNLOAD_URL_1 = "https://www.fivium.co.uk";
  private static final UploadedFile UPLOADED_FILE_1 = new UploadedFile();
  private static final UploadedFileView UPLOADED_FILE_VIEW_1 = new UploadedFileView(
      FILE_ID_1.toString(),
      FILE_NAME_1,
      FILE_SIZE_STRING_1,
      FILE_DESCRIPTION_1,
      FILE_UPLOAD_INSTANT_1,
      DOWNLOAD_URL_1
  );
  private static final UUID FILE_ID_2 = UUID.randomUUID();
  private static final String FILE_NAME_2 = "file name2";
  private static final long FILE_SIZE_2 = 4096L;
  private static final String FILE_SIZE_STRING_2 = "4.0 KB";
  private static final String FILE_DESCRIPTION_2 = "file description";
  private static final Instant FILE_UPLOAD_INSTANT_2 = Instant.parse("2025-04-10T10:16:00Z");
  private static final String DOWNLOAD_URL_2 = "https://www.fivium.com";
  private static final UploadedFile UPLOADED_FILE_2 = new UploadedFile();
  private static final UploadedFileView UPLOADED_FILE_VIEW_2 = new UploadedFileView(
      FILE_ID_2.toString(),
      FILE_NAME_2,
      FILE_SIZE_STRING_2,
      FILE_DESCRIPTION_2,
      FILE_UPLOAD_INSTANT_2,
      DOWNLOAD_URL_2
  );

  @BeforeAll
  static void setUpBeforeClass() {
    UPLOADED_FILE_1.setId(FILE_ID_1);
    UPLOADED_FILE_1.setName(FILE_NAME_1);
    UPLOADED_FILE_1.setContentLength(FILE_SIZE_1);
    UPLOADED_FILE_1.setDescription(FILE_DESCRIPTION_1);
    UPLOADED_FILE_1.setUploadedAt(FILE_UPLOAD_INSTANT_1);

    UPLOADED_FILE_2.setId(FILE_ID_2);
    UPLOADED_FILE_2.setName(FILE_NAME_2);
    UPLOADED_FILE_2.setContentLength(FILE_SIZE_2);
    UPLOADED_FILE_2.setDescription(FILE_DESCRIPTION_2);
    UPLOADED_FILE_2.setUploadedAt(FILE_UPLOAD_INSTANT_2);
  }

  @Test
  void newFromUploadedFile() {
    assertThat(SummaryFileView.newFromUploadedFile(KEY_1, UPLOADED_FILE_1, DOWNLOAD_URL_1))
        .isEqualTo(new SummaryFileView(
            KEY_1,
            List.of(UPLOADED_FILE_VIEW_1))
        );
  }

  @Test
  void newFromUploadedFile_then_addUploadedFile() {
    assertThat(
        SummaryFileView
            .newFromUploadedFile(KEY_1, UPLOADED_FILE_1, DOWNLOAD_URL_1)
            .addUploadedFile(UPLOADED_FILE_2, DOWNLOAD_URL_2)
    )
        .isEqualTo(new SummaryFileView(
            KEY_1,
            List.of(UPLOADED_FILE_VIEW_1, UPLOADED_FILE_VIEW_2))
        );
  }
}
