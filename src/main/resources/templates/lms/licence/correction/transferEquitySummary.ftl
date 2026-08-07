<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
>
    <#if transferEquityViews?size == 0>
        <@fdsSummaryList.summaryListCard headingText="Transfers" summaryListId="transfers">
          <p class="govuk-body">No transfers added</p>
            <@fdsAction.link linkText="Add transfer" linkUrl=springUrl(addTransferUrl) linkClass="govuk-button govuk-button--secondary govuk-!-margin-top-4"/>
        </@fdsSummaryList.summaryListCard>
    <#else>
        <#list transferEquityViews as view>
            <#assign withdrawingContext = "whether ${view.transferFromOrganisationName()} is withdrawing for the equity transfer to ${view.transferToOrganisationName()}"/>
            <#assign transferContext = "the equity transfer from ${view.transferFromOrganisationName()} to ${view.transferToOrganisationName()}"/>
            <@fdsSummaryList.summaryListCard headingText="Transfer ${view?index + 1}" summaryListId="transfer-${view?index}">
              <table class="govuk-table govuk-!-margin-bottom-0">
                <thead class="govuk-table__head">
                <tr class="govuk-table__row">
                  <th scope="col" class="govuk-table__header">From</th>
                  <th scope="col" class="govuk-table__header">To</th>
                  <th scope="col" class="govuk-table__header govuk-table__header--numeric">Equity</th>
                  <th scope="col" class="govuk-table__header">Withdrawing?</th>
                  <th scope="col" class="govuk-table__header"><span class="govuk-visually-hidden">Actions</span></th>
                </tr>
                </thead>
                <tbody class="govuk-table__body">
                <tr class="govuk-table__row">
                  <td class="govuk-table__cell">${view.transferFromOrganisationName()}</td>
                  <td class="govuk-table__cell">${view.transferToOrganisationName()}</td>
                  <td class="govuk-table__cell govuk-table__cell--numeric">${view.equity()}%</td>
                  <td class="govuk-table__cell">
                      <#if !withdrawApplicable[view?index]>
                        N/A
                      <#elseif !(view.retainBeneficialInterest()??)>
                        Not answered
                      <#else>
                          <#if view.retainBeneficialInterest()>No<#else>Yes</#if>
                      </#if>
                  </td>
                  <td class="govuk-table__cell govuk-table__cell--numeric">
                      <#if withdrawApplicable[view?index]>
                          <#if !(view.retainBeneficialInterest()??)>
                            <div class="govuk-!-margin-bottom-2">
                                <@fdsAction.link linkText="Answer" linkUrl=springUrl(withdrawUrls[view?index]) linkScreenReaderText=withdrawingContext linkClass="govuk-link govuk-link--no-visited-state"/>
                            </div>
                          <#else>
                            <div class="govuk-!-margin-bottom-2">
                                <@fdsAction.link linkText="Change" linkUrl=springUrl(withdrawUrls[view?index]) linkScreenReaderText=withdrawingContext linkClass="govuk-link govuk-link--no-visited-state"/>
                            </div>
                          </#if>
                      </#if>
                      <@fdsForm.htmlForm actionUrl=springUrl(removeUrls[view?index])>
                        <button type="submit" class="fds-link-button">Remove<span class="govuk-visually-hidden"> ${transferContext}</span></button>
                      </@fdsForm.htmlForm>
                  </td>
                </tr>
                </tbody>
              </table>
            </@fdsSummaryList.summaryListCard>
        </#list>

      <div class="govuk-!-margin-top-4 govuk-!-margin-bottom-6">
          <@fdsAction.link linkText="Add transfer" linkUrl=springUrl(addTransferUrl) linkClass="govuk-button govuk-button--secondary"/>
      </div>
    </#if>

    <@fdsAction.link linkText="Save and continue" linkUrl=springUrl(saveAndContinueUrl) linkClass="govuk-button"/>

</@defaultPage>