<#include '../../layout/layout.ftl'>
<#import '../../licence/scheduleWorkProgrammeApplication/_applicationContext.ftl' as applicationContextInfo>
<#import '../../component/actions/actionItems.ftl' as actionItems>
<#import '../../licence/scheduleWorkProgrammeApplication/scheduleApplicationSummary.ftl' as scheduleApplicationSummary>

<@defaultPage
htmlTitle="Application summary mockup"
pageHeading=""
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
>
  <@applicationContextInfo.applicationContextInfo applicationContext=applicationContext/>
  <@actionItems.actionItems actionItems=applicationActions screenReaderText=applicationContext.reference()/>

  <@fdsTabs.tabs tabsHeading="Application summary">
    <@fdsTabs.tabList>
      <@fdsTabs.tab tabLabel="Application" tabAnchor="application" />
      <@fdsTabs.tab tabLabel="Factual Summary" tabAnchor="factual-summary" />
    </@fdsTabs.tabList>

    <@fdsTabs.tabContent tabAnchor="application">
      <@scheduleApplicationSummary.scheduleApplicationSummary accordionId=accordionId summarySections=summarySections/>
    </@fdsTabs.tabContent>

    <@fdsTabs.tabContent tabAnchor="factual-summary">
      <#if factualSummary??>

      <h3 class="govuk-heading-m">Licence Details</h3>
      <table class="govuk-table">
        <tbody class="govuk-table__body">
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header govuk-!-width-one-third">Licence Number</th>
            <td class="govuk-table__cell">${factualSummary.licenceNumber}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Round</th>
            <td class="govuk-table__cell">${factualSummary.round}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Block(s)</th>
            <td class="govuk-table__cell">${factualSummary.blocks}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Field/discovery name(s)</th>
            <td class="govuk-table__cell">${factualSummary.fieldDiscovery}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Licence start date</th>
            <td class="govuk-table__cell">
              ${factualSummary.licenceStartDate}
            </td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Current phase/term expiry date</th>
            <td class="govuk-table__cell">
              ${factualSummary.expiryDate}
            </td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Phase & Term duration</th>
            <td class="govuk-table__cell">
              ${factualSummary.phaseTermDuration?no_esc}
            </td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Licensee(s) (the 'Licensees')</th>
            <td class="govuk-table__cell">${factualSummary.licensees}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Licence administrator/operator</th>
            <td class="govuk-table__cell">${factualSummary.operator}</td>
          </tr>
          <#if factualSummary.workProgramme??>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Work Programme</th>
            <td class="govuk-table__cell">${factualSummary.workProgramme?no_esc}</td>
          </tr>
          </#if>
        </tbody>
      </table>

      <h3 class="govuk-heading-m">Request</h3>
      <table class="govuk-table">
        <tbody class="govuk-table__body">
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header govuk-!-width-one-third">Date of request</th>
            <td class="govuk-table__cell">${factualSummary.requestDate}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Was request made on behalf of all Licensees?</th>
            <td class="govuk-table__cell">${factualSummary.onBehalfOfAll}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Period of extension requested</th>
            <td class="govuk-table__cell">${factualSummary.extensionPeriod}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Request summary</th>
            <td class="govuk-table__cell">${factualSummary.requestSummary}</td>
          </tr>
        </tbody>
      </table>
      <#else>
        <p class="govuk-body">Factual summary data is not available.</p>
      </#if>
    </@fdsTabs.tabContent>
  </@fdsTabs.tabs>
</@defaultPage>
