<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN>
    <@fdsForm.htmlForm>

        <@fdsCheckbox.checkboxGroup
            path="form.selectedWorkProgrammes['all']"
            fieldsetHeadingText="Which work programme activites form part of the decision?"
            fieldsetHeadingSize="h1"
            fieldsetHeadingClass="govuk-fieldset__legend--m"
            hiddenContent=true>

            <#list workProgrammes as wp>
                <@fdsCheckbox.checkboxItem
                    path="form.selectedWorkProgrammes['${wp.id}']"
                    labelText="${wp.description}">

                    <@fdsRadio.radioGroup
                        path="form.workProgrammeActions['${wp.id}']"
                        labelText="What is the decision in relation to this activity?"
                        fieldsetHeadingSize="h2"
                        fieldsetHeadingClass="govuk-fieldset__legend--s"
                        nestingPath="form.selectedWorkProgrammes['${wp.id}']"
                        hiddenContent=true
                    >
                        <@fdsRadio.radioItem path="form.workProgrammeActions['${wp.id}']" itemMap={"amend": "Amend or extend"} isFirstItem=true>
                            <@fdsCheckbox.checkboxGroup
                                path="form.workProgrammeAmendOrExtend['${wp.id}-all']"
                                fieldsetHeadingText="What do you want to do?"
                                fieldsetHeadingSize="h2"
                                fieldsetHeadingClass="govuk-label govuk-label--s"
                                nestingPath="form.workProgrammeActions['${wp.id}']"
                                hiddenContent=true>

                                <@fdsCheckbox.checkboxItem
                                    path="form.workProgrammeAmendOrExtend['${wp.id}-extend']"
                                    labelText="Amend duration">
                                    <@duration.threeFieldDuration
                                        dayPath="form.workProgrammeDurations['${wp.id}'].days"
                                        monthPath="form.workProgrammeDurations['${wp.id}'].months"
                                        yearPath="form.workProgrammeDurations['${wp.id}'].years"
                                        fieldNamePath="form.workProgrammeDurations['${wp.id}'].fieldName"
                                        fieldDisplayTextPath="form.workProgrammeDurations['${wp.id}'].fieldDisplayText"
                                        nestingPath="form.workProgrammeAmendOrExtend['${wp.id}-extend']"
                                        labelText="Duration of extension"
                                        formId="amendment-duration-${wp.id}"/>
                                </@fdsCheckbox.checkboxItem>

                                <@fdsCheckbox.checkboxItem
                                    path="form.workProgrammeAmendOrExtend['${wp.id}-amend']"
                                    labelText="Amend text">
                                    <@fdsTextarea.textarea
                                        path="form.workProgrammeAmendedTexts['${wp.id}']"
                                        labelText="Amended work programme text"
                                        nestingPath="form.workProgrammeAmendOrExtend['${wp.id}-amend']"
                                    />
                                </@fdsCheckbox.checkboxItem>

                            </@fdsCheckbox.checkboxGroup>
                        </@fdsRadio.radioItem>

                        <@fdsRadio.radioItem path="form.workProgrammeActions['${wp.id}']" itemMap={"waive": "Waive"} />

                        <@fdsRadio.radioItem path="form.workProgrammeActions['${wp.id}']" itemMap={"transfer": "Transferred to another licence"}>
                            <@fdsAddToList.addToList
                                pathForList="form.workProgrammeTargetLicences['${wp.id}']"
                                pathForSelector="form.workProgrammeTransferSelectors[${wp.id}]"
                                alreadyAdded=transferLicences
                                addToListId="transfer-licences-${wp.id}"
                                selectorLabelText="Select a licence (optional)"
                                selectorNestingPath="form.workProgrammeActions['${wp.id}']"
                                restUrl=springUrl(searchUrl)
                            />
                        </@fdsRadio.radioItem>

                        <@fdsRadio.radioItem path="form.workProgrammeActions['${wp.id}']" itemMap={"delay": "Delay / No further action"} />

                    </@fdsRadio.radioGroup>

                </@fdsCheckbox.checkboxItem>
            </#list>

            <#list additionalWorkProgrammes as wp>
                <@fdsCheckbox.checkboxItem
                    path="form.selectedWorkProgrammes['${wp.id}']"
                    labelText="${wp.description}">

                    <@fdsRadio.radioGroup
                        path="form.workProgrammeActions['${wp.id}']"
                        labelText="What is the decision in relation to this activity?"
                        fieldsetHeadingSize="h2"
                        fieldsetHeadingClass="govuk-fieldset__legend--s"
                        nestingPath="form.selectedWorkProgrammes['${wp.id}']"
                        hiddenContent=true
                    >
                        <@fdsRadio.radioItem path="form.workProgrammeActions['${wp.id}']" itemMap={"amend": "Amend or extend"} isFirstItem=true>
                            <@fdsCheckbox.checkboxGroup
                                path="form.workProgrammeAmendOrExtend['${wp.id}-all']"
                                fieldsetHeadingText="What do you want to do?"
                                fieldsetHeadingSize="h2"
                                fieldsetHeadingClass="govuk-label govuk-label--s"
                                nestingPath="form.workProgrammeActions['${wp.id}']"
                                hiddenContent=true>

                                <@fdsCheckbox.checkboxItem
                                    path="form.workProgrammeAmendOrExtend['${wp.id}-extend']"
                                    labelText="Amend duration">
                                    <@duration.threeFieldDuration
                                        dayPath="form.workProgrammeDurations['${wp.id}'].days"
                                        monthPath="form.workProgrammeDurations['${wp.id}'].months"
                                        yearPath="form.workProgrammeDurations['${wp.id}'].years"
                                        fieldNamePath="form.workProgrammeDurations['${wp.id}'].fieldName"
                                        fieldDisplayTextPath="form.workProgrammeDurations['${wp.id}'].fieldDisplayText"
                                        nestingPath="form.workProgrammeAmendOrExtend['${wp.id}-extend']"
                                        labelText="Duration of extension"
                                        formId="amendment-duration-${wp.id}"/>
                                </@fdsCheckbox.checkboxItem>

                                <@fdsCheckbox.checkboxItem
                                    path="form.workProgrammeAmendOrExtend['${wp.id}-amend']"
                                    labelText="Amend text">
                                    <@fdsTextarea.textarea
                                        path="form.workProgrammeAmendedTexts['${wp.id}']"
                                        labelText="Amended work programme text"
                                        nestingPath="form.workProgrammeAmendOrExtend['${wp.id}-amend']"
                                    />
                                </@fdsCheckbox.checkboxItem>

                            </@fdsCheckbox.checkboxGroup>
                        </@fdsRadio.radioItem>

                        <@fdsRadio.radioItem path="form.workProgrammeActions['${wp.id}']" itemMap={"waive": "Waive"} />

                        <@fdsRadio.radioItem path="form.workProgrammeActions['${wp.id}']" itemMap={"transfer": "Transferred to another licence"}>
                            <@fdsAddToList.addToList
                                pathForList="form.workProgrammeTargetLicences['${wp.id}']"
                                pathForSelector="form.workProgrammeTransferSelectors[${wp.id}]"
                                alreadyAdded=transferLicences
                                addToListId="transfer-licences-${wp.id}"
                                selectorLabelText="Select a licence (optional)"
                                selectorNestingPath="form.workProgrammeActions['${wp.id}']"
                                restUrl=springUrl(searchUrl)
                            />
                        </@fdsRadio.radioItem>

                        <@fdsRadio.radioItem path="form.workProgrammeActions['${wp.id}']" itemMap={"delay": "No further action"} />

                    </@fdsRadio.radioGroup>

                </@fdsCheckbox.checkboxItem>
            </#list>
        </@fdsCheckbox.checkboxGroup>

        <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl("/mockups/decision-journey")/>

    </@fdsForm.htmlForm>
</@defaultPage>
