---
name: generate-jira-test-steps
description: >
  How to draft manual testing steps for the changes on this branch. Use when asked to
  "add test details", "write test steps", "how do I test this ticket", or "UAT steps".
---

# Jira test steps

Manual testers on this project write UAT walkthroughs as a numbered list on the Jira
ticket: which user to log in as, which pages to click through, and — critically — not
just the happy path but the validation errors too. Your job is to draft that same kind
of walkthrough from the code, so the developer doesn't have to reconstruct it from
memory right after finishing the feature.

The output is a **draft to review, not something to post automatically**. Print it in
the chat. Only post it to Jira as a comment if the user explicitly asks — see
"Posting to Jira" at the end.

## 1. Find the ticket

This repo's branches follow `feature/<JIRA-KEY>/short-description` (see the current
branch with `git branch --show-current`). Extract the key from that pattern. If the
branch doesn't match — no `feature/` prefix, or a name that isn't a real Jira key —
ask the user for the ticket key rather than guessing.

## 2. Read the ticket

Use the Atlassian MCP tools (`getAccessibleAtlassianResources` for the cloud ID, then
`getJiraIssue`) to fetch the ticket's summary, description, and comments.

Sub-tasks in this project often have an **empty description** — the acceptance
criteria live on the parent story instead (fetch it via the `parent` field). If the
ticket's own description is empty or thin, pull the parent's description and any
relevant existing UAT comments on it too — they often show exactly what pages/roles
were used for sibling tickets in the same story, which is a strong hint for this one.

## 3. Find what changed on the branch

Don't assume the diff base — detect it. Try, in order, `git merge-base HEAD
origin/develop`, `git merge-base HEAD develop`, falling back to other common
integration branch names if neither exists, then diff from that merge-base to `HEAD`.
Guessing a fixed branch name is fragile since it silently breaks the day someone's
local `develop` is stale or missing.

Look specifically for:

- **Controllers** (`*Controller.java`) — the `@GetMapping`/`@PostMapping` paths tell
  you what pages/actions exist. The access-control annotations on the class or method
  (`@HasAnyRole(roles = {...})`, plus any of the other `AccessInterceptorRule`
  annotations described in this repo's CLAUDE.md, e.g. status-gating annotations) tell
  you who can reach it and in what state.
- **Form + Validator pairs** — the `Validator.validate()` logic is your source for
  which fields are required and what the error messages say. This is exactly what
  turns "fill in the form" into a concrete validation-error step.
- **Freemarker templates** (`.ftl`) — read these for the actual link text, button
  labels, and page headings, so your steps say "click 'Add a change'" rather than
  inventing a plausible-sounding label. Testers navigate by what's on screen, not by
  URL paths — so describe navigation the same way ("go to the Licence positions and
  transactions work area", not `/lms/licence-positions`).

If the diff has no user-facing controller/template changes at all (e.g. it's a pure
refactor or backend-only fix), say so plainly and don't force a fabricated UI
walkthrough — suggest what actually would demonstrate the fix instead (e.g. "covered
by the automated tests, no manual UI path").

## 4. Pick the right login user(s)

`src/main/java/uk/co/nstauthority/licensingmanagementservice/internalonly/DataBootstrapper.java`
seeds team/role membership for a fixed set of `@lms.co.uk` accounts (it does not
create the accounts — those already exist against the Energy Portal IDP, so any of
these emails can actually be signed into). Grep this file fresh each time rather than
relying on a remembered list — roles get added and renamed. Cross-reference the
`Role` enum
(`src/main/java/uk/co/nstauthority/licensingmanagementservice/teams/Role.java`) if a
role name on a `@HasAnyRole` annotation isn't self-explanatory.

Match the role(s) required by the changed controller(s) to the bootstrapped user that
holds that role. If a journey spans multiple personas (e.g. one role submits, a
different role approves), write it as separate numbered sections, each starting with
its own "Log in as ...".

There is no dev login bypass — these are real SAML accounts against the Energy Portal
IDP, so "log in as X" in the output means literally signing in with that account's
credentials, same as the EPGF-176 example below.

## 5. Write the steps

Match this style — short, numbered, imperative, one action per line, concrete UI
labels, happy path first, validation second:

```
1. Log in as administrator@lms.co.uk
2. Go to the Licence positions & transactions work area
3. Select two new licences
4. Start a correction on one of the new licences
5. Select an existing position
6. Add a change
7. Select "Partial surrender"
8. Select any number of blocks
9. You should be redirected to the correction page and see the partial surrender details
10. Try to add another partial surrender — you should be prevented by a validation error
```

Guidelines:

- Lead with the login step, every time — it's the first thing a tester needs and the
  example ticket (EPGF-176) always starts there.
- Cover the happy path completely before moving to validation — don't interleave them,
  it's harder to follow.
- For validation, pick the checks that are actually non-obvious from the form alone:
  required-field omissions, business-rule errors caught in the validator (e.g. "can't
  add a second partial surrender to the same correction"), not generic "try submitting
  an empty form" filler.
- Skip screenshots — you can't produce them, and the numbered actions should be
  unambiguous without one.
- Keep it short. If the change is small, the walkthrough should be too — a dozen
  padded steps for a one-field tweak is worse than five sharp ones.

## Posting to Jira

Default to just printing the draft in the chat for review. Only write it back to Jira
if the user explicitly asks (e.g. "post that as a comment", "add it to the ticket").
When they do, confirm the ticket key first, then use ToolSearch to find the Atlassian
MCP tool for adding a comment to a Jira issue — posting a comment is visible to
everyone watching the ticket, so don't do it speculatively.
