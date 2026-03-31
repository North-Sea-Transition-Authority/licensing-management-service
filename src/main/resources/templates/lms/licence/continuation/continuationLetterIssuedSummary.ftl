<#-- IN continuationLetterIssuedSummary.ftl -->

<#include '../../layout/layout.ftl'>
<#import '../../summary/_summaryDetails.ftl' as summaryDetails>

<#macro continuationLetterIssuedSummary accordionId summarySection>
    <#if summarySection?has_content>
        <@fdsAccordion.accordion accordionId="summaryaccordion-${accordionId}">
            <#list summarySection.summaryItems() as summaryItem>
                <@fdsAccordion.accordionSection sectionHeading=summaryItem.displayName() openSection=(summaryItem?index == 0)>
                    <@summaryDetails.summaryDetails summaryItem=summaryItem/>
                </@fdsAccordion.accordionSection>
            </#list>
        </@fdsAccordion.accordion>
    </#if>
</#macro>