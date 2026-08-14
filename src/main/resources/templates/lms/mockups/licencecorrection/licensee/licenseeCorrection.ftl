<#include '../../../layout/layout.ftl'>

<@defaultPage
  htmlTitle="Change licensees"
  pageHeading="Change licensees"
  backLinkUrl=springUrl(backUrl)
  pageSize=PageSize.TWO_THIRDS_COLUMN
>
  <@fdsForm.htmlForm>
    <@fdsSummaryList.summaryList>
        <@fdsSummaryList.summaryListRowNoAction keyText="Previous licensees">
          BP EXPLORATION (ALPHA) LIMITED
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryList>

    <@fdsFieldset.fieldset legendHeading="Organisations joining" showHeadingOnly=true legendHeadingSize="h2">
      <@fdsAddToList.addToList
        pathForList="form.joiningOrganisationUnitIds"
        pathForSelector="form.joiningOrganisationUnitSelector"
        addToListId="joining-licensees-table"
        restUrl=springUrl(organisationUnitsUrl)
        alreadyAdded=preselectedJoiningOrgUnits
        itemName="Joining licensees"
        selectorLabelText="Select a licensee to add"
      />
      <@fdsDetails.summaryDetails
        summaryTitle="The licensee I want to select is not shown in the list"
      >
        <p class="govuk-body">
          If the licensee you want to select is not shown in the list then you can <@requestNewCompany.requestCompanyLink/>
        </p>
      </@fdsDetails.summaryDetails>
    </@fdsFieldset.fieldset>

    <@fdsFieldset.fieldset legendHeading="Organisations withdrawing" showHeadingOnly=true legendHeadingSize="h2">
      <@fdsAddToList.addToList
        pathForList="form.withdrawingOrganisationUnitIds"
        pathForSelector="form.withdrawingOrganisationUnitSelector"
        addToListId="withdrawing-licensees-table"
        restUrl=springUrl(organisationUnitsUrl)
        alreadyAdded=preselectedWithdrawingOrgUnits
        itemName="Withdrawing licensees"
        selectorLabelText="Select a licensee to remove"
      />
      <@fdsDetails.summaryDetails
      summaryTitle="The licensee I want to select is not shown in the list"
      >
        <p class="govuk-body">
          If the licensee you want to select is not shown in the list then you can <@requestNewCompany.requestCompanyLink/>
        </p>
      </@fdsDetails.summaryDetails>
    </@fdsFieldset.fieldset>

    <@fdsAction.submitButtons
      primaryButtonText="Add"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
