<#include '../../layout/layout.ftl'>

<#macro mailMergeFieldSummaryDetails mailMergeFieldViews>
  <@fdsDetails.summaryDetails summaryTitle="How can I include application data in my section?">
    <h2 class="govuk-heading-m">Mail merge fields</h2>

    <p class="govuk-body">
      Any of the fields listed below can be included in section text to pull in information from the application.
      These will be visible on a document.
    </p>

    <table class="govuk-table">
      <thead class="govuk-table__head">
        <tr class="govuk-table__row">
          <th class="govuk-table__header" scope="col">Mail merge field</th>
          <th class="govuk-table__header" scope="col">Description</th>
        </tr>
      </thead>
      <tbody class="govuk-table__body">
        <#list mailMergeFieldViews as mailMergeFieldView>
          <tr class="govuk-table__row">
            <td class="govuk-table__cell">((${mailMergeFieldView.mnemonic()}))</td>
            <td class="govuk-table__cell">${mailMergeFieldView.description()}</td>
          </tr>
        </#list>
      </tbody>
    </table>

    <h2 class="govuk-heading-m">What does '??' mean?</h2>

    <p class="govuk-body">
      Phrases starting and ending with '??' indicate that an edit needs to be made to remove the text that does not
      apply to this document.
    </p>

    <h2 class="govuk-heading-m">What does '[[]]' mean?</h2>

    <p class="govuk-body">
      Phrases starting with `[[` and ending with ']]' indicate that the phrase will be a footnote.
    </p>
  </@fdsDetails.summaryDetails>
</#macro>
