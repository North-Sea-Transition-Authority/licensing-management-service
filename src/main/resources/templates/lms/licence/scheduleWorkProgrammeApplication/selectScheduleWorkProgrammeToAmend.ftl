<#include '../../layout/layout.ftl'>
<@defaultPage htmlTitle=pageTitle pageHeading="" pageSize=PageSize.TWO_THIRDS_COLUMN errorSummaryItems=errorSummaryItems>

    <@fdsForm.htmlForm>
        <@fdsRadio.radioGroup path="form.selectedWorkProgrammeActivityAmendmentId" labelText=pageTitle hiddenContent=false fieldsetHeadingSize="h1"
        fieldsetHeadingClass="govuk-fieldset__legend--l"
        noFieldsetHeadingSize="--l">
            <#list workProgrammeAmendments as wp>
                <@fdsRadio.radioItem path="form.selectedWorkProgrammeActivityAmendmentId"  itemMap={wp.id : wp.label} itemHintText = wp.description >
                </@fdsRadio.radioItem>
            </#list>
        </@fdsRadio.radioGroup>
      <br>
        <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>
    </@fdsForm.htmlForm>

</@defaultPage>