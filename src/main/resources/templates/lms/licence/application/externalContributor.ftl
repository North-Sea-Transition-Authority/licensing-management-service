<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading="External contributors"
pageSize=PageSize.TWO_THIRDS_COLUMN
breadcrumbs=breadcrumbs
errorSummaryItems=errorSummaryItems>

    <@fdsForm.htmlForm>
        <@fdsRadio.radioGroup
        path="form.addExternalContributors"
        labelText="Do you want to add external contributors to help with this application?"
        hintText="Such as joint venture partners, contractors, or lawyers.">
            <@fdsRadio.radioYes path="form.addExternalContributors"/>
            <@fdsRadio.radioNo path="form.addExternalContributors"/>
        </@fdsRadio.radioGroup>

        <@fdsDetails.summaryDetails summaryTitle="What is an external contributor?">
          <p class="govuk-body">An external contributor is someone who needs access to this specific application, but should not see your organisation's other licences or applications.</p>
          <p class="govuk-body">What they can do:</p>
          <ul class="govuk-list govuk-list--bullet">
            <li>View and update this application</li>
            <li>Track its progress and receive notifications</li>
          </ul>
          <p class="govuk-body">What they cannot do:</p>
          <ul class="govuk-list govuk-list--bullet">
            <li>Submit the application</li>
            <li>Access any other data in your organisation</li>
            <li>Access this application after it is completed</li>
          </ul>
          <p class="govuk-body">Common examples include:</p>
          <ul class="govuk-list govuk-list--bullet">
            <li>A representative from another licensee organisation</li>
            <li>An external legal firm or environmental contractor</li>
            <li>An internal colleague who only needs access to this specific project</li>
          </ul>
        </@fdsDetails.summaryDetails>

        <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Back" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>
    </@fdsForm.htmlForm>
</@defaultPage>
