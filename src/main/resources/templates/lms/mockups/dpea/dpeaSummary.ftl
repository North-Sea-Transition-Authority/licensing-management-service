<#include '../../layout/layout.ftl'>
<#import '../../licence/scheduleWorkProgrammeApplication/_applicationContext.ftl' as applicationContextInfo>
<#import '../../component/actions/actionItems.ftl' as actionItems>
<#import '../../licence/scheduleWorkProgrammeApplication/scheduleApplicationSummary.ftl' as scheduleApplicationSummary>

<@defaultPage
htmlTitle="DPEA factual summary mockup"
pageHeading=""
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
>
  <@applicationContextInfo.applicationContextInfo applicationContext=applicationContext/>
  <@actionItems.actionItems actionItems=applicationActions screenReaderText=applicationContext.reference()/>

  <@fdsTabs.tabs tabsHeading="Application summary">
    <@fdsTabs.tabList>
      <@fdsTabs.tab tabLabel="Application" tabAnchor="application" />
      <@fdsTabs.tab tabLabel="DPEA factual summary" tabAnchor="dpea-summary" />
    </@fdsTabs.tabList>

    <@fdsTabs.tabContent tabAnchor="application">
      <@scheduleApplicationSummary.scheduleApplicationSummary accordionId=accordionId summarySections=summarySections/>
    </@fdsTabs.tabContent>

    <@fdsTabs.tabContent tabAnchor="dpea-summary">
      <#if dpeaSummary??>

      <table class="govuk-table">
        <tbody class="govuk-table__body">
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header govuk-!-width-one-third">Document</th>
            <td class="govuk-table__cell">${dpeaSummary.document}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Parties</th>
            <td class="govuk-table__cell">${dpeaSummary.parties}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Licence</th>
            <td class="govuk-table__cell">${dpeaSummary.licence}</td>
          </tr>
        </tbody>
      </table>

      <h3 class="govuk-heading-m">Decision</h3>
      <table class="govuk-table">
        <tbody class="govuk-table__body">
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header govuk-!-width-one-third">Decision</th>
            <td class="govuk-table__cell">${dpeaSummary.decision?no_esc}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Date</th>
            <td class="govuk-table__cell">${dpeaSummary.decisionDate}</td>
          </tr>
        </tbody>
      </table>

      <h3 class="govuk-heading-m">DSP</h3>
      <table class="govuk-table">
        <tbody class="govuk-table__body">
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header govuk-!-width-one-third">Title</th>
            <td class="govuk-table__cell">${dpeaSummary.dspTitle}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Date</th>
            <td class="govuk-table__cell">${dpeaSummary.dspDate}</td>
          </tr>
        </tbody>
      </table>

      <h3 class="govuk-heading-m">Document Preparation Assurance</h3>
      <table class="govuk-table">
        <tbody class="govuk-table__body">
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header govuk-!-width-one-third">Licensee agreement</th>
            <td class="govuk-table__cell">${dpeaSummary.licenseeAgreement}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Legal approval</th>
            <td class="govuk-table__cell">${dpeaSummary.legalApproval}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Ancillary documents required</th>
            <td class="govuk-table__cell">${dpeaSummary.ancillaryDocuments}</td>
          </tr>
        </tbody>
      </table>

      <h3 class="govuk-heading-m">Document Execution Assurance</h3>
      <table class="govuk-table">
        <tbody class="govuk-table__body">
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header govuk-!-width-one-third">RACI consultees</th>
            <td class="govuk-table__cell">${dpeaSummary.raciConsultees?no_esc}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Did all consultees respond?</th>
            <td class="govuk-table__cell">${dpeaSummary.allConsulteesResponded}</td>
          </tr>
          <tr class="govuk-table__row">
            <th scope="row" class="govuk-table__header">Decision maker consistent with Delegation Framework?</th>
            <td class="govuk-table__cell">${dpeaSummary.delegationFrameworkConsistent}</td>
          </tr>
        </tbody>
      </table>

      <#else>
        <p class="govuk-body">DPEA summary data is not available.</p>
      </#if>
    </@fdsTabs.tabContent>
  </@fdsTabs.tabs>
</@defaultPage>
