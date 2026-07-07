<#include '../../layout/layout.ftl'>
<@defaultPage htmlTitle=pageTitle pageHeading="" pageSize=PageSize.TWO_THIRDS_COLUMN breadcrumbs=breadcrumbs errorSummaryItems=errorSummaryItems>

    <@fdsForm.htmlForm>
        <@fdsRadio.radioGroup path="form.selectedWorkProgrammeActivityAmendmentId" labelText=pageTitle hiddenContent=false fieldsetHeadingSize="h1"
        fieldsetHeadingClass="govuk-fieldset__legend--l"
        noFieldsetHeadingSize="--l">
            <#list workProgrammeAmendmentViews as workProgrammeAmendmentView>
                <@fdsRadio.radioItem
                path="form.selectedWorkProgrammeActivityAmendmentId"
                itemMap={workProgrammeAmendmentView.id() : workProgrammeAmendmentView.categoryWithDueDate() }
                itemHintText=workProgrammeAmendmentView.description()>
                </@fdsRadio.radioItem>
            </#list>
        </@fdsRadio.radioGroup>
      <br>
        <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>
    </@fdsForm.htmlForm>

</@defaultPage>