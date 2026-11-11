<#include '../layout/layout.ftl'>

<#assign pageTitle="Licence positions & transactions"/>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
>

  <p class="govuk-body">This form will clear any existing positions on two licences (production or carbon storage), then create the
    positions below on them, newest first. Each position is numbered by its place in the licence's timeline.</p>

  <@fdsDetails.summaryDetails summaryTitle="If the first licence is a production licence">
    <ol class="govuk-list govuk-list--number" reversed>
      <li>
        2 weeks ago, same transaction as the second licence's position
        <ul class="govuk-list govuk-list--bullet">
          <li>Subareas B and C on block 2's eastern half are merged into one subarea</li>
          <li>BP 100% in the merged subarea</li>
        </ul>
      </li>
      <li>
        3 weeks ago, same transaction as position 6
        <ul class="govuk-list govuk-list--bullet">
          <li>Block 1 is fully surrendered</li>
          <li>
            Block 2 is cut in half and its western half surrendered: subarea A is relinquished, B is cropped back to the
            eastern half and C is kept
          </li>
          <li>On block 2's eastern half: BP 100% in B and Flamstone 100% in C</li>
        </ul>
      </li>
      <li>
        4 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>Administrator: BP</li>
        </ul>
      </li>
      <li>
        4 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>Licensees: Shell joins, BP leaves</li>
        </ul>
      </li>
      <li>
        5 weeks ago, same transaction as position 9
        <ul class="govuk-list govuk-list--bullet">
          <li>Block 3's subarea is split in half, into A (west) and B (east)</li>
          <li>Shell 100% in both halves</li>
        </ul>
      </li>
      <li>
        7 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>No changes</li>
        </ul>
      </li>
      <li>
        7 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>No changes</li>
        </ul>
      </li>
      <li>
        7 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>Block 4's subarea is split into strata above and below 3000m</li>
          <li>Shell 100% in both strata</li>
        </ul>
      </li>
      <li>
        7 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>No changes</li>
        </ul>
      </li>
      <li>
        7 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>Administrator: Shell</li>
          <li>Licensees: BP and Flamstone join</li>
          <li>
            Four blocks are created. Blocks 1, 3 and 4 have one subarea covering the whole block, and block 2 is divided
            into subareas A, B and C from west to east
          </li>
          <li>
            Subarea beneficial interests: Shell 100% in the subareas of blocks 1, 3 and 4, and on block 2 Shell 100% in
            A, BP 100% in B and Flamstone 100% in C
          </li>
        </ul>
      </li>
    </ol>
  </@fdsDetails.summaryDetails>

  <@fdsDetails.summaryDetails summaryTitle="If the first licence is a carbon storage licence">
    <ol class="govuk-list govuk-list--number" reversed>
      <li>
        2 weeks ago, same transaction as the second licence's position
        <ul class="govuk-list govuk-list--bullet">
          <li>No changes</li>
        </ul>
      </li>
      <li>
        3 weeks ago, same transaction as position 6
        <ul class="govuk-list govuk-list--bullet">
          <li>No changes</li>
        </ul>
      </li>
      <li>
        4 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>Beneficial interests: Shell transfers 10% to BP and 15% to PPRS</li>
          <li>Storage operator: BP</li>
        </ul>
      </li>
      <li>
        4 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>Licensees: Shell joins, BP leaves</li>
        </ul>
      </li>
      <li>
        5 weeks ago, same transaction as position 9
        <ul class="govuk-list govuk-list--bullet">
          <li>No changes</li>
        </ul>
      </li>
      <li>
        7 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>No changes</li>
        </ul>
      </li>
      <li>
        7 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>No changes</li>
        </ul>
      </li>
      <li>
        7 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>No changes</li>
        </ul>
      </li>
      <li>
        7 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>No changes</li>
        </ul>
      </li>
      <li>
        7 weeks ago
        <ul class="govuk-list govuk-list--bullet">
          <li>Beneficial interests: Shell 60%, BP 40%, PPRS 0%</li>
          <li>Storage operator: Shell</li>
          <li>Licensees: BP and Flamstone join</li>
          <li>One block is created with no subareas</li>
        </ul>
      </li>
    </ol>
  </@fdsDetails.summaryDetails>

  <@fdsDetails.summaryDetails summaryTitle="The second licence">
    <ol class="govuk-list govuk-list--number">
      <li>
        1 week ago, same transaction as the first licence's position 10
        <ul class="govuk-list govuk-list--bullet">
          <li>
            If a production licence: administrator Shell, and the same blocks and subarea beneficial interests as a
            production first licence's position 1
          </li>
          <li>If a carbon storage licence: storage operator Shell, and one block with no subareas</li>
        </ul>
      </li>
    </ol>
  </@fdsDetails.summaryDetails>

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