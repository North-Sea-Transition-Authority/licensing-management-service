package uk.co.nstauthority.licensingmanagementservice.licence.application.letter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindingResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionForm;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionFormValidator;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionsSummaryView;
import uk.co.nstauthority.licensingmanagementservice.document.instance.DocumentInstanceSectionDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.instance.DocumentInstanceSectionSummaryViewTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.instance.LmsDocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.DigitalSignatureMailMergeField;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;

@ExtendWith(MockitoExtension.class)
class ApplicationLetterValidationServiceTest {

  @Mock
  private LmsDocumentInstanceService lmsDocumentInstanceService;

  @Mock
  private DocumentInstanceSectionFormValidator documentInstanceSectionFormValidator;

  @InjectMocks
  private ApplicationLetterValidationService applicationLetterValidationService;


  @ParameterizedTest
  @MethodSource("getContentWithErrors")
  void getDocumentSectionOverviewError_withUnresolvedMailMergeFields(String invalidContent) {
    var summaryViewWithErrors = DocumentInstanceSectionSummaryViewTestUtil.newBuilder()
        .withContent(invalidContent)
        .build();
    var summaryViewWithoutErrors = DocumentInstanceSectionSummaryViewTestUtil.newBuilder()
        .withErrorMessages(List.of())
        .build();

    var sectionsSummaryView = DocumentInstanceSectionsSummaryView.from(List.of(summaryViewWithErrors, summaryViewWithoutErrors));

    var resultingErrors = applicationLetterValidationService.getDocumentSectionOverviewError(sectionsSummaryView);
    assertThat(resultingErrors).extracting(
            ErrorSummaryItem::getDisplayOrder,
            ErrorSummaryItem::getFieldName,
            ErrorSummaryItem::getErrorMessage
        )
        .containsExactly(
            tuple(
            0,
            "summaryaccordion-%s-error".formatted(summaryViewWithErrors.id()),
            "There are invalid mail merge fields in %s".formatted(summaryViewWithErrors.title())
            )
        );
  }

  @Test
  void getDocumentSectionOverviewError_withDigitalSignature() {
    var summaryViewWithDigitalSignature = DocumentInstanceSectionSummaryViewTestUtil.newBuilder()
        .withContent("Hello %s, ".formatted(DigitalSignatureMailMergeField.SIGNATURE_PLACEHOLDER_TEXT))
        .build();

    var sectionsSummaryView = DocumentInstanceSectionsSummaryView.from(List.of(summaryViewWithDigitalSignature));

    var resultingErrors = applicationLetterValidationService.getDocumentSectionOverviewError(sectionsSummaryView);
    assertThat(resultingErrors).isEmpty();
  }

  @Test
  void getDocumentSectionOverviewError_withResolvedErrors() {
    var summaryViewWithErrors = DocumentInstanceSectionSummaryViewTestUtil.newBuilder()
        .withErrorMessages(List.of("there are errors"))
        .build();
    var summaryViewWithoutErrors = DocumentInstanceSectionSummaryViewTestUtil.newBuilder()
        .withErrorMessages(List.of())
        .build();

    var sectionsSummaryView = DocumentInstanceSectionsSummaryView.from(List.of(summaryViewWithErrors, summaryViewWithoutErrors));

    var resultingErrors = applicationLetterValidationService.getDocumentSectionOverviewError(sectionsSummaryView);
    assertThat(resultingErrors).extracting(
            ErrorSummaryItem::getDisplayOrder,
            ErrorSummaryItem::getFieldName,
            ErrorSummaryItem::getErrorMessage
        )
        .containsExactly(
            tuple(
                0,
                "summaryaccordion-%s-error".formatted(summaryViewWithErrors.id()),
                "There are invalid mail merge fields in %s".formatted(summaryViewWithErrors.title())
            )
        );
  }

  @Test
  void getDocumentSectionSpecificErrors_whenHasErrors() {
    var errorMessageFromResolvedContent = "There are errors with resolved values";
    var validatorErrorMessage = "There are the following errors in this section: validation errors";
    var documentSection = DocumentInstanceSectionDtoTestUtil.newBuilder().build();
    when(lmsDocumentInstanceService.getDocumentInstanceSectionErrors(documentSection))
        .thenReturn(List.of(errorMessageFromResolvedContent));

    var form = DocumentInstanceSectionForm.from(documentSection);

    doAnswer(invocation -> {
      var bindingResult = invocation.getArgument(2, BindingResult.class);
      bindingResult.rejectValue("content", "mandatory", validatorErrorMessage);
      return null;
    }).when(documentInstanceSectionFormValidator).validate(eq(form), eq(documentSection.documentInstanceDto()), any(BindingResult.class));

    var resultingBindingResult = applicationLetterValidationService.getDocumentSectionSpecificErrors(form, documentSection);

    assertThat(resultingBindingResult.hasErrors()).isTrue();
    assertThat(resultingBindingResult.getFieldErrors())
        .extracting(DefaultMessageSourceResolvable::getDefaultMessage)
        .containsExactly("%s, %s".formatted(validatorErrorMessage, errorMessageFromResolvedContent));
  }

  @Test
  void getDocumentSectionSpecificErrors_whenHasErrorFromResolvedContent() {
    var errorMessageFromResolvedContent = "There are errors with resolved values";
    var documentSection = DocumentInstanceSectionDtoTestUtil
        .newBuilder().build();
    when(lmsDocumentInstanceService.getDocumentInstanceSectionErrors(documentSection))
        .thenReturn(List.of(errorMessageFromResolvedContent));

    var form = DocumentInstanceSectionForm.from(documentSection);


    var resultingBindingResult = applicationLetterValidationService.getDocumentSectionSpecificErrors(form, documentSection);

    assertThat(resultingBindingResult.hasErrors()).isTrue();
    assertThat(resultingBindingResult.getFieldErrors())
        .extracting(DefaultMessageSourceResolvable::getDefaultMessage)
        .containsExactly("There are the following errors in this section: %s".formatted(errorMessageFromResolvedContent));
  }

  @Test
  void getDocumentSectionSpecificErrors_whenHasNoErrors() {
    var documentSection = DocumentInstanceSectionDtoTestUtil.newBuilder().build();
    when(lmsDocumentInstanceService.getDocumentInstanceSectionErrors(documentSection))
        .thenReturn(List.of());
    var form = DocumentInstanceSectionForm.from(documentSection);

    var resultingBindingResult = applicationLetterValidationService.getDocumentSectionSpecificErrors(form, documentSection);

    assertThat(resultingBindingResult.hasErrors()).isFalse();
  }

  private static Stream<Arguments> getContentWithErrors() {
    return Stream.of(
        Arguments.of("((MAIL_MERGE_ERROR))"),
        Arguments.of("??MAIL_MERGE_ERROR??")
    );
  }
}