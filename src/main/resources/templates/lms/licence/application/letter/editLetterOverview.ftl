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
    <@documentSections.documentSections
    topLevelDocumentSectionSummaryViews=documentInstanceSectionsSummaryView.topLevelDocumentInstanceSectionSummaryViews()
    accordionId=accordionId
    includeRemove=hasMoreThanOneSection
    isTemplate=false
    errorList=errorList
    userHasValidPermission=userHasValidPermission
    />
</@defaultPage>