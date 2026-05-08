<#include '../../layout/layout.ftl'>
<#import '../../summary/_summaryDetails.ftl' as summaryDetails>
<#import 'summaryCardList/licenceContinuationWpaRequirementSummaryCard.ftl' as licenceContinuationWpaRequirementCard>

<#macro continuationApplicationSummary accordionId summarySections workProgrammeActivities=[] isReviewer=false>
  <@fdsAccordion.accordion accordionId="summaryaccordion-${accordionId}">
    <#list summarySections as summarySection>
      <#list summarySection.summaryItems() as summaryItem>
        <@fdsAccordion.accordionSection sectionHeading=summaryItem.displayName() openSection=(summarySection?index == 0 && summaryItem?index == 0)>
            <#if summaryItem.displayName() == "Work programme activities requirement">
                <#list workProgrammeActivities as workProgrammeActivity>
                    <@licenceContinuationWpaRequirementCard.workProgrammeActivities workProgrammeActivity=workProgrammeActivity isReviewer=isReviewer/>
                </#list>
            </#if>
          <@summaryDetails.summaryDetails summaryItem=summaryItem/>
        </@fdsAccordion.accordionSection>
      </#list>
    </#list>
  </@fdsAccordion.accordion>
</#macro>