<#include '../layout/layout.ftl'>

<#assign pageTitle="Licence positions & transactions"/>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
>

  <@fdsInsetText.insetText>
    <p>This form will clear any existing positions on two licences (production or carbon storage), then create
      transaction and position information on them. The primary licence will have:</p>
    <ul class="govuk-list govuk-list--bullet">
      <li>Ten licence positions, all starting before the current date</li>
      <li>Five will start on one date and two on a later date, so each has a different position order and transaction</li>
      <li>Two positions will be associated with the same transaction but on different dates</li>
      <li>One will be created for the secondary licence, but for a transaction also used on the original licence</li>
      <li>
        A licensee change on the first position adding BP and Flamstone, and another on the seventh position adding
        Shell and removing BP
      </li>
    </ul>
    <p>Both licences will also be given blocks on their first position. A production licence has four blocks, the
      second divided into three subareas and the others with one subarea each. A carbon storage licence has one block
      with no subareas.</p>
    <p>If a production licence is selected:</p>
    <ul class="govuk-list govuk-list--bullet">
      <li>
        An administrator position change will be generated for:
        <ul class="govuk-list govuk-list--bullet">
          <li>The first position</li>
          <li>Another licence position other than the final position</li>
        </ul>
      </li>
      <li>The third position will split the fourth block's subarea into a shallow and a deep stratum at 3000m</li>
      <li>The sixth position will split the third block's subarea in half</li>
      <li>
        The penultimate position will surrender two blocks: the first fully, and the second partially by cutting
        it in half and surrendering the western half. Of the second block's subareas, the one in the western half is
        relinquished, the one across the cut is cropped back to the eastern half and the one in the eastern half is
        kept as it is
      </li>
      <li>
        The final position will merge the subareas left on the second block's eastern half, and set beneficial
        interests in the first subarea the split created (Shell 60%, BP 40%)
      </li>
    </ul>
    <p>If a carbon storage licence is selected, position changes will be generated for:</p>
    <ul class="govuk-list govuk-list--bullet">
      <li>The first position, setting the initial equity holdings (Shell 60%, BP 40%, PPRS 0%) and Shell as storage operator</li>
      <li>
        Another licence position other than the final position, transferring equity from Shell
        (10% to BP and 15% to PPRS) and making BP the storage operator
      </li>
    </ul>
    <p>If the second licence is a production licence, an administrator position change will be generated for its first
      position. If it is a carbon storage licence, a storage operator change will be generated instead.</p>
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