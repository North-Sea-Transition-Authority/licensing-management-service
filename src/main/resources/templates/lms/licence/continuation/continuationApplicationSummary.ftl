<#include '../../layout/layout.ftl'>
<#import '../../summary/_summaryDetails.ftl' as summaryDetails>
<#import 'summaryCardList/licenceContinuationWpaRequirementSummaryCard.ftl' as licenceContinuationWpaRequirementCard>

<#macro continuationApplicationSummary accordionId summarySections showWorkProgrammeActivities=false workProgrammeActivities=[]>
  <@fdsAccordion.accordion accordionId="summaryaccordion-${accordionId}">
    <#list summarySections as summarySection>
      <#list summarySection.summaryItems() as summaryItem>
        <@fdsAccordion.accordionSection sectionHeading=summaryItem.displayName() openSection=(summarySection?index == 0 && summaryItem?index == 0)>
            <#if showWorkProgrammeActivities && summaryItem.displayName() == "Continuation requirement">
                <#list workProgrammeActivities as workProgrammeActivity>
                    <@licenceContinuationWpaRequirementCard.workProgrammeActivities workProgrammeActivity=workProgrammeActivity/>
                </#list>
            </#if>
          <@summaryDetails.summaryDetails summaryItem=summaryItem/>
        </@fdsAccordion.accordionSection>
      </#list>
    </#list>
  </@fdsAccordion.accordion>
</#macro>