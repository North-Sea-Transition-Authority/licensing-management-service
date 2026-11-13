<#include '../../../../layout/layout.ftl'>
<#import '../../../../licence/position/_positionChanges.ftl' as positionChanges>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.TWO_THIRDS_COLUMN
>
  <@fdsForm.htmlForm>
    <@fdsSummaryList.summaryList>
      <#if surrenderDate??>
        <@fdsSummaryList.summaryListRowNoAction keyText="Date of surrender">
          ${surrenderDate}
        </@fdsSummaryList.summaryListRowNoAction>
      </#if>
      <@positionChanges.partialSurrenderBlockRows blockRows=blockRows/>
    </@fdsSummaryList.summaryList>

    <@fdsAction.submitButtons
      primaryButtonText=primaryButton.buttonText()
      primaryButtonClass=primaryButton.buttonClass()
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(cancelUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
