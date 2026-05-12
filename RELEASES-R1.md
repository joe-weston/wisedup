# RELEASES.md — Release 1: The Build Loop
> Read MISSION.md before this document.
> This release has one outcome: a senior dev approves a story and receives
> a working preview URL and a plain-English QA checklist with zero manual work in between.
> Do not begin Release 2 until every item in the Definition of Done is verified.

---

## Release Goal

A senior dev creates or receives a user story, approves it in the dashboard,
and the Ruflo agent swarm handles everything from that point forward:
planning the work, building the feature, writing and running tests,
opening a pull request, and posting back a preview URL and QA checklist
to the story card.

The human's role in this release is: write the story, click approve, review the result.
Nothing else.

---

## What This Release Is Not

Release 1 is deliberately narrow. The following are explicitly out of scope
and will be addressed in later releases:

- Automatic story creation from meetings (Release 2)
- ClickUp synchronization (Release 4)
- Authentication and multi-tenancy (Release 5)
- Client-facing views or portals (Release 5)
- Mermaid architecture diagrams (Release 4)
- Environment tier switching — staging branch or isolated Supabase projects (Release 3)
- The studio chat interface for polish edits (Release 3)

Everything in Release 1 runs against a single agency, a single client,
and a single project. There is no login. There is no access control beyond
a project-level API key used by the workspace to call the platform.

---

## Personas

**The Senior Dev (primary user of Release 1)**
Understands the project and the client. Creates stories manually this release.
Approves stories when they are ready to build. Reviews the preview and QA checklist
to decide whether to merge or request changes. Does not want to do anything
that a Ruflo agent can do instead.

**The Junior Dev (secondary user of Release 1)**
Runs the Ruflo workspace locally. Monitors the build pipeline.
Handles escalations when the agent swarm gets stuck. Does not need to understand
the full codebase to operate the workspace.

---

## User Stories

### Story 1.1 — Create a story manually
As a senior dev, I want to create a user story in the dashboard with a title,
description, acceptance criteria, and a note about where the story came from,
so that the story has a traceable origin before it enters the build pipeline.

Acceptance criteria:
- I can create a story from the project dashboard
- I must provide a title
- I can optionally provide a description and acceptance criteria
- I must select a source type (manual, SOW clause reference, or other)
- I can add a free-text note explaining the origin
- The story is created with status: drafted
- The story appears immediately on the story board in the drafted column

---

### Story 1.2 — View stories on a Kanban board
As a senior dev, I want to see all stories for a project organized by their
current status, so that I can understand what is in progress and what needs
my attention at a glance.

Acceptance criteria:
- The board shows all eight status columns in lifecycle order:
  drafted, queued, in_dev, in_review, needs_client, staging, approved, shipped
- Each story card shows the title, current status, and source origin badge
- Story cards show a preview URL link when one has been set
- Story cards show a PR link when one has been set
- The board updates in real time when a story's status changes
  without requiring a page refresh
- I can click a story card to open the full story detail

---

### Story 1.3 — Approve a story for build
As a senior dev, I want to approve a story so that the Ruflo agent swarm
begins building it immediately, without me having to trigger anything manually
beyond the approval click.

Acceptance criteria:
- I can approve a story from the story detail page
- The Approve for Build button is only visible when the story is in
  drafted or queued status
- On approval the story status updates to queued
- The Ruflo workspace build workflow is triggered automatically
- I do not need to open a terminal or run a command for the build to start
- The story card updates to in_dev when the agent swarm begins work

---

### Story 1.4 — Receive a preview URL on the story card
As a senior dev, I want the Vercel preview URL to appear on the story card
automatically after the agents build the feature and Vercel deploys it,
so that I can click through to the live preview without searching for it.

Acceptance criteria:
- The preview URL appears on the story card without any manual action from me
- The story card updates in real time when the URL becomes available
- I can click the preview URL to open the Vercel preview in a new tab
- The story detail page shows a preview iframe rendering the Vercel preview
- The preview URL reflects the feature branch deployment, not the main branch

---

### Story 1.5 — Receive a QA checklist on the story card
As a senior dev, I want a plain-English QA checklist to appear on the story card
after the agents finish building, so that I know exactly what to verify
without having to read the code or write my own test plan.

Acceptance criteria:
- The QA checklist appears on the story card automatically after the build completes
- The checklist is written in plain language that a non-developer can follow
- The checklist includes step-by-step instructions for verifying the primary user flow
- The checklist identifies edge cases that should be manually verified
- The checklist states which environment was tested
- The checklist includes explicit pass and fail criteria for each item
- The checklist renders as interactive checkboxes in the dashboard
- The checklist notes any known limitations or follow-up stories needed

---

### Story 1.6 — View the full story status history
As a senior dev, I want to see a timeline of every status change on a story
including who or what triggered each change, so that I can understand exactly
what happened during the build and where time was spent.

Acceptance criteria:
- The story detail page shows a chronological timeline of all status changes
- Each entry shows the status it moved from, the status it moved to,
  the timestamp, and the actor (agent name, human name, or webhook source)
- Entries are human-readable, not raw database records
- The timeline is visible without scrolling past the story's primary content

---

### Story 1.7 — Review and approve a migration proposal
As a senior dev, I want to see any database schema changes the agents propose
before they are run, and I want to explicitly approve or reject them,
so that no schema change ever touches the database without my knowledge.

Acceptance criteria:
- When an agent identifies a required schema change it stops building
  and posts a proposal to the dashboard rather than running the migration
- The story card shows a pending migration proposal badge when one is waiting
- I can view the proposed SQL and a plain-English explanation of what it changes
- I can see which tables are affected
- I can approve or reject the proposal from the dashboard
- On approval the agent resumes building automatically
- On rejection the story status moves to needs_client with a note
- A migration is never run without my explicit approval

---

### Story 1.8 — View the PR opened by the agents
As a senior dev, I want to see the GitHub pull request the agents opened
for a story, so that I can review the code changes before merging.

Acceptance criteria:
- The story card shows the PR link after the git agent opens it
- The PR link opens the correct GitHub pull request in a new tab
- The PR description contains the story title, description,
  acceptance criteria, list of changed files, and test results summary
- The branch name follows the pattern feature/story-{id}-{slug}

---

## Data Requirements

The following information must be stored and remain queryable for every story.
Agents must write this data to Supabase — not only to local agent memory.

**Per story:**
- Unique ID, project reference, title, description, acceptance criteria
- Current status and when it last changed and who changed it
- Source reference: where this story came from (type and ID)
- GitHub branch name and PR URL
- Vercel preview URL and which environment it points to
- QA checklist content and test results
- ClickUp task ID (nullable, used in Release 4)

**Per status change:**
- Story reference
- Previous status and new status
- Timestamp
- Actor type: human, agent, or webhook
- Actor identity: user ID, agent name, or webhook source name
- Optional note explaining the transition

**Per migration proposal:**
- Story reference (nullable for standalone proposals)
- Full SQL content
- Plain-English explanation
- List of affected tables
- Per-tier run instructions
- Status: pending, approved, rejected, or applied
- Who proposed it and when
- Who approved or rejected it and when
- When it was applied

**Traceability rule (non-negotiable):**
Every story must have a source_ref_type set before it enters the build pipeline.
Stories with source_ref_type of manual must have a source_ref_note.
The agents must enforce this rule and refuse to build a story without it.

---

## Agent Behavior Requirements

The following describe what the Ruflo agent swarm must do during a build.
These are behavioral requirements, not implementation instructions.
Agents choose their own implementation approach.

**Architect agent**
Must read the full story including acceptance criteria before planning anything.
Must read the existing project codebase structure before producing a task plan.
Must identify explicitly whether a schema change is required.
Must not begin planning a story that has no acceptance criteria.

**Coder agent**
Must read the architect's task plan completely before writing any code.
Must follow existing project conventions rather than inventing new patterns.
Must stop immediately if a schema change is needed and invoke the migration agent.
Must not mark its work complete if the migration has not been approved.

**Migration agent**
Must produce a plain-English explanation alongside every SQL proposal.
Must include per-tier run instructions in every proposal.
Must post the proposal to the platform API and then stop.
Must never execute a migration. Must never proceed past the proposal step.

**Tester agent**
Must write tests that verify the acceptance criteria specifically, not just code coverage.
Must include at least one end-to-end test that covers the primary user flow.
Must run all tests and confirm they pass before proceeding.
Must return to the coder agent if tests fail, up to two retry cycles.
Must not mark testing complete with any failing tests.

**QA agent**
Must write the checklist in language a non-developer can understand.
Must base the checklist on the acceptance criteria, not on the code.
Must state which environment was tested on every checklist.
Must include pass and fail criteria for each item, not just instructions.
Must not copy test output verbatim into the checklist — translate it into human language.

**Git agent**
Must include story ID, description, acceptance criteria, and test summary in every PR.
Must use the branch naming pattern feature/story-{id}-{slug}.
Must not merge any branch without explicit human approval.
Must not force push to any branch.

**All agents**
Must post a status update to the platform API at every lifecycle transition.
Must not assume the previous agent's work is complete — verify by reading outputs.
Must escalate to the operator rather than retrying more than twice on any failure.
Must never commit secrets, tokens, API keys, or credentials to any file.

---

## Environment Requirements

**The client project** (the Next.js app being built for the client):
- One Supabase project used for all environments in Release 1
- Local development uses .env.local pointing at this project
- Vercel preview deployments also use this project
- This is Environment Tier 1 — simple, one connection string

**The agency-flow platform** (the dashboard where stories are managed):
- Separate Supabase project — never shared with any client project
- Deployed to Vercel
- Receives webhooks from Vercel deployments of client projects
- Exposes API routes that the workspace agents call

**Credentials management in Release 1:**
- Per-project credentials stored in .env files
- .env files are never committed to any repository
- .env.example documents every required variable with a description
- Migration to a secrets manager is planned for Release 5

---

## Integration Requirements

**GitHub**
- The workspace must be able to create branches, commit code, and open PRs
- PR descriptions must follow the format defined in agent behavior requirements
- Branch names must follow the pattern feature/story-{id}-{slug}
- No direct commits to main or staging — all changes via PR

**Vercel**
- Vercel must be configured to deploy preview builds from feature branches
- Vercel must send a deployment webhook to the platform when a deploy completes
- The platform webhook handler must parse the branch name to identify the story
- The preview URL must be written to the story card automatically

**Platform API**
- The workspace communicates with the platform exclusively via the REST API
- All requests carry the project API key in the Authorization header
- The platform must validate the API key on every request before doing anything
- The workspace must call the platform to update story status at every transition
- The workspace must never write directly to the platform Supabase database

---

## QA and Testing Requirements

The following must be true for Release 1 to be considered complete.

**Platform API**
- Every API route has a test covering the happy path
- Every API route has a test confirming it rejects requests with no API key
- Every API route has a test confirming it rejects requests with an invalid API key
- Status update tests confirm the transition is written to story_status_history
- Migration proposal tests confirm the proposal is created with status pending

**Dashboard UI**
- The story board renders all eight status columns
- Stories move between columns in real time without page refresh
- Manual story creation saves correctly and the story appears on the board
- The Approve for Build button triggers the workspace webhook
- The preview iframe loads when a preview URL is set
- The QA checklist renders as interactive checkboxes
- The migration proposal approve and reject buttons work correctly
- The status history timeline renders in chronological order

**Ruflo Workspace**
- The session start protocol fetches project context and queued stories
- /build:story runs the full swarm in correct sequence for a real story
- /build:release fetches all queued stories and builds each in sequence
- The migration agent stops the workflow and posts a proposal correctly
- The workflow resumes correctly after a migration is approved
- The QA agent posts the checklist to the platform API correctly
- The git agent opens a PR with the correct description format
- The Vercel webhook correctly extracts the story ID from the branch name
- The preview URL appears on the story card after the webhook fires

**End-to-end acceptance test**
The following scenario must pass completely before Release 1 is signed off:

1. Create a story manually with title, description, acceptance criteria,
   and source_ref_type set to manual with a source note
2. Confirm story appears in drafted column on the story board
3. Click Approve for Build on the story detail page
4. Confirm story moves to in_dev on the board without page refresh
5. Wait for swarm to complete
6. Confirm PR appears on the story card with correct description
7. Confirm preview URL appears on the story card
8. Confirm QA checklist appears on the story card with checkboxes
9. Confirm status history shows all transitions with correct actor names
10. If a migration was proposed during the build:
    confirm the workflow paused, confirm the proposal is visible in the dashboard,
    approve the migration, confirm the workflow resumed and completed

---

## Definition of Done

Release 1 is complete when every item below is checked.
No item may be waived. If an item cannot be completed, it must be
resolved before Release 2 begins.

**Schema and storage**
- [ ] All tables created and migrated in the platform Supabase project
- [ ] RLS enabled on all tables
- [ ] Supabase Realtime enabled on the stories table
- [ ] Every story created during testing has a source_ref_type set

**Platform API**
- [ ] All API routes implemented and deployed
- [ ] All API routes reject unauthenticated requests
- [ ] All API routes have passing tests for happy path and auth failure
- [ ] Vercel webhook handler correctly identifies story from branch name
- [ ] Vercel webhook handler writes preview URL to correct story

**Dashboard**
- [ ] Story board renders all eight status columns
- [ ] Story cards update in real time via Supabase Realtime
- [ ] Manual story creation works including source_ref fields
- [ ] Story detail shows preview iframe, QA checklist, PR link, status history
- [ ] QA checklist renders as interactive checkboxes
- [ ] Migration proposals are visible with approve and reject actions
- [ ] Approve for Build button triggers the workspace build webhook

**Workspace**
- [ ] CLAUDE.md session start protocol runs on every session open
- [ ] /build:story runs full swarm pipeline for a single story
- [ ] /build:release fetches and builds all queued stories
- [ ] Migration agent pauses workflow and posts proposal to platform
- [ ] Workflow resumes correctly after migration approval
- [ ] All agents post status updates to platform API at every transition
- [ ] No agent ever commits a secret or credential

**End-to-end**
- [ ] Full acceptance test scenario passes from story creation to preview URL
- [ ] At least one story with a migration proposal has been built and verified
- [ ] QA checklists produced are readable by a non-developer
- [ ] All tests pass in CI before release is signed off
