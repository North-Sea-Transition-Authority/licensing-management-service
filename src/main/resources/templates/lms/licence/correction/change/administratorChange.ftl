<#include '../../../layout/layout.ftl'>

<@defaultPage
htmlTitle="Change licence administrator"
backLinkUrl=springUrl(backLinkUrl)
>
    <@fdsSummaryList.summaryList>
        <@fdsSummaryList.summaryListRowNoAction keyText="Previous licence administrator">
            <#if previousLicenceAdministratorName?has_content>
                ${previousLicenceAdministratorName}
            <#else>
              None
            </#if>
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryList>

    <@fdsForm.htmlForm>
      <@fdsSearchSelector.searchSelectorRest
        path="form.adminId.inputValue"
        restUrl=springUrl(organisationUnitsUrl)
        labelText="Select a licence administrator"
        preselectedItems=preselectedAdministrator!{}
      />
      <@fdsDetails.summaryDetails
        summaryTitle="The licence administrator I want to select is not in the list"
      >
        <p class="govuk-body">
          If the licence administrator you want to select is not shown in the list then you can <@requestNewCompany.requestCompanyLink/>
        </p>
      </@fdsDetails.summaryDetails>
      <@fdsAction.submitButtons
        primaryButtonText="Add"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(backLinkUrl)
      />
    </@fdsForm.htmlForm>
</@defaultPage>