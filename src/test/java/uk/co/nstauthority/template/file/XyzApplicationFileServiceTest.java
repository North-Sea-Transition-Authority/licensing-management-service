package uk.co.nstauthority.template.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.template.file.XyzApplicationFileTestUtil.createdUploadedFile;

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
import uk.co.fivium.fileuploadlibrary.core.FileUsage;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.nstauthority.template.authentication.ServiceUserDetailTestUtil;

@ExtendWith(MockitoExtension.class)
class XyzApplicationFileServiceTest {

  private static final String USAGE_ID = UUID.randomUUID().toString();
  private static final String USAGE_TYPE = "TestUsage";
  private static final String DOCUMENT_TYPE = "supporting-document";
  private static final XyzApplicationFileUsage DEFAULT_USAGE = new TestXyzApplicationFileUsage(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE);

  @Mock
  private FileService fileService;

  @InjectMocks
  private XyzApplicationFileService xyzApplicationFileService;

  @Captor
  private ArgumentCaptor<Function<FileUsage.Builder, FileUsage>> fileUsageFunctionCaptor;

  private XyzApplicationFileUsage fileUsage;
  private List<UploadedFile> uploadedFiles;
  private List<UUID> uploadedFileIds;
  private List<UploadedFileForm> uploadedFileForms;

  @BeforeEach
  void setUp() {
    fileUsage = new TestXyzApplicationFileUsage(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE);

    uploadedFiles = new ArrayList<>();
    uploadedFiles.add(createdUploadedFile(fileUsage));

    uploadedFileIds = uploadedFiles.stream().map(UploadedFile::getId).toList();
    uploadedFileForms = uploadedFiles.stream().map(FileUploadLibraryUtils::asForm).toList();
  }

  @Test
  void saveDocuments() {
    doAnswer(invocation -> {
      var builderFunction = invocation.getArgument(1, Function.class);
      builderFunction.apply(FileUsage.newBuilder());
      return null;
    })
        .when(fileService)
        .updateUsageAndDescription(any(UploadedFile.class), any(), anyString());

    when(fileService.findAll(uploadedFileIds)).thenReturn(uploadedFiles);

    xyzApplicationFileService.saveDocuments(fileUsage, uploadedFileForms);

    for (var uploadedFile : uploadedFiles) {
      verify(fileService).updateUsageAndDescription(
          eq(uploadedFile),
          fileUsageFunctionCaptor.capture(),
          eq(XyzApplicationFileTestUtil.FILE_DESCRIPTION)
      );
      assertThat(fileUsageFunctionCaptor.getValue().apply(FileUsage.newBuilder()))
          .extracting(
              FileUsage::usageId,
              FileUsage::usageType,
              FileUsage::documentType
          ).containsExactly(
              USAGE_ID,
              USAGE_TYPE,
              DOCUMENT_TYPE
          );
    }
  }

  @Test
  void deleteFiles() {
    when(fileService.findAll(fileUsage.usageId(), fileUsage.usageType(), fileUsage.documentType()))
        .thenReturn(uploadedFiles);
    xyzApplicationFileService.deleteFiles(fileUsage);
    for (var uploadedFile: uploadedFiles) {
      verify(fileService).delete(uploadedFile);
    }
  }

  @Test
  void getUploadedFiles() {
    when(fileService.findAll(fileUsage.usageId(), fileUsage.usageType(), fileUsage.documentType()))
        .thenReturn(uploadedFiles);
    var resultUploadedFiles = xyzApplicationFileService.getUploadedFiles(fileUsage);
    assertThat(resultUploadedFiles)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyElementsOf(uploadedFiles);
  }

  @Test
  void getUploadedFiles_noUploadedFiles_thenEmptyList() {
    when(fileService.findAll(fileUsage.usageId(), fileUsage.usageType(), fileUsage.documentType()))
        .thenReturn(Collections.emptyList());
    var resultUploadedFiles = xyzApplicationFileService.getUploadedFiles(fileUsage);
    assertThat(resultUploadedFiles).isEmpty();
  }

  @Test
  void getUploadedFileForms() {
    when(fileService.findAll(uploadedFileIds)).thenReturn(uploadedFiles);
    var resultingUploadedFileForms = xyzApplicationFileService.getUploadedFileForms(uploadedFileIds);
    assertThat(resultingUploadedFileForms)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyElementsOf(resultingUploadedFileForms);
  }

  @Test
  void getUploadedFileForms_noUploadedFiles_thenEmptyList() {
    when(fileService.findAll(uploadedFileIds)).thenReturn(Collections.emptyList());
    var resultingUploadedFileForms = xyzApplicationFileService.getUploadedFileForms(uploadedFileIds);
    assertThat(resultingUploadedFileForms).isEmpty();
  }

  @Test
  void getFileNotFoundException() {
    var fileId = UUID.randomUUID();
    var result = xyzApplicationFileService.getFileNotFoundException(fileId, fileUsage);
    assertThat(result)
        .isInstanceOf(ResponseStatusException.class)
        .matches(e -> e.getStatusCode().value() == HttpStatus.NOT_FOUND.value())
        .hasMessageContaining("File [%s] does not exist for %s [%s]".formatted(fileId, fileUsage.usageType(), fileUsage.usageId()));
  }

  @ParameterizedTest
  @MethodSource("throwIfFileDoesNotBelongToUsageParams")
  void throwIfFileDoesNotBelongToUsage(UploadedFile uploadedFile, XyzApplicationFileUsage fileUsage) {
    assertThatThrownBy(() -> xyzApplicationFileService.throwIfFileDoesNotBelongToUsage(uploadedFile, fileUsage))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("File [%s] does not exist for %s [%s]".formatted(uploadedFile.getId(), fileUsage.usageType(), fileUsage.usageId()));
  }

  private static Stream<Arguments> throwIfFileDoesNotBelongToUsageParams() {
    var unmodifiedFile = createdUploadedFile(DEFAULT_USAGE);

    return Stream.of(
        Arguments.of(unmodifiedFile, new TestXyzApplicationFileUsage(USAGE_ID + "_", USAGE_TYPE, DOCUMENT_TYPE)),
        Arguments.of(unmodifiedFile, new TestXyzApplicationFileUsage(USAGE_ID, USAGE_TYPE + "_", DOCUMENT_TYPE)),
        Arguments.of(unmodifiedFile, new TestXyzApplicationFileUsage(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE + "_")),
        Arguments.of(unmodifiedFile, new TestXyzApplicationFileUsage(null, USAGE_TYPE, DOCUMENT_TYPE)),
        Arguments.of(unmodifiedFile, new TestXyzApplicationFileUsage(USAGE_ID, null, DOCUMENT_TYPE)),
        Arguments.of(unmodifiedFile, new TestXyzApplicationFileUsage(USAGE_ID, USAGE_TYPE, null)),
        Arguments.of(createdUploadedFile(new TestXyzApplicationFileUsage(USAGE_ID + "_", USAGE_TYPE, DOCUMENT_TYPE)), DEFAULT_USAGE),
        Arguments.of(createdUploadedFile(new TestXyzApplicationFileUsage(USAGE_ID, USAGE_TYPE + "_", DOCUMENT_TYPE)), DEFAULT_USAGE),
        Arguments.of(createdUploadedFile(new TestXyzApplicationFileUsage(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE + "_")), DEFAULT_USAGE),
        Arguments.of(createdUploadedFile(new TestXyzApplicationFileUsage(null, USAGE_TYPE, DOCUMENT_TYPE)), DEFAULT_USAGE),
        Arguments.of(createdUploadedFile(new TestXyzApplicationFileUsage(USAGE_ID, null, DOCUMENT_TYPE)), DEFAULT_USAGE),
        Arguments.of(createdUploadedFile(new TestXyzApplicationFileUsage(USAGE_ID, USAGE_TYPE, null)), DEFAULT_USAGE)
    );
  }

  @ParameterizedTest
  @MethodSource("fileDoesBelongToUsageParams")
  void throwIfFileDoesNotBelongToUsage_whenFileDoesBelongToUsage_thenNoException(
      UploadedFile uploadedFile, XyzApplicationFileUsage fileUsage
  ) {
    assertThatNoException()
        .isThrownBy(() -> xyzApplicationFileService.throwIfFileDoesNotBelongToUsage(uploadedFile, fileUsage));
  }

  private static Stream<Arguments> fileDoesBelongToUsageParams() {
    var emptyUsage = new TestXyzApplicationFileUsage(null, null, null);

    return Stream.of(
        Arguments.of(createdUploadedFile(DEFAULT_USAGE), DEFAULT_USAGE),
        Arguments.of(createdUploadedFile(emptyUsage), emptyUsage)
    );
  }

  @Test
  void doesFileHaveUsage_hasUsage_thenTrue() {
    var fileUsageWithId = new TestXyzApplicationFileUsage("123", null, null);
    var fileUsageWithUsageType = new TestXyzApplicationFileUsage(null, "S29 transaction", null);
    var fileUsageWithDocumentType = new TestXyzApplicationFileUsage(null, null, "supporting-documents");

    var result = xyzApplicationFileService.doesFileHaveUsage(createdUploadedFile(fileUsageWithId));
    assertThat(result).isTrue();

    result = xyzApplicationFileService.doesFileHaveUsage(createdUploadedFile(fileUsageWithUsageType));
    assertThat(result).isTrue();

    result = xyzApplicationFileService.doesFileHaveUsage(createdUploadedFile(fileUsageWithDocumentType));
    assertThat(result).isTrue();

    result = xyzApplicationFileService.doesFileHaveUsage(createdUploadedFile(DEFAULT_USAGE));
    assertThat(result).isTrue();
  }

  @Test
  void doesFileHaveUsage_noUsage_thenTrue() {
    var emptyUsage = new TestXyzApplicationFileUsage(null, null, null);
    var result = xyzApplicationFileService.doesFileHaveUsage(createdUploadedFile(emptyUsage));
    assertThat(result).isFalse();
  }

  @Test
  void fileBelongsToUser() {
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var uploadedFile = new UploadedFile();
    uploadedFile.setUploadedBy(user.wuaId().toString());

    assertThat(xyzApplicationFileService.fileBelongsToUser(uploadedFile, user)).isTrue();
  }

  @Test
  void fileBelongsToUser_noUploadedByUserOnFile() {
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var uploadedFile = new UploadedFile();

    assertThat(xyzApplicationFileService.fileBelongsToUser(uploadedFile, user)).isFalse();
  }

  @Test
  void fileBelongsToUser_wuaIdDoesNotMatch() {
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    var uploadedFile = new UploadedFile();
    uploadedFile.setUploadedBy(String.valueOf(user.wuaId()+ 1));

    assertThat(xyzApplicationFileService.fileBelongsToUser(uploadedFile, user)).isFalse();
  }

  @Test
  void getUploadedFilesGroupedByUsageId() {
    uploadedFiles.add(createdUploadedFile(fileUsage));

    when(fileService.findAllByUsageIdsWithUsageType(List.of(fileUsage.usageId()), fileUsage.usageType()))
        .thenReturn(uploadedFiles);
    var resultUploadedFiles = xyzApplicationFileService.getUploadedFilesGroupedByUsageId(List.of(fileUsage.usageId()), fileUsage.usageType());

    assertThat(resultUploadedFiles).containsEntry(fileUsage.usageId(), uploadedFiles);
  }

  @Test
  void getUploadedFilesGroupedByUsageId_noUploadedFiles_thenEmptyMap() {
    when(fileService.findAllByUsageIdsWithUsageType(List.of(fileUsage.usageId()), fileUsage.usageType()))
        .thenReturn(Collections.emptyList());
    var resultUploadedFiles = xyzApplicationFileService.getUploadedFilesGroupedByUsageId(List.of(fileUsage.usageId()), fileUsage.usageType());
    assertThat(resultUploadedFiles).isEmpty();
  }

  private record TestXyzApplicationFileUsage(
      String usageId,
      String usageType,
      String documentType
  ) implements XyzApplicationFileUsage {
  }

}