<#include '../../../layout/layout.ftl'>
<#import '../../../macros/_licenseesDisplay.ftl' as licenseesDisplay>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  backLinkEnabled=true
  errorSummaryItems=errorSummaryItems
>
    <#if previousLicenseeNames?has_content>
      <@licenseesDisplay.displayLicensees previousLicenseeNames/>
    </#if>

    <@fdsForm.htmlForm>
      <h3 class="govuk-heading-m" aria-label="Organisations joining">Organisations joining</h3>
      <@fdsAddToList.addToList
        pathForList="form.joiningOrganisationIds"
        pathForSelector="form.joiningOrganisationSelector"
        restUrl=springUrl(joiningOrganisationUnitSearchEndpoint)
        alreadyAdded=preselectedJoiningOrgUnits
        itemName="Joining licensees"
        selectorOptionaLabel=true
        selectorLabelText="Select a licensee to add"
        addToListId="joiningIds"
      />

      <@fdsDetails.summaryDetails
        summaryTitle="The licensee I want to select is not in the list"
      >
        <p class="govuk-body">
          If the licensee you want to select is not shown in the list then you can <@requestNewCompany.requestCompanyLink/>
        </p>
      </@fdsDetails.summaryDetails>

      <#if previousLicenseeNames?has_content>
        <h3 class="govuk-heading-m" aria-label="Organisations withdrawing">Organisations withdrawing</h3>
        <@fdsAddToList.addToList
          pathForList="form.withdrawingOrganisationIds"
          pathForSelector="form.withdrawingOrganisationSelector"
          restUrl=springUrl(withdrawingOrganisationUnitSearchEndpoint)
          alreadyAdded=preselectedWithdrawingOrgUnits
          itemName="Withdrawing licensees"
          selectorOptionaLabel=true
          selectorLabelText="Select a licensee to remove"
          addToListId="withdrawingIds"
          selectorHintText="You can only withdraw licensees that are already on the licence"
        />
      </#if>

      <@fdsAction.submitButtons
        primaryButtonText="Add"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(cancelUrl)
      />
    </@fdsForm.htmlForm>
</@defaultPage>