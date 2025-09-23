package uk.co.nstauthority.licensingmanagementservice.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.file.XyzApplicationFileTestUtil.createdUploadedFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.co.fivium.fileuploadlibrary.FileUploadLibraryUtils;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;

@ExtendWith(MockitoExtension.class)
class ApplicationFileServiceTest {

  private static final String USAGE_ID = UUID.randomUUID().toString();
  private static final String USAGE_TYPE = "TestUsage";
  private static final String DOCUMENT_TYPE = "supporting-document";
  private static final ApplicationFileUsage DEFAULT_USAGE = new TestApplicationFileUsage(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE);

  @Mock
  private FileService fileService;

  @InjectMocks
  private ApplicationFileService applicationFileService;

  @Captor
  private ArgumentCaptor<Function<uk.co.fivium.fileuploadlibrary.core.FileUsage.Builder, uk.co.fivium.fileuploadlibrary.core.FileUsage>> fileUsageFunctionCaptor;

  private ApplicationFileUsage applicationFileUsage;
  private List<UploadedFile> uploadedFiles;
  private List<UUID> uploadedFileIds;
  private List<UploadedFileForm> uploadedFileForms;

  @BeforeEach
  void setUp() {
    applicationFileUsage = new TestApplicationFileUsage(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE);

    uploadedFiles = new ArrayList<>();
    uploadedFiles.add(createdUploadedFile(applicationFileUsage));

    uploadedFileIds = uploadedFiles.stream().map(UploadedFile::getId).toList();
    uploadedFileForms = uploadedFiles.stream().map(FileUploadLibraryUtils::asForm).toList();
  }

  @Test
  void saveDocuments() {
    doAnswer(invocation -> {
      var builderFunction = invocation.getArgument(1, Function.class);
      builderFunction.apply(uk.co.fivium.fileuploadlibrary.core.FileUsage.newBuilder());
      return null;
    })
        .when(fileService)
        .updateUsageAndDescription(any(UploadedFile.class), any(), anyString());

    when(fileService.findAll(uploadedFileIds)).thenReturn(uploadedFiles);

    applicationFileService.saveDocuments(applicationFileUsage, uploadedFileForms);

    for (var uploadedFile : uploadedFiles) {
      verify(fileService).updateUsageAndDescription(
          eq(uploadedFile),
          fileUsageFunctionCaptor.capture(),
          eq(XyzApplicationFileTestUtil.FILE_DESCRIPTION)
      );
      assertThat(fileUsageFunctionCaptor.getValue().apply(uk.co.fivium.fileuploadlibrary.core.FileUsage.newBuilder()))
          .extracting(
              uk.co.fivium.fileuploadlibrary.core.FileUsage::usageId,
              uk.co.fivium.fileuploadlibrary.core.FileUsage::usageType,
              uk.co.fivium.fileuploadlibrary.core.FileUsage::documentType
          ).containsExactly(
              USAGE_ID,
              USAGE_TYPE,
              DOCUMENT_TYPE
          );
    }
  }

  @Test
  void deleteFiles() {
    when(fileService.findAll(applicationFileUsage.usageId(), applicationFileUsage.usageType(), applicationFileUsage.documentType()))
        .thenReturn(uploadedFiles);
    applicationFileService.deleteFiles(applicationFileUsage);
    for (var uploadedFile: uploadedFiles) {
      verify(fileService).delete(uploadedFile);
    }
  }

  @Test
  void getUploadedFiles() {
    when(fileService.findAll(applicationFileUsage.usageId(), applicationFileUsage.usageType(), applicationFileUsage.documentType()))
        .thenReturn(uploadedFiles);
    var resultUploadedFiles = applicationFileService.getUploadedFiles(applicationFileUsage);
    assertThat(resultUploadedFiles)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyElementsOf(uploadedFiles);
  }

  @Test
  void getUploadedFiles_noUploadedFiles_thenEmptyList() {
    when(fileService.findAll(applicationFileUsage.usageId(), applicationFileUsage.usageType(), applicationFileUsage.documentType()))
        .thenReturn(Collections.emptyList());
    var resultUploadedFiles = applicationFileService.getUploadedFiles(applicationFileUsage);
    assertThat(resultUploadedFiles).isEmpty();
  }

  @Test
  void getUploadedFileForms() {
    when(fileService.findAll(uploadedFileIds)).thenReturn(uploadedFiles);
    var resultingUploadedFileForms = applicationFileService.getUploadedFileForms(uploadedFileIds);
    assertThat(resultingUploadedFileForms)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyElementsOf(resultingUploadedFileForms);
  }

  @Test
  void getUploadedFileForms_noUploadedFiles_thenEmptyList() {
    when(fileService.findAll(uploadedFileIds)).thenReturn(Collections.emptyList());
    var resultingUploadedFileForms = applicationFileService.getUploadedFileForms(uploadedFileIds);
    assertThat(resultingUploadedFileForms).isEmpty();
  }

  @Test
  void getFileNotFoundException() {
    var fileId = UUID.randomUUID();
    var result = applicationFileService.getFileNotFoundException(fileId, applicationFileUsage);
    assertThat(result)
        .isInstanceOf(ResponseStatusException.class)
        .matches(e -> e.getStatusCode().value() == HttpStatus.NOT_FOUND.value())
        .hasMessageContaining("File [%s] does not exist for %s [%s]".formatted(fileId, applicationFileUsage.usageType(), applicationFileUsage.usageId()));
  }

  @ParameterizedTest
  @MethodSource("throwIfFileDoesNotBelongToUsageParams")
  void throwIfFileDoesNotBelongToUsage(UploadedFile uploadedFile, ApplicationFileUsage applicationFileUsage) {
    assertThatThrownBy(() -> applicationFileService.throwIfFileDoesNotBelongToUsage(uploadedFile, applicationFileUsage))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("File [%s] does not exist for %s [%s]".formatted(uploadedFile.getId(), applicationFileUsage.usageType(), applicationFileUsage.usageId()));
  }

  private static Stream<Arguments> throwIfFileDoesNotBelongToUsageParams() {
    var unmodifiedFile = createdUploadedFile(DEFAULT_USAGE);

    return Stream.of(
        Arguments.of(unmodifiedFile, new TestApplicationFileUsage(USAGE_ID + "_", USAGE_TYPE, DOCUMENT_TYPE)),
        Arguments.of(unmodifiedFile, new TestApplicationFileUsage(USAGE_ID, USAGE_TYPE + "_", DOCUMENT_TYPE)),
        Arguments.of(unmodifiedFile, new TestApplicationFileUsage(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE + "_")),
        Arguments.of(unmodifiedFile, new TestApplicationFileUsage(null, USAGE_TYPE, DOCUMENT_TYPE)),
        Arguments.of(unmodifiedFile, new TestApplicationFileUsage(USAGE_ID, null, DOCUMENT_TYPE)),
        Arguments.of(unmodifiedFile, new TestApplicationFileUsage(USAGE_ID, USAGE_TYPE, null)),
        Arguments.of(createdUploadedFile(new TestApplicationFileUsage(USAGE_ID + "_", USAGE_TYPE, DOCUMENT_TYPE)), DEFAULT_USAGE),
        Arguments.of(createdUploadedFile(new TestApplicationFileUsage(USAGE_ID, USAGE_TYPE + "_", DOCUMENT_TYPE)), DEFAULT_USAGE),
        Arguments.of(createdUploadedFile(new TestApplicationFileUsage(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE + "_")), DEFAULT_USAGE),
        Arguments.of(createdUploadedFile(new TestApplicationFileUsage(null, USAGE_TYPE, DOCUMENT_TYPE)), DEFAULT_USAGE),
        Arguments.of(createdUploadedFile(new TestApplicationFileUsage(USAGE_ID, null, DOCUMENT_TYPE)), DEFAULT_USAGE),
        Arguments.of(createdUploadedFile(new TestApplicationFileUsage(USAGE_ID, USAGE_TYPE, null)), DEFAULT_USAGE)
    );
  }

  @ParameterizedTest
  @MethodSource("fileDoesBelongToUsageParams")
  void throwIfFileDoesNotBelongToUsage_whenFileDoesBelongToUsage_thenNoException(
      UploadedFile uploadedFile, ApplicationFileUsage applicationFileUsage
  ) {
    assertThatNoException()
        .isThrownBy(() -> applicationFileService.throwIfFileDoesNotBelongToUsage(uploadedFile, applicationFileUsage));
  }

  private static Stream<Arguments> fileDoesBelongToUsageParams() {
    var emptyUsage = new TestApplicationFileUsage(null, null, null);

    return Stream.of(
        Arguments.of(createdUploadedFile(DEFAULT_USAGE), DEFAULT_USAGE),
        Arguments.of(createdUploadedFile(emptyUsage), emptyUsage)
    );
  }

  @Test
  void doesFileHaveUsage_hasUsage_thenTrue() {
    var fileUsageWithId = new TestApplicationFileUsage("123", null, null);
    var fileUsageWithUsageType = new TestApplicationFileUsage(null, "S29 transaction", null);
    var fileUsageWithDocumentType = new TestApplicationFileUsage(null, null, "supporting-documents");

    var result = applicationFileService.doesFileHaveUsage(createdUploadedFile(fileUsageWithId));
    assertThat(result).isTrue();

    result = applicationFileService.doesFileHaveUsage(createdUploadedFile(fileUsageWithUsageType));
    assertThat(result).isTrue();

    result = applicationFileService.doesFileHaveUsage(createdUploadedFile(fileUsageWithDocumentType));
    assertThat(result).isTrue();

    result = applicationFileService.doesFileHaveUsage(createdUploadedFile(DEFAULT_USAGE));
    assertThat(result).isTrue();
  }

  @Test
  void doesFileHaveUsage_noUsage_thenTrue() {
    var emptyUsage = new TestApplicationFileUsage(null, null, null);
    var result = applicationFileService.doesFileHaveUsage(createdUploadedFile(emptyUsage));
    assertThat(result).isFalse();
  }

  @Test
  void fileBelongsToUser() {
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var uploadedFile = new UploadedFile();
    uploadedFile.setUploadedBy(user.wuaId().toString());

    assertThat(applicationFileService.fileBelongsToUser(uploadedFile, user)).isTrue();
  }

  @Test
  void fileBelongsToUser_noUploadedByUserOnFile() {
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var uploadedFile = new UploadedFile();

    assertThat(applicationFileService.fileBelongsToUser(uploadedFile, user)).isFalse();
  }

  @Test
  void fileBelongsToUser_wuaIdDoesNotMatch() {
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var uploadedFile = new UploadedFile();
    uploadedFile.setUploadedBy(String.valueOf(user.wuaId()+ 1));

    assertThat(applicationFileService.fileBelongsToUser(uploadedFile, user)).isFalse();
  }

  @Test
  void getUploadedFilesGroupedByUsageId() {
    uploadedFiles.add(createdUploadedFile(applicationFileUsage));

    when(fileService.findAllByUsageIdsWithUsageType(List.of(applicationFileUsage.usageId()), applicationFileUsage.usageType()))
        .thenReturn(uploadedFiles);
    var resultUploadedFiles = applicationFileService.getUploadedFilesGroupedByUsageId(List.of(applicationFileUsage.usageId()), applicationFileUsage.usageType());

    assertThat(resultUploadedFiles).containsEntry(applicationFileUsage.usageId(), uploadedFiles);
  }

  @Test
  void getUploadedFilesGroupedByUsageId_noUploadedFiles_thenEmptyMap() {
    when(fileService.findAllByUsageIdsWithUsageType(List.of(applicationFileUsage.usageId()), applicationFileUsage.usageType()))
        .thenReturn(Collections.emptyList());
    var resultUploadedFiles = applicationFileService.getUploadedFilesGroupedByUsageId(List.of(applicationFileUsage.usageId()), applicationFileUsage.usageType());
    assertThat(resultUploadedFiles).isEmpty();
  }

  private record TestApplicationFileUsage(
      String usageId,
      String usageType,
      String documentType
  ) implements ApplicationFileUsage {
  }

}