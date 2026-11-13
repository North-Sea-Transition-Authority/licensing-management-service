<#include '../../../../layout/layoutWithGisAssets.ftl'>
<#import '../../../../summary/_summaryDetails.ftl' as summaryDetails>

<@defaultPage
  htmlTitle=pageTitle
  caption=pageCaption
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
  backLinkUrl=springUrl(backLinkUrl)
>
  <@fdsForm.htmlForm>
    <#list summarySections?filter(summarySection -> summarySection.display() == "INLINE") as summarySection>
      <#list summarySection.summaryItems() as summaryItem>
        <h2 class="govuk-heading-l">${summaryItem.displayName()}</h2>
        <@summaryDetails.summaryDetails summaryItem=summaryItem/>
      </#list>
    </#list>

    <@fdsAccordion.accordion accordionId="summaryaccordion-${accordionId}">
      <#list summarySections?filter(summarySection -> summarySection.display() == "ACCORDION") as summarySection>
        <#list summarySection.summaryItems() as summaryItem>
          <#assign summaryText>
            <#if summaryItem.tag()??>
              <@fdsTag.tag tagClass=summaryItem.tag().tagClass()>${summaryItem.tag().tagText()}</@fdsTag.tag>
            </#if>
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

    <@fdsAction.submitButtons
      primaryButtonText="Submit"
      secondaryLinkText=backLinkText
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
