<#include '../../layout/layout.ftl'>
<#include '_documentSectionActions.ftl'>

<#macro documentSectionSummaryViewContent documentSectionSummaryView userHasValidPermission isTemplate includeRemove=true>
  <#if isTemplate>
    <#assign documentSectionUrls = documentSectionSummaryView.documentTemplateSectionUrls()/>
  <#else>
    <#assign documentSectionUrls = documentSectionSummaryView.documentInstanceSectionUrls()/>
  </#if>

  <div>
    <#if documentSectionSummaryView.hasPageBreakBefore()>
      <strong class="govuk-tag govuk-tag--blue govuk-body govuk-secondary-text-colour govuk-!-font-weight-bold">NEW PAGE</strong>
    </#if>
  </div>

  <#if isTemplate && documentSectionSummaryView.conditionTitle()?has_content>
    <div class="govuk-hint">Condition: ${documentSectionSummaryView.conditionTitle()}</div>
  </#if>

    <#if userHasValidPermission>
        <@sectionActions
        includeRemove=includeRemove
        documentSectionSummaryView=documentSectionSummaryView
        documentSectionUrls=documentSectionUrls/>
    </#if>

  <div class="govuk-body govuk-!-margin-top-4">
    ${(documentSectionSummaryView.content()!)?no_esc}
  </div>

  <#list documentSectionSummaryView.children() as child>
    <@documentSectionSummaryViewContent documentSectionSummaryView=child userHasValidPermission=userHasValidPermission includeRemove=includeRemove isTemplate=isTemplate/>
  </#list>
</#macro>