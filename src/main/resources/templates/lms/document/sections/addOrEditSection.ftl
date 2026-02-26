<#include '../../layout/layout.ftl'>
<#import '../../macros/document/_editDocumentSectionForm.ftl' as editDocumentSectionForm>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_WIDTH
  errorSummaryItems=errorList
  breadcrumbs=breadcrumbs
>
  <@fdsForm.htmlForm>
    <@editDocumentSectionForm.editDocumentSectionForm form=form mailMergeFieldViews=mailMergeFieldViews conditionsFdsSelectMap=conditionsFdsSelectMap/>

    <@fdsAction.submitButtons
      primaryButtonText="Save"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(cancelUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
