<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  backLinkUrl=springUrl(backLinkUrl)
>
  <@fdsForm.htmlForm>
    <@fdsSummaryList.summaryList>
      <@fdsSummaryList.summaryListRowNoAction keyText="Current position date">
        ${currentPositionDate}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="Regulator reference">
        ${regulatorReference}
      </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryList>
    <@fdsDateInput.dateInput
      dayPath="form.correctPositionDate.dayInput.inputValue"
      monthPath="form.correctPositionDate.monthInput.inputValue"
      yearPath="form.correctPositionDate.yearInput.inputValue"
      labelText="Position date"
      formId="correctPositionDate"
    />
    <@fdsAction.submitButtons
      primaryButtonText="Update date"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>