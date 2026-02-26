<#include '../../layout/layout.ftl'>

<#macro sectionActions documentSectionSummaryView documentSectionUrls includeRemove=true>
  
  <@fdsActionDropdown.actionDropdown dropdownButtonText="Add section">
    <@fdsActionDropdown.actionDropdownItem
      actionText="Add section before"
      linkAction=true
      linkActionUrl=springUrl(documentSectionUrls.addSectionBeforeUrl())
      linkActionScreenReaderText=documentSectionSummaryView.titleWithSectionNumber()
    />

    <@fdsActionDropdown.actionDropdownItem
      actionText="Add section after"
      linkAction=true
      linkActionUrl=springUrl(documentSectionUrls.addSectionAfterUrl())
      linkActionScreenReaderText=documentSectionSummaryView.titleWithSectionNumber()
    />

    <@fdsActionDropdown.actionDropdownItem
      actionText="Add subsection"
      linkAction=true
      linkActionUrl=springUrl(documentSectionUrls.addSubsectionUrl())
      linkActionScreenReaderText=documentSectionSummaryView.titleWithSectionNumber()
    />
  </@fdsActionDropdown.actionDropdown>

  <@fdsAction.link
    linkText="Edit"
    linkUrl=springUrl(documentSectionUrls.editUrl())
    linkScreenReaderText=documentSectionSummaryView.titleWithSectionNumber()
    linkClass="govuk-button govuk-button--secondary"
  />

  <#if includeRemove>
    <@fdsAction.link
      linkText="Remove"
      linkUrl=springUrl(documentSectionUrls.removeUrl())
      linkScreenReaderText=documentSectionSummaryView.titleWithSectionNumber()
      linkClass="govuk-button govuk-button--secondary"
    />
  </#if>
</#macro>