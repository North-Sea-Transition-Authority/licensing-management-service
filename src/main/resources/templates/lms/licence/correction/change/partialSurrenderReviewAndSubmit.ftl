<#include '../../../layout/layout.ftl'>
<#import '../../../summary/_summaryDetails.ftl' as summaryDetails>

<@defaultPage
  htmlTitle=pageTitle
  caption=pageCaption
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
  backLinkUrl=springUrl(backLinkUrl)
>
  <@fdsForm.htmlForm>
    <@fdsAccordion.accordion accordionId="summaryaccordion-${accordionId}">
      <#list summarySections as summarySection>
        <#list summarySection.summaryItems() as summaryItem>
          <@fdsAccordion.accordionSection
            sectionHeading=summaryItem.displayName()
            openSection=(summarySection?index == 0 && summaryItem?index == 0)
          >
            <@summaryDetails.summaryDetails summaryItem=summaryItem/>
          </@fdsAccordion.accordionSection>
        </#list>
      </#list>
    </@fdsAccordion.accordion>

    <@fdsAction.submitButtons
      primaryButtonText="Submit"
      primaryDisabledButton=true
      secondaryLinkText="Back to task list"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>