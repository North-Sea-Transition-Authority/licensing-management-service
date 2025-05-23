package uk.co.nstauthority.licensingmanagementservice.file;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteResponse;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadResponse;

@Controller
@RequestMapping("/test-file-controller")
public class TestFileController {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestFileController.class);

  @PostMapping
  public ResponseEntity<FileUploadResponse> upload(MultipartFile multipartFile) {
    LOGGER.info("Uploaded file {}", multipartFile.getName());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{fileId}")
  public ResponseEntity<InputStreamResource> download(@PathVariable UUID fileId) {
    LOGGER.info("Downloaded file {}", fileId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/delete/{fileId}")
  public ResponseEntity<FileDeleteResponse> delete(@PathVariable UUID fileId) {
    LOGGER.info("Deleted file {}", fileId);
    return ResponseEntity.ok().build();
  }
}
