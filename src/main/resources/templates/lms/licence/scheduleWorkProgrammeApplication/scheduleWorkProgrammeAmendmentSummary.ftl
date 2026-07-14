<#include '../../layout/layout.ftl'>
<#import 'summarylistcard/scheduleLicenceWorkProgrammeAmendmentSummaryCard.ftl' as licenceWorkProgrammeAmendmentCard>


<#assign pageTitle = "Work programme amendments" />

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN breadcrumbs=breadcrumbs errorSummaryItems=errorSummaryItems>

    <@fdsForm.htmlForm>
        <#list licenceWorkProgrammeAmendments as licenceWorkProgrammeAmendment>
            <@licenceWorkProgrammeAmendmentCard.licenceWorkProgrammeAmendments licenceWorkProgrammeAmendment=licenceWorkProgrammeAmendment/>
        </#list>

        <@fdsRadio.radioGroup
        path="form.licenceWorkProgrammeAmendmentSummaryOptions"
        fieldsetHeadingClass="govuk-fieldset__legend--l"
        labelText="Do you want to add another work programme amendment to this application?"
        hiddenContent=true>
            <#assign firstItem=true/>
            <#list licenceWorkProgrammeAmendmentSummaryOptions as key, value>
                <@fdsRadio.radioItem path="form.licenceWorkProgrammeAmendmentSummaryOptions" itemMap={key : value} isFirstItem=firstItem/>
                <#assign firstItem=false/>
            </#list>
        </@fdsRadio.radioGroup>

        <@fdsAction.button buttonText="Save and continue"/>
    </@fdsForm.htmlForm>
</@defaultPage>