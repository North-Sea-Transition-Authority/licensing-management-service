<#include '../../../layout/layout.ftl'>
<#import '../../position/_positionChanges.ftl' as positionChanges>

<#macro correctedPositionsAccordion correctionId positions>
  <#if positions?has_content>
    <@fdsAccordion.accordion accordionId="corrected-positions-${correctionId}">
      <#list positions?reverse as position>
        <#local marker = position.marker()!''>
        <#local sectionHeading>
          <span id="${position.positionId()}" tabindex="-1">${position.positionName()}</span>
          <#if marker?has_content>
            <@fdsTag.tag tagClass=marker.tagClass>${marker.label}</@fdsTag.tag>
          </#if>
        </#local>
        <@fdsAccordion.accordionSection
          sectionHeading=sectionHeading
          sectionHeadingSize="h3"
          summaryText=position.reference()
        >
          <#list position.changes()?reverse as reviewChange>
            <@positionChanges.changeCard
              change=reviewChange.change()
              summaryListId="${position.positionId()}-${reviewChange?index}"
              correction=reviewChange
            />
          </#list>
        </@fdsAccordion.accordionSection>
      </#list>
    </@fdsAccordion.accordion>
  <#else>
    <@fdsInsetText.insetText>No changes have been made on this correction.</@fdsInsetText.insetText>
  </#if>
</#macro>
