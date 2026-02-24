<#include '../layout/layout.ftl'>
<#import 'sections/_documentSections.ftl' as documentSections>
<#import '../macros/caseprocessingtabs/caseProccessingTabs.ftl' as caseProcessingTabs>

<#-- @ftlvariable name="errorList" type="java.util.List<uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem>" -->
<#-- @ftlvariable name="breadcrumbs" type="java.util.Map<java.lang.String, uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbItem>" -->

<#assign pageTitle="${documentTemplateDto.title()}"/>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  breadcrumbs=breadcrumbs
  pageSize=PageSize.FULL_WIDTH
  pageHeadingClass="govuk-heading-xl govuk-!-margin-bottom-2"
>
      <@documentSections.documentSections
        topLevelDocumentSectionSummaryViews=documentSectionsSummaryView.topLevelDocumentTemplateSectionSummaryViews()
        accordionId=accordionId
        isTemplate=true
      />

</@defaultPage>