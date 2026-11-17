# Decision letter templates and template selection

* Status: proposed
* Date: 2026-11-06

## Summary

NSTA supplied seven decision letters. Six can be triggered from decisions LMS already records, two of them on assumptions or wording NSTA still need to confirm. NO WP Waiver cannot be triggered yet.

* **Selection.** Today LMS picks a letter template from the application type alone. It also needs to take the decision outcome into account, such as approved or refused.
* **Fields.** 11 new mail merge fields, all with an identified source (10 if clarification 4 is accepted).

## NSTA templates provided

**Common to all seven:** NSTA letterhead (address, phone, email, website), addressee name and address, date, "Dear [requisitioner]", subject "Decision Letter - RE: Licence [number]" and "Thank you for the application reference [number]".

| Template              | Decision stated                                                        | Reasoning | Free text | Confirm in 2 weeks | Contact email | "Not progressing" warning | Final, complaints, JR | Sign-off | Legal footer |
|-----------------------|------------------------------------------------------------------------|:---------:|:---------:|:------------------:|:-------------:|:-------------------------:|:---------------------:|:--------:|:------------:|
| Extension Approval    | Grant an X month extension of a phase or term. Subsequent term reduced |     -     |    Yes    |        Yes         |      Yes      |            Yes            |           -           |    -     |      -       |
| NO Extension          | Refuse extension. Licence ends at the phase or term end date           |    Yes    |    Yes    |         -          |       -       |             -             |          Yes          |    -     |     Yes      |
| WP Amendment Approval | Amend a named work programme activity                                  |     -     |    Yes    |        Yes         |      Yes      |            Yes            |           -           |    -     |      -       |
| NO WP Amendment       | Refuse work programme amendment                                        |    Yes    |    Yes    |         -          |       -       |             -             |          Yes          |   Yes    |     Yes      |
| WP Waiver Approval    | Waive a contingent commitment. Quotes the commitment's legal text      |     -     |    Yes    |         -          |      Yes      |            Yes            |           -           |    -     |     Yes      |
| NO WP Waiver          | Refuse to waive a contingent commitment                                |    Yes    |    Yes    |         -          |       -       |            Yes            |          Yes          |    -     |     Yes      |
| WP Delay              | Refuse amendment, but NSTA takes no action on the missed deadline      |     -     |    Yes    |         -          |       -       |            Yes            |           -           |    -     |     Yes      |

## NSTA review comments (from the Word files)

* Waiver letters, approval and refusal, are not final. NSTA want to see how waivers work in practice first.
* WP Delay is a case-by-case side letter, mostly used for carbon storage licences.

## Template to data mapping

| Template              | Selected by                                                | Status                                             |
|-----------------------|------------------------------------------------------------|----------------------------------------------------|
| Extension Approval    | `extensionDecision = GRANTED`                              | Ready                                              |
| NO Extension          | `extensionDecision = REJECTED`                             | Ready                                              |
| WP Amendment Approval | `workProgrammeDecision = GRANTED` + activity `AMEND`       | Ready                                              |
| NO WP Amendment       | `workProgrammeDecision = REJECTED`                         | Ready                                              |
| WP Waiver Approval    | `workProgrammeDecision = GRANTED` + activity `WAIVE`       | Ready\*                                            |
| NO WP Waiver          | `workProgrammeDecision = REJECTED`                         | **Blocked**\*\*                                    |
| WP Delay              | `workProgrammeDecision = GRANTED` + activity `ACKNOWLEDGE` | Ready\*\*\*                                        |

\* Triggerable from existing data, but NSTA's comment says the wording is not final.

\*\* A refusal to waive cannot currently be recorded (clarification 1).

\*\*\* Assumption: `ACKNOWLEDGE` ("Acknowledge - no further action") is the delay outcome. NSTA to confirm (clarification 2).

No template exists for the "To be completed on another licence" outcome.

## Mail merge fields

Existing and reusable: `LICENCE_REFERENCE`, `REGULATOR_MNEMONIC`, `REGULATOR_NAME`, `SIGNATORY_NAME`, `DIGITAL_SIGNATURE`. Company name, address and date are already rendered by `document.ftl`.

`CURRENT_TERM_PHASE_NAME` is not suitable for extension letters: it returns the current term or phase, which may differ from the one extended.

| New field                           | Source                                                                                               | Used in                                   |
|-------------------------------------|------------------------------------------------------------------------------------------------------|-------------------------------------------|
| `APPLICATION_REFERENCE`             | `ScheduleWorkProgrammeApplication.applicationReference`                                              | All                                       |
| `REQUESTER_NAME`                    | Submitter display name via `EnergyPortalUserService.findByWuaId`                                     | All (see clarification 4)                 |
| `EXTENSION_TERM_PHASE_NAME`         | `RecordOfDecisionExtension` phase or term                                                            | Extension letters                         |
| `EXTENSION_DURATION`                | `RecordOfDecisionExtension.extensionDuration`                                                        | Extension Approval                        |
| `CURRENT_EXPIRY_DATE`               | Phase or term `endDate`                                                                              | Extension letters                         |
| `NEW_EXPIRY_DATE`                   | `endDate` plus extension                                                                             | Extension Approval                        |
| `SUBSEQUENT_TERM_REDUCTION`         | `RecordOfDecisionReduction` rows (conditional)                                                       | Extension Approval                        |
| `CONFIRM_BY_DATE`                   | Letter date + 14 days                                                                                | Extension Approval, WP Amendment Approval |
| `WORK_PROGRAMME_ACTIVITY`           | `RecordOfDecisionWorkProgramme.workProgrammeActivity` via `getCategoryString()` + `getDescription()` | WP Amendment Approval                     |
| `AMENDED_DURATION` / `AMENDED_TEXT` | `RecordOfDecisionWorkProgramme`                                                                      | WP Amendment Approval                     |

Free-text sections: `[REASONING]` in the refusal letters, `[Free form entry]` in all letters.

## Outstanding clarifications

| # | Question                                                                                                                                                                                                                                                            | Proposal                                                                                                                                     |
|---|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | Which refusal letter does a refused work programme request produce? NSTA records a single Yes or No. Only a Yes leads to a per-commitment choice (amend, waive and so on), so a No cannot show whether an amendment or a waiver was refused                         | Always send NO WP Amendment. Add NO WP Waiver once NSTA define how a refused waiver is recorded                                              |
| 2 | When is the WP Delay letter sent? We assume it follows the "Acknowledge - no further action" outcome, used for short extensions. The letter says NSTA will not approve an amendment, and NSTA's comment says it is a case-by-case side letter, not an automatic one | Send it for that outcome. NSTA to confirm, otherwise the issuer adds it manually                                                             |
| 3 | Does the "To be completed on another licence" outcome need any text in the letter? NSTA have no template for it                                                                                                                                                     | No. The letter has no section for it, so it may contain no work programme content                                                            |
| 4 | Who is the letter addressed to? NSTA's templates say "Dear [requisitioner]", the person who submitted the application. Existing LMS letters address the company ("Dear [company]", "Dear licensee")                                                                 | Address the company, as existing letters do. This drops the requester name field                                                             |
| 5 | Which sign-off should the letters use? Only NO WP Amendment has one in NSTA's files ("Yours sincerely", a named manager and job title). Is this intentional? Existing LMS letters end "Yours faithfully" with the signatory's name                                  | Use NSTA's wording on all letters, from the existing sign-off component                                                                      |
| 6 | The WP Waiver Approval letter must quote the legal text of the contingent commitment being waived. Where does LMS get that text? The activity description is free text and may not match the licence wording                                                        | Manual mail merge field (`??...??`) for the issuer to complete. The letter cannot be issued until it is replaced. NSTA to confirm the source |

## Mixed outcomes

One application can hold an extension decision plus several work programme activity decisions. Does it get one letter or several?

|       | A: composed letter                                                         | B: letter per decision                     |
|-------|----------------------------------------------------------------------------|--------------------------------------------|
| Shape | One letter, sections switched on by outcome (like `ContinuationCondition`) | One letter per decision, one template each |
| Pros  | Mixed outcomes work                                                        | Maps one to one onto NSTA's files          |
| Cons  | Each conditional section needs a template definition                       | Several letters per application            |

## Links

* Refines [ADR-0004](0004-mail-merge-fields.md)
