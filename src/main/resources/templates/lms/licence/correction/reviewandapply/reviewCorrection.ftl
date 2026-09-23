<#include '../../../layout/layout.ftl'>
<#import '../_correctionDetailsCard.ftl' as correctionDetailsCard>
<#import '../../position/_positionChanges.ftl' as positionChanges>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  pageSize=PageSize.FULL_COLUMN
  backLinkUrl=springUrl(backLinkUrl)
  errorSummaryItems=errorSummaryItems
>
  <@correctionDetailsCard.correctionDetailsCard
    correction=correction
    allocatedToUser=allocatedToUser
    createdDate=createdDate
  />

  <h2 class="govuk-heading-l">Corrected positions</h2>

  <#if positions?has_content>
    <@fdsAccordion.accordion accordionId="corrected-positions-${correction.getId()}">
      <#list positions?reverse as position>
        <#assign marker = position.marker()!''>
        <#assign sectionHeading>
          <span id="${position.positionId()}" tabindex="-1">${position.positionName()}</span>
          <#if marker?has_content>
            <@fdsTag.tag tagClass=marker.tagClass>${marker.label}</@fdsTag.tag>
          </#if>
        </#assign>
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
</@defaultPage>
