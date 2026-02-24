package uk.co.nstauthority.licensingmanagementservice.document;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateNotFoundException;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;
import uk.co.nstauthority.licensingmanagementservice.util.RequestUtil;

@Component
public class DocumentTemplateArgumentResolver implements HandlerMethodArgumentResolver {

  static final String DOCUMENT_TEMPLATE_ID = "documentTemplateId";
  private final DocumentTemplateService documentTemplateService;

  public DocumentTemplateArgumentResolver(DocumentTemplateService documentTemplateService) {
    this.documentTemplateService = documentTemplateService;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(DocumentTemplateDto.class);
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory
  ) {
    var documentTemplateId = RequestUtil.getId(((ServletWebRequest) webRequest).getRequest(), DOCUMENT_TEMPLATE_ID)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Missing required %s param".formatted(DOCUMENT_TEMPLATE_ID)
        ));

    try {
      return documentTemplateService.getDocumentTemplateDtoOrThrow(documentTemplateId);
    } catch (DocumentTemplateNotFoundException documentTemplateNotFoundException) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, documentTemplateNotFoundException.getMessage());
    }
  }
}
