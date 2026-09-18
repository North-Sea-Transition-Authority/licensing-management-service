---
name: changing-documented-licence-areas
description: >
  Required reading before changing anything under licence/schedule,
  licence/scheduleworkprogrammeapplication, licence/continuation, licence/reminder or
  licence/crosslicenceeventtracker — a Flyway migration, an @Entity, a persisted enum, or the
  application code that decides what gets written: status transitions, date and reference
  calculation, duplication behaviour, which questions a journey asks, reminder sending rules,
  event cache refresh, file usage types, document templates and team scoping. Four documents
  in documentation/businessandtechnical/ describe both the schema and that behaviour to people
  outside the team, nothing regenerates them, and keeping them true is part of the change.
  Also use when asked whether those documents still match the code.
---

# Changing documented licence areas

The documents in `documentation/businessandtechnical/` describe this part of the service to
people who do not read the code — analysts writing reporting queries, testers checking data,
and developers new to the service. At the time of writing there are four; Step 1 is how you
confirm that is still the case:

| Document | Covers |
|---|---|
| `licence-schedules-data-model.md` | `licences`, `licence_schedules`, `licence_schedule_details` and the seven schedule content tables, plus `schedule_events`, `event_comments` and `work_programme_activity_statuses` |
| `licence-applications-data-model.md` | `schedule_work_programme_applications` and `licence_continuation_applications`, their detail tables, and every request, requirement and record-of-decision table hanging off them |
| `licence-schedule-reminders-data-model.md` | `licence_reminders` — which schedule deadlines have been warned about, and what decides whether a reminder is sent |
| `licence-event-cache-data-model.md` | `licence_event_cache` — the denormalised read model behind the cross licence event tracker, and when it is rebuilt |

The split is deliberate: reminders and the event tracker cache are driven by the schedule but
are separate areas of functionality, and folding them into the schedules document would bury
the schedule itself. Keep them separate. When a change spans two of these — a new schedule
event type that should also be remindable, say — update both rather than cross-referencing one
from the other.

**Nothing regenerates them.** A change that lands without the matching documentation edit
silently makes them wrong, and wrong is worse than absent — someone will write a query
against the stale version and believe the result.

So the change is not finished when the code works. It is finished when the documents match.
Same branch, same commit. Not a follow-up ticket, and not an optional extra to offer the
user at the end.

## These documents are not just a schema dump

This is the part that gets missed. Roughly half of what they assert cannot be seen in the
schema at all — it is behaviour that lives in application code, and it goes stale without a
single migration being written. Some of what they claim:

- One `ACTIVE` schedule version per licence, and one `licence_schedules` row per licence —
  both described as invariants enforced in application code rather than by a constraint.
- Updating a schedule **copies every child row field-for-field** into a new version, with
  fresh ids, and re-points the internal references by matching on `term_type` / `phase_type`.
- Dates are **recalculated and re-persisted for the entire version** on every add, edit or
  delete — including the precise arithmetic (a term ending the day before its anniversary,
  pure-day durations not adjusted, rate end dates never stored at all).
- `PENDING` event comments are **promoted to `PUBLISHED`** when a draft is applied, and hard
  deleted when it is discarded.
- `work_programme_activity_statuses` is an **append-only ledger**; a copied activity reuses
  the existing ledger rather than starting a new one.
- Deletes inside a draft are physical; deleting a draft or an application is a status flip.
- Which statuses each application family actually reaches — extension and amendment is
  documented as never reaching `COMPLETE` or `WITHDRAWN`.
- The application reference formats `LMS/EAA/{year}/{n}` and `LMS/CA/{year}/{n}`, the fact
  that `n` counts version-1 submissions in the calendar year, and that the reference is
  assigned at submission rather than creation.
- Which request-purpose options a licence type is offered, and which continuation
  requirements are asked for based on the *next* term or phase.
- The Maintain / Reduce / Extend decision being expressed by **which table holds a row**
  rather than by a stored value.
- **Recording a decision does not change the schedule** — an absence of behaviour, documented
  as load-bearing.
- Which external contributor team is scoped on the application versus the application detail,
  the file usage type strings, and which document template each family uses.
- That a reminder is withheld entirely for a licence that is not `EXTANT`, and that a deadline
  surviving a schedule update is not warned about a second time.
- That the tracker cache is rebuilt only when a schedule is applied, holds no rates, other
  events or expiry dates, and has four columns nothing ever writes.

All of that is code, not schema. Changing it changes the documents.

## Step 1 — check which documents exist

The table above is a snapshot and this skill is edited by hand, so it goes stale the moment
someone adds a document without updating it. Start by listing the directory:

```bash
ls documentation/businessandtechnical/
```

| What you see | What to do |
|---|---|
| Exactly the four documents listed above | Carry on |
| A document not in the table | Read its opening section — the title, the "what this is" paragraph and its table list. Treat it exactly like the others for the rest of this skill: audit it for drift, work out what your change invalidates in it, edit it in the same commit. Then **say that this skill's table is out of date** so it can be corrected. |
| A document listed above that is missing | Do not assume it was deleted on purpose — check `git log` for a rename before concluding anything |

Do not skip this because you already know what the documents are. The cost is one command;
the failure it prevents is editing three documents while a fourth quietly contradicts them.

If the change you are about to make belongs to none of the documents that exist, that is a
finding in itself — see the note at the end of Step 4 about starting a new document.

## Step 2 — recognise you are in scope

You are, if the change touches any of:

| Area | Typical entry points |
|---|---|
| Schema | a `V<n>__*.sql` migration, an `@Entity` class, an enum persisted as `TEXT` |
| Status transitions | anything calling `setStatus`, `LicenceContinuationService`, `ScheduleWorkProgrammeApplicationService`, `RecordFinalDecisionService` |
| Derived values | date calculation for terms, phases, activities and rates; `LicenceScheduleStateService`; the application reference format constants |
| Versioning and copying | the `duplication` package, `DuplicationSource` / `NotDuplicationSource`, `@DuplicateThisOnUpdate`, `original_event_id` handling |
| What a journey asks | `SwpApplicationRequestPurposeService`, `OtherRequirementsVisibilityResolverService`, `LicenceTypeFeature` and the licence type rules |
| Reminder sending | the `licence/reminder` package — the deadline sources, `ReminderSuppressionService`, `ReminderType`, `NoticePeriod`, and the uniqueness keys that stop a repeat send |
| Event cache refresh | `CrossLicenceEventTrackerService.refreshScheduleCache` and anything that changes which events it writes, or adds a second caller |
| Cross-cutting bindings | `FileUsageType`, `LicenceScheduleFileUsageType`, `ApplicationLetterService` and document template types, `TeamType.EXTERNAL_CONTRIBUTORS` scoping |

A refactor counts. If it changes *what is written or when*, it changes the documents — even
if no column moves.

## Step 3 — audit for drift that arrived without Claude

Most commits on this repo are written without Claude, so by the time you are invoked the
documents may already be out of date for reasons that have nothing to do with the task in
hand. Do this audit **before** editing: otherwise you carefully preserve wording around a
claim that stopped being true three sprints ago.

Each document carries its own baseline — the commit that last touched it:

```bash
DOC=documentation/businessandtechnical/licence-schedules-data-model.md
SHA=$(git log -1 --format=%H -- "$DOC")
git log -1 --format='last documented: %h %ad %s' --date=short -- "$DOC"
```

Then look at what has changed in the code that document describes since that commit:

```bash
BASE=src/main/java/uk/co/nstauthority/licensingmanagementservice/licence
git log --oneline "$SHA"..HEAD -- src/main/resources/db/migration "$BASE/schedule"
```

Each document has its own baseline and its own code paths. Substitute accordingly:

| Document | Code paths, alongside `src/main/resources/db/migration` |
|---|---|
| `licence-schedules-data-model.md` | `licence/schedule` |
| `licence-applications-data-model.md` | `licence/scheduleworkprogrammeapplication`, `licence/continuation` |
| `licence-schedule-reminders-data-model.md` | `licence/reminder` |
| `licence-event-cache-data-model.md` | `licence/crosslicenceeventtracker` |

The two derived documents have a second dependency that the paths above will not show: both
are driven by schedule content, so a change under `licence/schedule` can invalidate them
without touching their own package. A new schedule event type, a change to how an activity's
effective due date resolves, or a new term or phase display name all reach them. If the
schedules audit turns up anything, check those two as well.

Two checks are worth running every time, because they are cheap and catch the two failure
modes that matter most.

**New migrations since the document was written** — the highest-signal check, since a
migration almost always means a documented table changed shape:

```bash
git diff --name-only --diff-filter=A "$SHA"..HEAD -- src/main/resources/db/migration
```

**Tables that exist but appear in no document** — catches a whole feature that was never
documented:

```bash
grep -rhoiE "CREATE TABLE (IF NOT EXISTS )?[a-z_]+" src/main/resources/db/migration/ \
  | awk '{print tolower($NF)}' | grep -vE "_aud$" | sort -u \
  | grep -E "schedule|work_programme|continuation|licence_|event_" \
  | while read -r t; do
      grep -qr "$t" documentation/businessandtechnical/ || echo "UNDOCUMENTED: $t"
    done
```

That filter is deliberately loose — it will list tables these documents were never meant to
cover, such as licence positions, corrections, transactions and contacts. That is the right
trade: a name you recognise as out of scope costs a second to dismiss, whereas a narrow filter
silently misses a whole feature. `licence_event_cache` went undocumented for exactly that
reason.

Two kinds of false positive to expect, so they do not send you documenting the wrong thing:

- **Tables since renamed or dropped.** The sweep reads `CREATE TABLE` across all history, so a
  table that no longer exists under that name still appears. `event_references` is one — it
  was renamed to `schedule_events`. Check for a later `ALTER TABLE ... RENAME` before treating
  a hit as a gap.
- **Separate feature areas.** Licence positions, corrections, transactions, contacts, statuses,
  responsible organisations and teams are their own domains and are out of scope for these four
  documents. If one of them needs documenting, that is a new document and a conversation with
  the user, not an addition here.

Then read the commit subjects from the `git log` above against the claims in
"Claims most likely to go stale unnoticed" below. A commit named for a new status, a new
reminder, a calculation change or a rename is worth opening; a test-only or template-only
commit is not.

### What to do with what you find

| Finding | Action |
|---|---|
| Drift in the same area you are already changing | Fix it as part of the change. It is indistinguishable from your own edit anyway. |
| Small unrelated drift — a missing enum value, a column absent from the reference list | Fix it, and list it separately in your summary so the reviewer knows why the diff is wider than the ticket |
| A whole undocumented table or feature, or a long backlog of commits | Do **not** silently write a new section. Report what is missing, say roughly what documenting it involves, and ask whether to do it now or raise it separately |

Judgement call worth making explicitly: not every new table belongs in these two documents.
A table that is genuinely a new family — reminders, say — may deserve its own document rather
than a section bolted onto one of these. Say so rather than forcing it in.

Timebox this. The audit is a sweep for obvious staleness, not a line-by-line re-verification
of the whole document. If the baseline commit is old enough that the sweep turns up more than
a handful of candidates, stop and report rather than disappearing into it.

## Step 4 — work out what it invalidates

Before editing, map the change onto the documents. Most changes touch at least two places in
one document.

### Schema changes

| What you changed | What it invalidates |
|---|---|
| New table | A new subsection under the relevant family, an entry in the column reference at the end, and a node on the Mermaid ERD |
| New, renamed or dropped column | The table's block in the body **and** its entry in the "Reference: current columns per table" section — the pair that drifts apart most often |
| New, renamed or removed enum value | The value table in the enumerated values section |
| New foreign key, or a column referencing another table without one | The ERD, and the FK-enforcement table in the caveats section |
| A unique index or constraint | The relevant table block — these documents are explicit about which invariants the schema guarantees and which are enforced only in application code |
| Table or column rename | The body, plus a note that the old name existed if a reader might still query it |

### Behaviour changes

| What you changed | What it invalidates |
|---|---|
| A new status, or a new transition | The status table, the ASCII lifecycle diagram, and the per-family table of which statuses are reached |
| How a date is calculated, or a stored date becoming derived (or the reverse) | The derived-versus-stored table, and any `COALESCE` example built on it |
| What a journey writes, or a question becoming conditional | The nullability and gating description for that table — these documents are explicit that most nulls mean "never asked" rather than "unanswered" |
| Which options or requirements are offered, and on what basis | The visibility rules table |
| Duplication behaviour — a table joining or leaving the copy | The versioning section, and the statement that application rows are never copied with the schedule |
| Anything that starts writing back to the schedule from an application | The "a decision does not change the schedule" subsection, and the caveat that repeats it — a rewrite, not a footnote |
| A reference or identifier format | The format description and the caveats around when it is assigned |
| File usage types, document templates, team scope ids | The files and letters section, and the external contributors section |
| A new deadline source, or a change to what makes a reminder due | The reminders document — the per-type deadline table, the three gates, and the notice window |
| A change to reminder uniqueness, notice period or suppression | The reminders document's uniqueness section, and the claim that a deadline surviving a schedule update is not re-reminded |
| Which events the tracker cache writes, or a second caller of the refresh | The event cache document — the refresh trigger, the table of what is written, and the list of event types never cached |
| Starting to populate a cache column that was previously always null | The event cache document's "never populated" section and its caveat — a straight deletion, not an edit |

### Claims most likely to go stale unnoticed

No test fails when these stop being true. If your change goes near one, re-read it:

- **"Not stored" claims** — the Maintain / Reduce / Extend choice and the request-purpose
  options are documented as having no column of their own. Adding one makes those sections
  actively wrong.
- **"Always 1" and "at most one" claims** — `version_number`, and one `ACTIVE` schedule per
  licence. Exactly the kind of thing a new feature quietly changes.
- **"Enforced in application code, not by a database constraint"** — if you add the
  constraint, the caveat becomes wrong in the reader's favour and should be deleted.
- **The documented naming inconsistencies** — `created_datetime` versus `created_date_time`,
  the three spellings of the application-detail foreign key. If a migration normalises one,
  delete the caveat rather than leave it misleading.
- **"Never populated" claims** — the event cache document states that `quad_block`,
  `steward_wua_id`, `application_id` and `application_type` are always null. The moment
  anything writes one, a whole section and a caveat become wrong in the reader's favour.
- **"Only one caller" claims** — the event cache is documented as refreshed solely when a
  schedule version is applied. A second caller changes the staleness story entirely.
- **"Never cached" and "not sent" claims** — rates, other schedule events and expiry are
  documented as absent from the tracker cache; reminders are documented as suppressed for any
  licence that is not `EXTANT`. Both are easy to change without noticing the document.

If a change introduces a family of tables or a journey that fits none of the documents that
exist, start a new document in the same directory with the same shape rather than bolting it
on — then add it to the table at the top of this skill, or say that it needs adding.

## Step 5 — edit the documents

They have a consistent voice and structure. Match it.

- **Write for someone with no access to the codebase.** Never name a Java class, package,
  service or method — that rule applies to the documents, not to this skill. Say
  `work_programme_activities.due_date`, not `WorkProgrammeActivity.dueDate`. Where a rule
  exists only in application code, state it as a fact about the data — "enforced in
  application code, not by a database constraint" — without naming what enforces it.
- **Describe behaviour as observable outcomes in the data**, not as a call sequence. "Every
  child row is copied field-for-field, each with a new id" is useful to a reader with only
  SQL; "the duplication service iterates the repositories" is not.
- **Use live PostgreSQL names**: the names as they stand after every migration has run, not
  the name in the `CREATE TABLE` that a later migration renamed.
- **Document enums as value tables** — stored value, display string, and when it applies.
  Keep the framing that the list describes what the service writes rather than what the
  schema guarantees; that is what tells a reader an unexpected value is a data-quality
  signal, not a gap in the document.
- **Give gating columns a table**, spelling out what to expect on both branches.
- **Put anything that would mislead a query author in the caveats section**, not only in the
  body. That section is what people read after getting a surprising result.
- **Cross-reference by section number** (`§7.3`), the way the existing text does.
- **Keep the column reference at the end complete**, not a summary. It is what people diff
  against the database.

### Mermaid ERD

Both documents carry one `flowchart TB` entity relationship diagram, preceded by the line
"Read top to bottom: each layer is keyed on the layer above it." Keep the styling consistent
across both:

- Plain nodes declared first, together, in top-to-bottom reading order; subgraphs after.
- Node labels are `<b>table_name</b><br/>key · columns<br/><i>short note</i>`.
- Subgraphs group tables sharing a parent, use `direction LR`, and their title names the
  keying column.
- Solid edges labelled `"1 : 1"` or `"1 : many"` for ownership; dotted (`-.` `.->`) for a
  reference that is not ownership.
- Leave detail out of the diagram rather than adding a long edge back up the hierarchy — the
  body covers it.

### What not to document

- Controller routes, journey step order, validation messages, templates. These documents
  describe the data and the rules that shape it, not the interface that collects it. A new
  page in an existing journey is only documented if it changes what ends up stored.
- Anything a reader could derive from the schema itself. The value of these documents is the
  behaviour the schema does *not* express: what is calculated versus stored, which invariants
  live in application code, which nulls are meaningful, and which references cross a schedule
  version boundary.

## Step 6 — check before you finish

1. For every table you touched, confirm the column reference section lists exactly the
   columns the migrations produce:

   ```bash
   grep -rn "<table_name>" src/main/resources/db/migration/
   ```

   Read every hit, in version order — a column is often added or renamed several migrations
   after the `CREATE TABLE`.
2. For every behaviour you changed, re-read the section that describes it end to end rather
   than patching one sentence. These claims are usually stated twice — once in the body and
   once in the caveats — and fixing only the first leaves the contradiction in the place
   people actually look.
3. Confirm the ERD still renders and that any new table appears on it.
4. If Step 1 found a document this skill does not list, or you created one, say so plainly in
   your summary — the table at the top of this skill needs the new row, and nothing else will
   catch it.
5. Confirm the documentation edit is staged alongside the code change, not left uncommitted.
