<#include '../../../../layout/layout.ftl'>
<#import '../../../../summary/_summaryDetails.ftl' as summaryDetails>

<@defaultPage
  htmlTitle=pageTitle
  caption=pageCaption
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
  backLinkUrl=springUrl(backLinkUrl)
>
  <#if allSurrenderedBlocksAreFull>
    <@fdsError.singleErrorSummary errorMessage="At least one block must be marked as a partial surrender"/>
  </#if>

  <@fdsForm.htmlForm>
    <#list summarySections as summarySection>
      <#list summarySection.summaryItems() as summaryItem>
        <@fdsSummaryList.summaryListWrapper
          headingText=summaryItem.displayName()
          headingSize="h2"
          summaryListId="summary-${summarySection?index}-${summaryItem?index}"
        >
          <@summaryDetails.summaryDetails summaryItem=summaryItem/>
        </@fdsSummaryList.summaryListWrapper>
      </#list>
    </#list>

    <@fdsAction.submitButtons
      primaryButtonText="Submit"
      <#--TODO: enable when we can submit partial surrenders-->
      primaryDisabledButton=true
      secondaryLinkText="Back to task list"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
