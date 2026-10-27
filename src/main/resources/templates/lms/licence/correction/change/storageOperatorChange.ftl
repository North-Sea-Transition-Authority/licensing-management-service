<#include '../../../layout/layout.ftl'>
<#import '../../../macros/_requestANewCompany.ftl' as requestCompany>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
backLinkEnabled=true
errorSummaryItems=errorSummaryItems
>
    <@fdsSummaryList.summaryList>
      <@fdsSummaryList.summaryListRowNoAction keyText="Previous storage operator">
        <#if previousStorageOperatorName?has_content>
          ${previousStorageOperatorName}
        <#else>
          None
        </#if>
      </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryList>

    <@fdsForm.htmlForm>
      <@fdsSearchSelector.searchSelectorRest
        path="form.operatorId.inputValue"
        restUrl=springUrl(organisationUnitsUrl)
        labelText="Select a storage operator"
        preselectedItems=preselectedStorageOperator!{}
      />
      <@requestCompany.displayRequest displayName="storage operator"/>
      <@fdsAction.submitButtons
        primaryButtonText="Add"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(cancelUrl)
      />
    </@fdsForm.htmlForm>
</@defaultPage>