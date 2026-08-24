<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
breadcrumbs=breadcrumbs>
    <@fdsForm.htmlForm>

        <@fdsSummaryList.summaryListCard
            summaryListId="work-programme-amendment-activity-detail"
            headingText=workProgrammeActivityDetails.category()>

            <@fdsSummaryList.summaryListRowNoAction keyText="Description">
                ${workProgrammeActivityDetails.description()}
            </@fdsSummaryList.summaryListRowNoAction>

            <@fdsSummaryList.summaryListRowNoAction keyText="Due date">
                ${workProgrammeActivityDetails.dueDate()}
            </@fdsSummaryList.summaryListRowNoAction>

            <@fdsSummaryList.summaryListRowNoAction keyText="Commitment">
                ${workProgrammeActivityDetails.commitment()}
            </@fdsSummaryList.summaryListRowNoAction>

        </@fdsSummaryList.summaryListCard>

        <@fdsRadio.radioGroup
            path="form.decision"
            labelText="What is the decision in relation to this activity?"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-fieldset__legend--m"
            hiddenContent=true>

            <@fdsRadio.radioItem
                path="form.decision"
                itemMap={"AMEND" : decisionOptions["AMEND"]}
                isFirstItem=true>
                <@fdsCheckbox.checkboxGroup
                    path="form.amendDuration"
                    fieldsetHeadingText="What is being amended?"
                    fieldsetHeadingSize="h3"
                    fieldsetHeadingClass="govuk-label govuk-label--s"
                    nestingPath="form.decision"
                    hiddenContent=true>

                    <@fdsCheckbox.checkboxItem
                        path="form.amendDuration"
                        labelText="Amend duration">
                        <@duration.threeFieldDuration
                            dayPath="form.amendedDuration.days"
                            monthPath="form.amendedDuration.months"
                            yearPath="form.amendedDuration.years"
                            fieldNamePath="form.amendedDuration.fieldName"
                            fieldDisplayTextPath="form.amendedDuration.fieldDisplayText"
                            nestingPath="form.amendDuration"
                            labelText="How long is the activity to be amended by?"
                            formId="amended-duration"/>
                    </@fdsCheckbox.checkboxItem>

                    <@fdsCheckbox.checkboxItem
                        path="form.amendText"
                        labelText="Amend text">
                        <@fdsTextarea.textarea
                            path="form.amendedText"
                            labelText="Amended work programme text"
                            nestingPath="form.amendText"/>
                    </@fdsCheckbox.checkboxItem>

                </@fdsCheckbox.checkboxGroup>
            </@fdsRadio.radioItem>

            <@fdsRadio.radioItem path="form.decision" itemMap={"WAIVE" : decisionOptions["WAIVE"]}/>

            <@fdsRadio.radioItem
                path="form.decision"
                itemMap={"COMPLETE_ON_ANOTHER_LICENCE" : decisionOptions["COMPLETE_ON_ANOTHER_LICENCE"]}>
                <@fdsAddToList.addToList
                    pathForList="form.targetLicenceIds"
                    pathForSelector="form.targetLicenceSelector"
                    alreadyAdded=targetLicences
                    addToListId="target-licences"
                    selectorLabelText="Select a licence"
                    selectorOptionaLabel=true
                    selectorNestingPath="form.decision"
                    itemName="Licences"
                    restUrl=springUrl(searchUrl)/>
            </@fdsRadio.radioItem>

            <@fdsRadio.radioItem path="form.decision" itemMap={"ACKNOWLEDGE" : decisionOptions["ACKNOWLEDGE"]}/>

        </@fdsRadio.radioGroup>

        <@fdsAction.submitButtons
            primaryButtonText="Save and continue"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>
