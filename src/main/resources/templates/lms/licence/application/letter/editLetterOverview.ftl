<#include '../../../layout/layout.ftl'>
<#import '../../../document/sections/_documentSections.ftl' as documentSections>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  breadcrumbs=breadcrumbs
  pageSize=PageSize.FULL_WIDTH
  errorSummaryItems=errorList
  pageHeadingClass="govuk-heading-xl govuk-!-margin-bottom-2"
>
    <@fdsForm.htmlForm actionUrl=springUrl(signUrl)>
      <@fdsAction.button buttonText="Approve and issue letters" buttonClass="govuk-button govuk-button--primary"/>
    </@fdsForm.htmlForm>

    <@fdsAction.link
    linkText="Preview document"
    linkUrl=springUrl(previewUrl)
    linkClass="govuk-button govuk-button--secondary"
    role=true
    openInNewTab=true
    />

    <@fdsAction.link
    linkText="Reload document"
    linkUrl=springUrl(reloadUrl)
    linkClass="govuk-button govuk-button--secondary"
    />

    <@documentSections.documentSections
    topLevelDocumentSectionSummaryViews=documentInstanceSectionsSummaryView.topLevelDocumentInstanceSectionSummaryViews()
    accordionId=accordionId
    includeRemove=hasMoreThanOneSection
    isTemplate=false
    errorList=errorList
    userHasValidPermission=userHasValidPermission
    />
</@defaultPage>