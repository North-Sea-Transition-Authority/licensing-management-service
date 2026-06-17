<#include '../layout/layout.ftl'>

<#assign pageTitle="Licence positions & transactions"/>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
>

  <@fdsInsetText.insetText>
    <p>This form will create transaction and position information on a production licence or a carbon storage licence. Each licence will have:</p>
    <ul class="govuk-list govuk-list--bullet">
      <li>Six licence positions, all starting before the current date</li>
      <li>Two will start on the same date, and therefore have a different position order and associated transaction</li>
      <li>Two positions will be associated with the same transaction but on different dates</li>
      <li>One will be created for the secondary licence, but for a transaction also used on the original licence</li>
    </ul>
  </@fdsInsetText.insetText>

  <@fdsForm.htmlForm>
    <@fdsSearchSelector.searchSelectorRest
      path="form.licenceId.inputValue"
      restUrl=springUrl(searchUrl)
      labelText="Select a licence"
      preselectedItems=preSelectedLicence
    />

    <@fdsSearchSelector.searchSelectorRest
      path="form.secondaryLicenceId.inputValue"
      restUrl=springUrl(searchUrl)
      labelText="Select a second licence"
      preselectedItems=preSelectedSecondaryLicence
    />

    <@fdsAction.submitButtons
      primaryButtonText="Create"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(cancelUrl)
    />
  </@fdsForm.htmlForm>

</@defaultPage>