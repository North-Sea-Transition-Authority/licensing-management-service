<#include '../../layout/layout.ftl'>
<#include '_documentSectionSummaryViewContent.ftl'>

<#macro documentSections topLevelDocumentSectionSummaryViews accordionId isTemplate includeRemove=true errorList=[]>
  <@fdsAccordion.accordion accordionId="summaryaccordion-${accordionId}" rememberExpanded=false>
    <#list topLevelDocumentSectionSummaryViews as documentSectionSummaryView>
        <#list errorList as error>
            <#if error.fieldName == "summaryaccordion-${documentSectionSummaryView.id()}-error">
                <#assign errorMsg>
                    ${error.errorMessage}
                </#assign>
                <#break>
            <#else>
                <#assign errorMsg=""/>
            </#if>
        </#list>
      <@fdsAccordion.accordionSection
        sectionHeading=documentSectionSummaryView.titleWithSectionNumber()
        summaryText=""
        openSection=errorMsg?has_content
      >
        <div class="<#if errorMsg?has_content> govuk-form-group--error</#if>">
          <#if errorMsg?has_content>
            <p id="summaryaccordion-${documentSectionSummaryView.id()}-error" class="govuk-error-message">
              <span class="govuk-visually-hidden">Error:</span> ${errorMsg}
            </p>
          </#if>
          <@documentSectionSummaryViewContent
            documentSectionSummaryView=documentSectionSummaryView
            isTemplate=isTemplate
            includeRemove=includeRemove
          />
        </div>
      </@fdsAccordion.accordionSection>
    </#list>
  </@fdsAccordion.accordion>
</#macro>