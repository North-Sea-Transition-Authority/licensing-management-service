<#include '../../../layout/layout.ftl'>

<@defaultPage
  htmlTitle="Change licence administrator"
    backLinkUrl=springUrl(backUrl)
>

  <@fdsSummaryList.summaryList>
    <@fdsSummaryList.summaryListRowNoAction keyText="Previous licence administrator">
      BP EXPLORATION (ALPHA) LIMITED (01021007)
    </@fdsSummaryList.summaryListRowNoAction>
  </@fdsSummaryList.summaryList>

  <@fdsSearchSelector.searchSelectorRest
    path="form.adminId"
    restUrl=springUrl(organisationUnitsUrl)
    labelText="Select a licence administrator"
  />
  <@fdsDetails.summaryDetails
    summaryTitle="The licence administrator I want to select is not in the list"
  >
    <p class="govuk-body">
      If the licence administrator you want to select is not shown in the list then you can <@requestNewCompany.requestCompanyLink/>
    </p>
  </@fdsDetails.summaryDetails>
    <@fdsAction.submitButtons
      primaryButtonText="Save and continue"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backUrl)
    />
</@defaultPage>