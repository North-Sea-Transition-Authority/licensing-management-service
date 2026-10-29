<#include '../../../layout/layout.ftl'>

<#assign pageTitle>
  ${isCorrectingExistingChange?then("Correct", "Add")} ${operatorType.displayName?uncap_first} change
</#assign>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=operatorType.licenceReference
  backLinkUrl=springUrl(backUrl)
  pageSize=PageSize.TWO_THIRDS_COLUMN
>

  <@fdsSummaryList.summaryList>
    <#if block?has_content>
      <@fdsSummaryList.summaryListRowNoAction keyText="Licence block">
        ${block}
      </@fdsSummaryList.summaryListRowNoAction>
    </#if>
    <#if subarea?has_content>
      <@fdsSummaryList.summaryListRowNoAction keyText="Subarea">
        ${subarea}
      </@fdsSummaryList.summaryListRowNoAction>
    </#if>
    <@fdsSummaryList.summaryListRowNoAction keyText="Previous ${operatorType.displayName?uncap_first}">
      ${previousOperator}
    </@fdsSummaryList.summaryListRowNoAction>
  </@fdsSummaryList.summaryList>

  <@fdsForm.htmlForm>
    <@fdsSearchSelector.searchSelectorRest
      path="form.operatorId"
      restUrl=springUrl(organisationUnitsUrl)
      labelText="Select a ${operatorType.displayName?uncap_first}"
      preselectedItems=preselectedOperator
    />

    <@fdsAction.submitButtons
      primaryButtonText=isCorrectingExistingChange?then("Save and continue", "Add")
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backUrl)
    />
  </@fdsForm.htmlForm>

</@defaultPage>
