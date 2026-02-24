package uk.co.nstauthority.licensingmanagementservice.document.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateMetadataService;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateMetadataTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.LmsDocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentTemplateController;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ExtendWith(MockitoExtension.class)
class DocumentTemplateSearchServiceTest {

  @Mock
  private DocumentTemplateService documentTemplateService;

  @Mock
  private DocumentTemplateMetadataService documentTemplateMetadataService;

  @InjectMocks
  private DocumentTemplateSearchService documentTemplateSearchService;

  @Test
  void getDocumentTemplateSearchItems_noFilters() {
    var form = new DocumentTemplateSearchFilterForm();

    var documentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().build();
    var metadata = DocumentTemplateMetadataTestUtil.newBuilder().withDocumentTemplateId(documentTemplateDto.id()).build();
    when(documentTemplateMetadataService.getAllDocumentTemplateMetadata()).thenReturn(List.of(metadata));
    when(documentTemplateService.getDocumentTemplateDtos()).thenReturn(List.of(documentTemplateDto));

    assertThat(documentTemplateSearchService.getDocumentTemplateSearchItems(form))
        .containsExactly(LmsDocumentTemplateDto.from(
            metadata,
            documentTemplateDto,
            ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(documentTemplateDto.id(), null))
        )
        );
  }

  @Test
  void getDocumentTemplateSearchItems_isSortedAlphabetically() {
    var form = new DocumentTemplateSearchFilterForm();

    var firstDocumentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder()
        .withTitle("aaa")
        .build();
    var firstMetadata = DocumentTemplateMetadataTestUtil.newBuilder()
        .withDocumentTemplateId(firstDocumentTemplateDto.id())
        .build();

    var secondDocumentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder()
        .withTitle("BBB")
        .build();
    var secondMetadata = DocumentTemplateMetadataTestUtil.newBuilder()
        .withDocumentTemplateId(secondDocumentTemplateDto.id())
        .build();

    when(documentTemplateMetadataService.getAllDocumentTemplateMetadata())
        .thenReturn(List.of(secondMetadata, firstMetadata));
    when(documentTemplateService.getDocumentTemplateDtos())
        .thenReturn(List.of(secondDocumentTemplateDto, firstDocumentTemplateDto));

    assertThat(documentTemplateSearchService.getDocumentTemplateSearchItems(form))
        .containsExactly(
            LmsDocumentTemplateDto.from(
                firstMetadata,
                firstDocumentTemplateDto,
                ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(firstDocumentTemplateDto.id(), null))
            ),
            LmsDocumentTemplateDto.from(
                secondMetadata,
                secondDocumentTemplateDto,
                ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(secondDocumentTemplateDto.id(), null))
            )
        );
  }

  @Test
  void getDocumentTemplateSearchItems_whenLicenceTypeFilter_thenFilterOut() {
    var form = new DocumentTemplateSearchFilterForm();
    form.setLicenceTypes(List.of(LicenceType.SEAWARD_PRODUCTION.getEnumName()));

    var differentDocumentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().build();
    var differentMetadata = DocumentTemplateMetadataTestUtil.newBuilder()
        .withDocumentTemplateId(differentDocumentTemplateDto.id())
        .withLicenceType(LicenceType.SEAWARD_EXPLORATION)
        .build();

    var matchingDocumentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().build();
    var matchingMetadata = DocumentTemplateMetadataTestUtil.newBuilder()
        .withDocumentTemplateId(matchingDocumentTemplateDto.id())
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    when(documentTemplateMetadataService.getAllDocumentTemplateMetadata())
        .thenReturn(List.of(differentMetadata, matchingMetadata));
    when(documentTemplateService.getDocumentTemplateDtos())
        .thenReturn(List.of(differentDocumentTemplateDto, matchingDocumentTemplateDto));

    assertThat(documentTemplateSearchService.getDocumentTemplateSearchItems(form))
        .containsExactly(LmsDocumentTemplateDto.from(
            matchingMetadata,
            matchingDocumentTemplateDto,
            ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(matchingDocumentTemplateDto.id(), null))
        )
        );
  }

  @Test
  void getDocumentTemplateSearchItems_whenDocumentTitleFilter_thenFilterOut() {
    var form = new DocumentTemplateSearchFilterForm();
    form.setDocumentTemplateTitle("aaa");

    var documentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().withTitle("BBB").build();
    var metadata = DocumentTemplateMetadataTestUtil.newBuilder()
        .withDocumentTemplateId(documentTemplateDto.id())
        .build();

    var matchingDocumentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().withTitle("AAA").build();
    var matchingMetadata = DocumentTemplateMetadataTestUtil.newBuilder()
        .withDocumentTemplateId(matchingDocumentTemplateDto.id())
        .build();

    when(documentTemplateMetadataService.getAllDocumentTemplateMetadata()).thenReturn(List.of(metadata, matchingMetadata));
    when(documentTemplateService.getDocumentTemplateDtos()).thenReturn(List.of(documentTemplateDto, matchingDocumentTemplateDto));

    assertThat(documentTemplateSearchService.getDocumentTemplateSearchItems(form))
        .containsExactly(LmsDocumentTemplateDto.from(
            matchingMetadata,
            matchingDocumentTemplateDto,
            ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(matchingDocumentTemplateDto.id(), null))
        )
        );
  }
}