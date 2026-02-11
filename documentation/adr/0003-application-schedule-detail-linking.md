# See schedule details as they were at time of submission of application
* Author: Harshid Dattani

## Context and problem statement

Both `ScheduleWorkProgrammeApplication` and `LicenceContinuationApplication` link directly to a `LicenceScheduleDetail`. When the `ACTIVE` detail is `REPLACED` (e.g. via a schedule amendment), the application view becomes inconsistent — it shows the current state of the detail rather than the state at submission time.

We need draft applications to resolve schedule information from the current `ACTIVE` detail, while submitted applications should be frozen to the detail that was active at submission.

## Option 1 - Link to schedule and store submitted schedule detail on submit

Change the relationship from `licenceScheduleDetail` to `licenceSchedule`, linking applications to `LicenceSchedule` instead of `LicenceScheduleDetail`. Add a nullable `submittedLicenceScheduleDetail` field.

Drafts resolve schedule information by finding the `ACTIVE` detail for the linked schedule. On submit, the `ACTIVE` detail ID is stored as the submitted schedule detail. Viewing a submitted application uses the stored detail.

### Pros
- Drafts always reflect the current `ACTIVE` schedule detail.
- Submitted applications are decoupled from future schedule changes.

### Cons
- Requires a migration to change the relationship and add the new field.
- Resolution of schedule detail logic differs between draft and submitted states.

## Option 2 - Update the linked schedule detail on submit

Keep the existing `licenceScheduleDetail` relationship. On submit, update it to point at the current `ACTIVE` detail.

### Pros
- No schema changes required — uses the existing field.

### Cons
- The linked detail may become stale while the application is in draft if the `ACTIVE` detail is `REPLACED`.
- No distinction between "the detail the draft is using" and "the detail frozen at submission" — both use the same field.

## Decision outcome

The decision is to link applications to `LicenceSchedule` and store the submitted `LicenceScheduleDetail` on submit (Option 1).

### Scope of changes

**New method to resolve the schedule detail:**
- Add `getScheduleDetailFromApplicationDetail()` to both `ScheduleWorkProgrammeApplicationService` and `LicenceContinuationService`. This method will return the `submittedLicenceScheduleDetail` if set, otherwise resolve the `ACTIVE` detail from the linked `LicenceSchedule`.

**Setting the relationship at creation:**
- `ScheduleWorkProgrammeApplicationService.createScheduleWorkProgrammeApplication()`: currently sets the `LicenceScheduleDetail` with the `ACTIVE` detail. Will change to set the `LicenceSchedule`.
- `LicenceContinuationService.createNewLicenceContinuationApplication()`: same change.

**Setting the submitted schedule detail on submit:**
- `ScheduleWorkProgrammeApplicationService`: on submit, store the current `ACTIVE` `LicenceScheduleDetail` in `submittedLicenceScheduleDetail`.
- `LicenceContinuationService`: same change.

**Getting the licence for an application:**
- `ScheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail()`: Will use the new `getScheduleDetailFromApplicationDetail()` method.
- `LicenceContinuationService.getLicenceFromContinuationApplicationDetail()`: same change.

**Reading schedule data from the detail:**
  The following places will use the new `getScheduleDetailFromApplicationDetail()` method instead of accessing the detail directly from the application entity.
- `LicenceScheduleExtensionController.getModelAndView()`
- `LicenceScheduleExtensionService.getLicenceScheduleExtensionViews()`
- `SelectLicenceWorkAmendmentController.getModelAndView()`
- `LicenceContinuationWpaRequirementController.getModelAndView()`
