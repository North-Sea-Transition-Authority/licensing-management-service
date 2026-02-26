<#include '../layout/layout.ftl'>
<#import 'sections/_documentSections.ftl' as documentSections>
<#import '../macros/caseprocessingtabs/caseProccessingTabs.ftl' as caseProcessingTabs>

<#assign pageTitle="${documentTemplateDto.title()}"/>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  breadcrumbs=breadcrumbs
  pageSize=PageSize.FULL_WIDTH
  pageHeadingClass="govuk-heading-xl govuk-!-margin-bottom-2"
>
    <#if previewWithConditionsUrl?has_content && previewWithoutConditionsUrl?has_content>
        <@fdsActionDropdown.actionDropdown dropdownButtonText="Preview document" dropdownButtonClass="govuk-button govuk-!-margin-bottom-4">
            <@fdsActionDropdown.actionDropdownItem
            actionText="With conditional sections"
            linkActionUrl=springUrl(previewWithConditionsUrl)
            linkAction=true
            />
            <@fdsActionDropdown.actionDropdownItem
            actionText="Without conditional sections"
            linkActionUrl=springUrl(previewWithoutConditionsUrl)
            linkAction=true
            />
        </@fdsActionDropdown.actionDropdown>
    <#elseif previewWithConditionsUrl?has_content && !previewWithoutConditionsUrl?has_content>
        <@fdsAction.link
        linkText="Preview document"
        linkUrl=springUrl(previewWithConditionsUrl)
        linkClass="govuk-button"
        role=true
        openInNewTab=true
        />
    <#else>
        <@fdsAction.link
        linkText="Preview document"
        linkUrl=springUrl(previewWithoutConditionsUrl)
        linkClass="govuk-button"
        role=true
        openInNewTab=true
        />
    </#if>
      <@documentSections.documentSections
        userHasValidPermission=userHasValidPermission
        topLevelDocumentSectionSummaryViews=documentSectionsSummaryView.topLevelDocumentTemplateSectionSummaryViews()
        accordionId=accordionId
        isTemplate=true
      />

</@defaultPage>