<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
>
    <#if setEquityViews?size == 0>
        <@fdsSummaryList.summaryListCard headingText="Organisations" summaryListId="set-organisations">
          <p class="govuk-body">No organisations added</p>
            <@fdsAction.link linkText="Add organisation" linkUrl=springUrl(addOrganisationUrl) linkClass="govuk-button govuk-button--secondary govuk-!-margin-top-4"/>
        </@fdsSummaryList.summaryListCard>
    <#else>
        <#list setEquityViews as view>
            <@fdsSummaryList.summaryListCard headingText=view.organisationName() summaryListId="set-organisations-${view?index}">
              <table class="govuk-table govuk-!-margin-bottom-0">
                <thead class="govuk-table__head">
                <tr class="govuk-table__row">
                  <th scope="col" class="govuk-table__header">Organisation</th>
                  <th scope="col" class="govuk-table__header govuk-table__header--numeric">Equity</th>
                  <th scope="col" class="govuk-table__header"><span class="govuk-visually-hidden">Actions</span></th>
                </tr>
                </thead>
                <tbody class="govuk-table__body">
                <tr class="govuk-table__row">
                  <td class="govuk-table__cell">${view.organisationName()}</td>
                  <td class="govuk-table__cell govuk-table__cell--numeric">${view.equity()}%</td>
                  <td class="govuk-table__cell govuk-table__cell--numeric">
                    <@fdsForm.htmlForm actionUrl=springUrl(removeUrls[view?index])>
                      <@fdsAction.button buttonText="Remove" buttonClass="fds-link-button"/>
                    </@fdsForm.htmlForm>
                  </td>
                </tr>
                </tbody>
              </table>
            </@fdsSummaryList.summaryListCard>
        </#list>

      <div class="govuk-!-margin-top-4 govuk-!-margin-bottom-6">
          <@fdsAction.link linkText="Add organisation" linkUrl=springUrl(addOrganisationUrl) linkClass="govuk-button govuk-button--secondary"/>
      </div>

      <div class="govuk-!-margin-bottom-6">
        <p class="govuk-body govuk-!-font-size-24 govuk-!-font-weight-bold">
          Total beneficial interest: ${totalEquity}%
        </p>
      </div>
    </#if>

    <@fdsForm.htmlForm actionUrl=springUrl(saveAndContinueUrl)>
        <@fdsAction.button buttonText="Save and continue"/>
    </@fdsForm.htmlForm>

</@defaultPage>