<#include '../layout/layout.ftl'>

<#assign pageTitle="Licence positions & transactions"/>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
>

  <@fdsInsetText.insetText>
    <p>This form will create transaction and position information on two licences (production or carbon storage). The primary licence will have:</p>
    <ul class="govuk-list govuk-list--bullet">
      <li>Six licence positions, all starting before the current date</li>
      <li>Two will start on the same date, and therefore have a different position order and associated transaction</li>
      <li>Two positions will be associated with the same transaction but on different dates</li>
      <li>One will be created for the secondary licence, but for a transaction also used on the original licence</li>
    </ul>
    <p>If a production licence is selected:</p>
    <ul class="govuk-list govuk-list--bullet">
      <li>
        An administrator position change will be generated for:
        <ul class="govuk-list govuk-list--bullet">
          <li>The first position</li>
          <li>Another licence position other than the final position</li>
        </ul>
      </li>
    </ul>
    <p>If a carbon storage licence is selected, beneficial interest position changes will be generated for:</p>
    <ul class="govuk-list govuk-list--bullet">
      <li>The first position, setting the initial equity holdings (Shell 60%, BP 40%, PPRS 0%)</li>
      <li>
        Another licence position other than the final position, transferring equity from Shell
        (10% to BP and 15% to PPRS)
      </li>
    </ul>
    <p>If the second licence is a production licence, an administrator position change will be generated for its first position.</p>
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