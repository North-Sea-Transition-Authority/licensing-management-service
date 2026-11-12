<#include '../../layout/layout.ftl'>
<#import 'summarylistcard/recordWorkProgrammeAmendmentSummaryCard.ftl' as recordWorkProgrammeAmendmentSummaryCard>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
breadcrumbs=breadcrumbs>
    <@fdsForm.htmlForm>

        <#list workProgrammeAmendments as workProgrammeAmendment>
            <@recordWorkProgrammeAmendmentSummaryCard.recordWorkProgrammeAmendmentCard
                workProgrammeAmendment=workProgrammeAmendment
                summaryListId="work-programme-amendment-${workProgrammeAmendment?index}"/>
        </#list>

        <@fdsRadio.radioGroup
            path="form.recordWorkProgrammeAmendmentSummaryOptions"
            labelText="Do you want to add another work programme activity to the scope of this decision?"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-fieldset__legend--m">
            <#assign isFirstOption=true/>
            <#list summaryOptions as optionKey, optionValue>
                <@fdsRadio.radioItem
                    path="form.recordWorkProgrammeAmendmentSummaryOptions"
                    itemMap={optionKey : optionValue}
                    isFirstItem=isFirstOption/>
                <#assign isFirstOption=false/>
            </#list>
        </@fdsRadio.radioGroup>

        <@fdsAction.submitButtons
            primaryButtonText="Save and continue"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>
