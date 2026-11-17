<#include '../../../layout/layout.ftl'>
<#import '../../../macros/_requestANewCompany.ftl' as requestCompany>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
backLinkEnabled=true
errorSummaryItems=errorSummaryItems
>
    <@fdsSummaryList.summaryList>
      <@fdsSummaryList.summaryListRowNoAction keyText="Previous ${operatorType}">
        <#if previousOperatorName?has_content>
          ${previousOperatorName}
        <#else>
          None
        </#if>
      </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryList>

    <#assign article = (operatorType)?lower_case?matches("^[aeiou].*")?then("an", "a")>
    <@fdsForm.htmlForm>
      <@fdsSearchSelector.searchSelectorRest
        path="form.id.inputValue"
        restUrl=springUrl(organisationUnitsUrl)
        labelText="Select ${article} ${operatorType}"
        preselectedItems=preselectedOperator!{}
      />
      <@requestCompany.displayRequest displayName=operatorType/>
      <@fdsAction.submitButtons
        primaryButtonText="Add"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(cancelUrl)
      />
    </@fdsForm.htmlForm>
</@defaultPage>