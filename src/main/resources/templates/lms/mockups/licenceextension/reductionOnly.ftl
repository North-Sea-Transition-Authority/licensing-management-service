<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN>
    <@fdsForm.htmlForm>

        <@fdsCheckbox.checkboxGroup
            path="form.selectedReduce['all']"
            fieldsetHeadingText="What do you want to reduce?"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-label govuk-label--l"
            hiddenContent=true>

            <#list terms as term>
                <@fdsCheckbox.checkboxItem
                    path="form.selectedReduce['${term.id()}']"
                    labelText="${term.name()}"
                    inputHintText="Due to end ${term.endDate()}">
                    <@duration.threeFieldDuration
                        dayPath="form.reductionDuration['${term.id()}'].days"
                        monthPath="form.reductionDuration['${term.id()}'].months"
                        yearPath="form.reductionDuration['${term.id()}'].years"
                        nestingPath="form.selectedReduce['${term.id()}']"
                        fieldNamePath="form.reductionDuration['${term.id()}'].fieldName"
                        fieldDisplayTextPath="form.reductionDuration['${term.id()}'].fieldDisplayText"
                        labelText="How long is this to be reduced by?"
                        formId="${term.id()}"/>
                </@fdsCheckbox.checkboxItem>
            </#list>
        </@fdsCheckbox.checkboxGroup>

        <@fdsAction.submitButtons primaryButtonText="Save and complete" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl("/mockups/decision-journey")/>

    </@fdsForm.htmlForm>
</@defaultPage>
