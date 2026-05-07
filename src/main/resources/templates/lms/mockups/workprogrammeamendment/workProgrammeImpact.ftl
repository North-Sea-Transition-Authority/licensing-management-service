<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<@defaultPage htmlTitle=pageTitle pageHeading="" pageSize=PageSize.TWO_THIRDS_COLUMN>
    <@fdsForm.htmlForm>

        <@fdsSummaryList.summaryListCard summaryListId="wp-impact-card" headingText=workProgramme.description headingSize="h1">
            <@fdsSummaryList.summaryListRowNoAction keyText="Due date">27 July 2026</@fdsSummaryList.summaryListRowNoAction>
            <@fdsSummaryList.summaryListRowNoAction keyText="Commitment">Firm</@fdsSummaryList.summaryListRowNoAction>
            <@fdsSummaryList.summaryListRowNoAction keyText="Legal text">The licensee shall...</@fdsSummaryList.summaryListRowNoAction>
        </@fdsSummaryList.summaryListCard>

        <@fdsRadio.radioGroup
            path="form.action"
            labelText=pageTitle
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-fieldset__legend--m"
            hiddenContent=true>

            <@fdsRadio.radioItem path="form.action" itemMap={"amend": "Amend duration or text"} isFirstItem=true>
                <@fdsCheckbox.checkboxGroup
                    path="form.action"
                    fieldsetHeadingText="What do you want to do?"
                    fieldsetHeadingSize="h2"
                    fieldsetHeadingClass="govuk-label govuk-label--s"
                    nestingPath="form.action"
                    hiddenContent=true>

                    <@fdsCheckbox.checkboxItem
                        path="form.extendDuration"
                        labelText="Amend duration">
                        <@duration.threeFieldDuration
                            dayPath="form.duration.days"
                            monthPath="form.duration.months"
                            yearPath="form.duration.years"
                            fieldNamePath="form.duration.fieldName"
                            fieldDisplayTextPath="form.duration.fieldDisplayText"
                            nestingPath="form.extendDuration"
                            labelText="Duration of extension"
                            formId="amendment-duration"/>
                    </@fdsCheckbox.checkboxItem>

                    <@fdsCheckbox.checkboxItem
                        path="form.amendText"
                        labelText="Amend text">
                        <@fdsTextarea.textarea
                            path="form.amendedText"
                            labelText="Amended work programme text"
                            nestingPath="form.amendText"
                        />
                    </@fdsCheckbox.checkboxItem>

                </@fdsCheckbox.checkboxGroup>
            </@fdsRadio.radioItem>

            <@fdsRadio.radioItem path="form.action" itemMap={"waive": "Waive"} />

            <@fdsRadio.radioItem path="form.action" itemMap={"transfer": "To be completed on another licence"}>
                <@fdsAddToList.addToList
                    pathForList="form.targetLicences"
                    pathForSelector="form.transferSelector"
                    alreadyAdded=transferLicences
                    addToListId="transfer-licences"
                    selectorLabelText="Select a licence (optional)"
                    selectorNestingPath="form.action"
                    restUrl=springUrl(searchUrl)
                />
            </@fdsRadio.radioItem>

            <@fdsRadio.radioItem path="form.action" itemMap={"delay": "Acknowldge - delay / No further action"} />

        </@fdsRadio.radioGroup>

        <@fdsAction.submitButtons
            primaryButtonText="Save and continue"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl("/mockups/work-programme-amendment/summary")/>

    </@fdsForm.htmlForm>
</@defaultPage>
