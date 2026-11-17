<#include '../layout/layout.ftl'>
<#import '_summaryDetails.ftl' as summaryDetails>

<#-- @ftlvariable name="summarySections" type="java.util.List<uk.co.nstauthority.licensingmanagementservice.summary.SummarySection>" -->

<#macro summarySections summarySections accordionId>
  <#list summarySections?filter(summarySection -> summarySection.display() == "INLINE") as summarySection>
    <#list summarySection.summaryItems() as summaryItem>
      <#if summaryItem.displayName()?has_content>
        <h2 class="govuk-heading-l">${summaryItem.displayName()}</h2>
      </#if>
      <@summaryDetails.summaryDetails summaryItem=summaryItem/>
    </#list>
  </#list>

  <@fdsAccordion.accordion accordionId="summaryaccordion-${accordionId}">
    <#list summarySections?filter(summarySection -> summarySection.display() == "ACCORDION") as summarySection>
      <#list summarySection.summaryItems() as summaryItem>
        <#assign summaryText>
          <#list summaryItem.tags() as tag>
            <@fdsTag.tag tagClass=tag.tagClass()>${tag.tagText()}</@fdsTag.tag>
          </#list>
        </#assign>
        <@fdsAccordion.accordionSection
          sectionHeading=summaryItem.displayName()
          summaryText=summaryText
          openSection=(summarySection?index == 0 && summaryItem?index == 0)
        >
          <@summaryDetails.summaryDetails summaryItem=summaryItem/>
        </@fdsAccordion.accordionSection>
      </#list>
    </#list>
  </@fdsAccordion.accordion>
</#macro>
