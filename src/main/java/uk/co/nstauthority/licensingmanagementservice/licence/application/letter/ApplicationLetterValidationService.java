package uk.co.nstauthority.licensingmanagementservice.licence.application.letter;

import io.micrometer.common.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionForm;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionFormValidator;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionsSummaryView;
import uk.co.nstauthority.licensingmanagementservice.document.MailMergeValidationUtil;
import uk.co.nstauthority.licensingmanagementservice.document.instance.LmsDocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;

@Service
public class ApplicationLetterValidationService {

  private final DocumentInstanceSectionFormValidator documentInstanceSectionFormValidator;
  private final LmsDocumentInstanceService documentInstanceService;

  private static final String CONTENT = "content";

  @Autowired
  public ApplicationLetterValidationService(
      DocumentInstanceSectionFormValidator documentInstanceSectionFormValidator,
      LmsDocumentInstanceService documentInstanceService
  ) {
    this.documentInstanceSectionFormValidator = documentInstanceSectionFormValidator;
    this.documentInstanceService = documentInstanceService;
  }

  public List<ErrorSummaryItem> getDocumentSectionOverviewError(
      DocumentInstanceSectionsSummaryView documentInstanceSectionsSummaryView
  ) {
    var errorList = new ArrayList<ErrorSummaryItem>();
    documentInstanceSectionsSummaryView.topLevelDocumentInstanceSectionSummaryViews()
        .forEach(documentInstanceSectionSummaryView -> {
          var errors = documentInstanceSectionSummaryView.errorMessages();
          var content = documentInstanceSectionSummaryView.content();

          if (CollectionUtils.isNotEmpty(errors)
              || MailMergeValidationUtil.contentContainsManualMailMergeFields(content)
              || MailMergeValidationUtil.contentContainsNonApplicableMailMergeFields(content)) {
            errorList.add(new ErrorSummaryItem(
                0,
                "summaryaccordion-%s-error".formatted(documentInstanceSectionSummaryView.id()),
                "There are invalid mail merge fields in %s".formatted(documentInstanceSectionSummaryView.title())
            ));
          }
        });
    return errorList;
  }

  public BindingResult getDocumentSectionSpecificErrors(
      DocumentInstanceSectionForm form,
      DocumentInstanceSectionDto documentSectionDto
  ) {
    var errors = documentInstanceService.getDocumentInstanceSectionErrors(documentSectionDto);
    var validationResult = new BeanPropertyBindingResult(form, "form");
    documentInstanceSectionFormValidator.validate(form, documentSectionDto.documentInstanceDto(), validationResult);

    var contentError = validationResult.hasFieldErrors(CONTENT)
                       ? validationResult.getFieldErrors(CONTENT).getFirst().getDefaultMessage()
                       : null;

    var sectionErrors = CollectionUtils.isNotEmpty(errors)
                        ? String.join(", ", errors)
                        : null;

    var finalMessage = Stream.of(contentError, sectionErrors)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.joining(", "));

    if (contentError == null && sectionErrors != null) {
      finalMessage = "There are the following errors in this section: %s".formatted(finalMessage);
    }

    var bindingResult = new BeanPropertyBindingResult(form, "form");

    if (StringUtils.isNotBlank(finalMessage)) {
      bindingResult.rejectValue(CONTENT, "content.invalid", finalMessage);
    }

    return bindingResult;
  }
}