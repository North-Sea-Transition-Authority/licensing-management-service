<#include '../../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
>
  <@fdsForm.htmlForm>
    <@fdsSummaryList.summaryList>
      <@fdsSummaryList.summaryListRowNoAction keyText="Execution date">
        ${positionDate}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="Transaction reference">
        ${positionReference}
      </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryList>

    <@fdsDateInput.dateInput
      dayPath="form.correctPositionDate.dayInput.inputValue"
      monthPath="form.correctPositionDate.monthInput.inputValue"
      yearPath="form.correctPositionDate.yearInput.inputValue"
      labelText="What do you want the new execution date to be?"
      formId="correctPositionDate"
    />

    <@fdsAction.submitButtons
      primaryButtonText="Continue"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
