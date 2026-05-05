<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN>
    <@fdsForm.htmlForm>

        <@fdsCheckbox.checkboxGroup
            path="form.selectedExtend['all']"
            fieldsetHeadingText="Select the terms and phases to extend"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-label govuk-label--l"
            hiddenContent=true>

            <#list terms as term>
                <#if term.canExtend()>
                <@fdsCheckbox.checkboxItem
                    path="form.selectedExtend['${term.id()}']"
                    labelText="${term.name()}"
                    inputHintText="Due to end ${term.endDate()}">
                    <@duration.threeFieldDuration
                        dayPath="form.extensionDuration['${term.id()}'].days"
                        monthPath="form.extensionDuration['${term.id()}'].months"
                        yearPath="form.extensionDuration['${term.id()}'].years"
                        nestingPath="form.selectedExtend['${term.id()}']"
                        fieldNamePath="form.extensionDuration['${term.id()}'].fieldName"
                        fieldDisplayTextPath="form.extensionDuration['${term.id()}'].fieldDisplayText"
                        labelText="How long is this term/phase to be extended by?"
                        formId="${term.id()}"/>
                </@fdsCheckbox.checkboxItem>
                </#if>
            </#list>
        </@fdsCheckbox.checkboxGroup>

        <@fdsAction.submitButtons primaryButtonText="Continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl("/mockups/decision-journey")/>

    </@fdsForm.htmlForm>
</@defaultPage>
