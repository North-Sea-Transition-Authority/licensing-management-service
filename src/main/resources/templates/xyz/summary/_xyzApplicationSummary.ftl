<#include '../layout/layout.ftl'>
<#import '_summaryDetails.ftl' as summaryDetails>
<#import '../../fds/utilities/utilities.ftl' as fdsUtil>

<#-- @ftlvariable name="summarySections" type="java.util.List<uk.co.nstauthority.template.summarySummarySection>" -->

<#macro applicationSummary accordionId summarySections>
  <@fdsAccordion.accordion accordionId="summaryaccordion-${accordionId}">
    <#list summarySections as summarySection>
      <#list summarySection.summaryItems() as summaryItem>
        <@fdsAccordion.accordionSection sectionHeading=summaryItem.displayName() openSection=(summarySection?index == 0 && summaryItem?index == 0)>
          <@summaryDetails.summaryDetails summaryItem=summaryItem/>
        </@fdsAccordion.accordionSection>
      </#list>
    </#list>
  </@fdsAccordion.accordion>
</#macro>
