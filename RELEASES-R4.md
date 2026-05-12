# RELEASES.md — Release 4: The Weekly Cycle
> Read MISSION.md and RELEASES-R1.md through RELEASES-R3.md before this document.
> Releases 1, 2, and 3 must be fully complete before Release 4 begins.
> This release has one outcome: the full weekly client engagement cycle
> runs end-to-end with minimal manual effort — from meeting to stories
> to build to release summary to the next meeting agenda.

---

## Release Goal

By the end of Release 3 the build loop works, stories come from meetings
automatically, and polish happens in the studio. But the weekly rhythm
around all of that is still manual. The senior dev still has to remember
to prep for Monday's meeting. They still have to summarize what shipped
for the client. They still have to keep ClickUp in sync by hand.
They still have no visual map of where the project stands architecturally.

Release 4 closes all of that. After this release the weekly cycle looks like:

  Monday meeting ends — Fathom fires — debrief notes generated automatically
  Tuesday — stories are drafted, senior dev approves, build loop fires
  Wednesday and Thursday — agents build, studio polishes
  Friday — release summary generated, agenda for next week drafted,
            ClickUp is already in sync, Mermaid diagrams reflect current state

The senior dev's weekly overhead drops to: approve stories, review previews,
send the release summary to the client.

---

## What This Release Is Not

- No multi-tenancy or authentication — that is Release 5
- No client-facing login or portal — that is Release 5
- No billing or usage tracking — that is Release 6
- No white-label options — that is Release 6
- ClickUp sync in this release is one Ruflo workspace command and
  a webhook receiver — it is not a full bidirectional real-time integration.
  That level of polish comes in Release 6.

---

## Personas

**The Senior Dev (primary user of Release 4)**
Runs the weekly cycle for one or more client projects.
Wants to spend Monday reviewing auto-generated debrief notes,
Tuesday approving stories, Friday reviewing the auto-generated release summary
before sending it to the client.
Does not want to write any of those documents manually.
Does not want to update ClickUp manually after every story status change.

**The Junior Dev (secondary user of Release 4)**
May be asked to run the meeting prep or release summary commands
before the senior dev reviews them.
Uses the Mermaid diagrams to understand where the project stands
without needing to read the full codebase or all meeting notes.

**The Client (indirect, Release 4 awareness only)**
Receives the release summary document.
Is not logging into the platform yet — that is Release 5.
The release summary is a document the senior dev sends manually for now.

---

## User Stories

### Story 4.1 — Group stories into a weekly release
As a senior dev, I want stories to be grouped into a named weekly release
so that I can track what shipped in a given week and generate a coherent
summary for the client.

Acceptance criteria:
- Every project has a concept of a current open release
- New stories are automatically assigned to the current open release
  when they are created or approved
- A release has a name, a week start date, a week end date, and a status
- Release statuses are: planning, in_progress, shipped, and archived
- I can create a new release manually from the dashboard
- The platform automatically creates the next release when the current one
  is marked as shipped
- I can see all stories in a release grouped by their current status
- I can see which releases are open, shipped, and archived from the project view
- Stories can be moved between releases manually if needed

---

### Story 4.2 — Generate post-meeting debrief notes automatically
As a senior dev, I want post-meeting debrief notes generated automatically
after every client meeting, so that I do not have to write them myself
and I have a clear record of what was decided and what needs to happen next.

Acceptance criteria:
- The debrief command can be triggered manually after a meeting
- The debrief command is also triggered automatically when a Fathom
  webhook is received for a project, after stories have been generated
- The debrief reads the full meeting transcript and recent project context
- The output identifies decisions made in the meeting explicitly
- The output identifies commitments given by either party
- The output identifies blockers raised that are not yet resolved
- The output identifies open questions that were not answered in the meeting
- New stories are created for any commitments not already covered
  by a drafted or existing story
- Existing stories mentioned as blocked in the meeting are updated
  to needs_client status with a note from the debrief
- The debrief document is saved to the project documents in the platform
- The debrief is readable by someone who was not in the meeting

---

### Story 4.3 — Generate a pre-meeting agenda automatically
As a senior dev, I want a meeting agenda generated before every client meeting
that reflects the current state of the project, so that I walk into the meeting
prepared and the client knows what we will cover.

Acceptance criteria:
- The meeting prep command can be triggered manually before any meeting
- The output is structured as a numbered agenda with clear sections
- Section one covers what shipped since the last meeting with preview links
- Section two covers stories currently in staging that need client approval
- Section three covers stories in needs_client status that require client input
- Section four covers open questions carried over from the last debrief
- Section five covers what is planned for the coming sprint
- Each item in the agenda includes enough context that the client
  understands it without needing to look anything up
- Preview links in the agenda point to the staging or production URL,
  not the feature branch URL
- The agenda document is saved to the project documents in the platform
- The agenda is readable by the client without technical knowledge

---

### Story 4.4 — Generate a client-facing release summary
As a senior dev, I want a release summary generated for the current release
that I can send directly to the client, so that I do not have to write
a status update after every sprint.

Acceptance criteria:
- The release summary command can be triggered from the release detail screen
  or via slash command in the workspace
- The output is written in plain language with no technical jargon
- The output covers every story shipped in the release
- Each story is described in terms of what the client can now do,
  not what was built technically
- Preview or production links are included for each shipped story
- The output includes a section covering what is coming in the next sprint
- The output includes any action items the client needs to complete
  before the next meeting
- The release summary is saved to the release record in the platform
- The release summary is viewable from the release detail screen
- The release is marked as shipped when the summary is generated
  if all stories in the release are in shipped or approved status

---

### Story 4.5 — Generate a Mermaid diagram of the current state architecture
As a senior dev, I want a diagram of the client's current system state
generated from the accumulated meeting transcripts and SOW documents,
so that I have a visual reference for architecture discussions and
onboarding new team members.

Acceptance criteria:
- The map current state command reads all meeting transcripts,
  SOW clauses, and project documents for the project
- The output is a valid Mermaid flowchart diagram
- The diagram represents systems, processes, data flows,
  and integrations mentioned across the source material
- Nodes that correspond to stories already shipped are visually annotated
- The diagram is saved as a document in the project with a date stamp
- The diagram renders correctly in the platform document viewer
- Running the command again on a later date produces a new versioned diagram
  without overwriting the previous one
- The diagram is accurate to the source material — it does not invent
  systems or connections that were not mentioned

---

### Story 4.6 — Generate a Mermaid diagram of the target state architecture
As a senior dev, I want a diagram of the target system state generated
from the stories and SOW objectives, so that the client and team can
see where the project is heading and how far along we are.

Acceptance criteria:
- The map target state command reads all stories regardless of status,
  all SOW clauses, and all project documents
- The output is a valid Mermaid flowchart diagram
- The diagram represents the intended architecture when all planned work is complete
- Nodes are color-coded or annotated to indicate status:
  shipped stories, stories in progress, and stories not yet started
  are visually distinguished
- The diagram is saved as a document in the project with a date stamp
- The diagram renders correctly in the platform document viewer
- Running the command again produces a new versioned diagram
- The diagram is accurate to the scope defined in the SOW and stories —
  it does not add scope that was not discussed

---

### Story 4.7 — Sync stories to ClickUp as tasks
As a senior dev, I want stories in Agency Flow to appear as tasks in ClickUp
so that the client and other stakeholders who live in ClickUp
can see what is being worked on without logging into Agency Flow.

Acceptance criteria:
- The ClickUp sync command creates a ClickUp task for every story
  in the current project that does not already have a ClickUp task ID
- Each task is created in the configured ClickUp list for the project
- The task title matches the story title
- The task description includes the story description and acceptance criteria
- The task status in ClickUp reflects the current story status
  using the project's configured status mapping
- The story record in Agency Flow is updated with the ClickUp task ID
  after the task is created
- Running sync a second time does not create duplicate tasks
- Stories that already have a ClickUp task ID are updated, not recreated
- The sync command reports how many tasks were created, updated,
  and skipped as duplicates

---

### Story 4.8 — Receive ClickUp status updates back into Agency Flow
As a senior dev, I want status changes made in ClickUp to be reflected
in Agency Flow so that when a team member updates a task in ClickUp
the story status stays in sync without manual effort.

Acceptance criteria:
- The platform exposes a webhook endpoint that ClickUp can be configured to call
- When a ClickUp task status changes the corresponding story status
  is updated in Agency Flow using the project's configured status mapping
- The status change is logged in story_status_history with actor_type webhook
  and the ClickUp task ID as the actor reference
- If a ClickUp status does not map to a known story status
  the change is logged as a note but the story status is not changed
- Comments added to a ClickUp task are logged as notes on the story
- The webhook endpoint is idempotent and handles ClickUp redeliveries gracefully
- ClickUp configuration per project includes the API token,
  the list ID, and the status mapping

---

### Story 4.9 — View a release detail screen
As a senior dev, I want a dedicated screen for each release that shows
all stories, their statuses, the release summary, and quick access
to the meeting prep and debrief documents, so that I have a single
place to manage each week's work.

Acceptance criteria:
- There is a release detail screen accessible from the project navigation
- The screen shows the release name, dates, status, and story count
- Stories are grouped by their current status on this screen
- I can see the release summary when it has been generated
- I can trigger the release summary generation from this screen
- I can see links to the debrief and meeting prep documents
  associated with this release's week
- I can manually move a story to a different release from this screen
- I can mark the release as shipped from this screen when all stories are done
- I can see all past releases from the project navigation

---

## Data Requirements

The following must be stored permanently in Supabase.
All release and cycle data must survive session restarts.

**Per release:**
- Unique ID and project reference
- Name, week start date, week end date
- Status: planning, in_progress, shipped, archived
- Generated release notes content
- Generated client summary content
- Timestamps for status changes

**Story additions for Release 4:**
- Release reference on every story
- Stories without a release reference at the start of Release 4
  must be assigned to the earliest open release during migration

**Per ClickUp configuration on a project:**
- ClickUp API token stored securely
- ClickUp list ID
- Status mapping stored as a key-value structure:
  Agency Flow story status maps to a ClickUp status name and vice versa
- ClickUp webhook secret for inbound signature validation

**Per ClickUp sync event:**
- Story reference
- ClickUp task ID
- Direction: outbound (Agency Flow to ClickUp) or inbound (ClickUp to Agency Flow)
- What changed and when
- Stored as story_status_history entries with actor_type webhook
  or actor_type agent as appropriate

**Generated documents:**
- All generated documents (debriefs, agendas, release summaries, diagrams)
  are stored in the documents table with doc_type set to the appropriate type
- Each document is associated with the project and optionally a release
- Documents are versioned by date — new runs produce new records,
  prior documents are retained

---

## Agent Behavior Requirements

**Debrief agent**
Must read the full transcript of the meeting being debriefed, not a summary.
Must read the last debrief document if one exists to understand open questions
carried over from prior meetings.
Must read all stories currently in needs_client status before writing.
Must identify decisions, commitments, blockers, and open questions explicitly
as distinct categories in the output — not mixed together in prose.
Must create drafted stories for commitments not already covered.
Must update story status to needs_client for stories the meeting identified
as blocked, and include a note explaining why.
Must not invent content that was not in the transcript.
Must write in plain language that a non-developer can read.

**Meeting prep agent**
Must read the debrief from the most recent meeting before writing the agenda.
Must fetch all stories shipped since the last meeting with their preview links.
Must fetch all stories currently in staging.
Must fetch all stories currently in needs_client.
Must not include stories in drafted or queued status in the agenda —
those are internal and not ready for client discussion.
Must use staging or production URLs in the agenda, not feature branch URLs.
Must write in plain language the client can understand without technical context.
Must not exceed a length that would make the agenda unusable in a one-hour meeting.

**Release summary agent**
Must read all shipped stories in the release before writing.
Must describe each story from the client's perspective — what they can now do —
not from the developer's perspective of what was built.
Must include a preview or production link for every story where one exists.
Must include a clear next steps section covering the coming sprint.
Must include any action items the client needs to complete.
Must not include stories that are not in shipped or approved status.
Must write at a level appropriate for a business stakeholder,
not a technical audience.

**Mermaid diagram agent**
Must read all meeting transcripts, SOW clauses, and project documents
before generating any diagram.
Must not invent systems, integrations, or relationships
that were not mentioned in the source material.
Must produce valid Mermaid syntax that renders without errors.
Must annotate the diagram to reflect the current state of stories
using a consistent visual convention.
Must produce a new versioned document rather than overwriting a prior diagram.

**ClickUp sync agent**
Must check for existing ClickUp task IDs before creating any task
to prevent duplicates.
Must map story statuses to ClickUp statuses using the project configuration,
not hardcoded assumptions.
Must report clearly on what was created, updated, and skipped.
Must not fail silently — any ClickUp API error must be logged and reported.

**All agents in Release 4**
Must write all generated documents to Supabase via the platform API
before ending the session.
Must not assume prior session context — seed from Supabase at session start.

---

## Integration Requirements

**ClickUp**
- Each project requires a ClickUp API token, list ID, and status mapping
  configured in project settings before sync can run
- The ClickUp webhook URL must be configurable per project in platform settings
- The platform exposes a webhook endpoint for inbound ClickUp events
- The webhook validates the ClickUp signature on every request
- The endpoint handles ClickUp redeliveries idempotently

**Fathom integration extension**
- The existing Fathom webhook from Release 2 is extended to also trigger
  the debrief agent after story generation completes
- The debrief trigger is async and does not block the Fathom webhook response

**Platform API additions for Release 4**
The workspace communicates with the platform via the existing API pattern.
New capabilities needed:

- Create and retrieve releases for a project
- Assign a story to a release
- Mark a release as shipped
- Store a generated document against a project or release
- Retrieve documents by type for a project
- Retrieve the ClickUp configuration for a project
- Record a ClickUp sync event on a story
- Receive inbound ClickUp webhook events

---

## QA and Testing Requirements

**Release grouping**
- Test that a new story is automatically assigned to the current open release
- Test that a project with no open release creates one automatically
- Test that manually moving a story between releases updates the release reference
- Test that marking a release as shipped triggers creation of the next release
- Test that the release detail screen shows stories grouped by status correctly

**Debrief agent**
- Test that the debrief command reads the correct meeting transcript
- Test that decisions, commitments, blockers, and open questions
  are each identified as distinct categories in the output
- Test that a commitment not covered by an existing story produces a new drafted story
- Test that a story identified as blocked in the meeting is updated to needs_client
- Test that the debrief document is saved to the platform correctly
- Test that the debrief does not invent content absent from the transcript
- Test that the debrief is triggered automatically after a Fathom webhook

**Meeting prep agent**
- Test that the agenda includes only stories in shipped, staging, or needs_client status
- Test that stories in drafted or queued status do not appear in the agenda
- Test that preview links in the agenda point to staging or production URLs
- Test that open questions from the last debrief appear in the agenda
- Test that the agenda document is saved to the platform correctly

**Release summary agent**
- Test that only shipped and approved stories appear in the summary
- Test that each story is described from the client perspective
- Test that preview and production links are included where available
- Test that the summary is saved to the release record in the platform
- Test that the release is marked as shipped when all stories are complete
  and the summary is generated

**Mermaid diagram agent**
- Test that the generated Mermaid syntax is valid and renders without errors
- Test that the diagram does not include systems not mentioned in source material
- Test that shipped stories are annotated correctly in the diagram
- Test that running the command twice produces two versioned documents,
  not an overwrite
- Test that the diagram is saved to the platform as a document

**ClickUp sync**
- Test that the sync command creates tasks for stories without a ClickUp task ID
- Test that re-running sync does not create duplicate tasks
- Test that stories with existing ClickUp task IDs are updated not recreated
- Test that the story record is updated with the ClickUp task ID after creation
- Test that the sync report correctly counts created, updated, and skipped stories
- Test that the inbound ClickUp webhook updates story status correctly
- Test that an unmapped ClickUp status is logged as a note without changing story status
- Test that the ClickUp webhook is idempotent on redelivery
- Test that an invalid ClickUp webhook signature returns 401

**End-to-end acceptance test**
The following scenario must pass completely before Release 4 is signed off:

1. Confirm all existing stories from prior testing are assigned to a release
2. Send a Fathom webhook payload for a new meeting
3. Confirm stories are drafted from the meeting as in Release 2
4. Confirm the debrief document is generated automatically after story drafting
5. Confirm the debrief identifies at least one decision and one open question
6. Approve two stories from the meeting and confirm the build loop fires
7. After the build completes confirm stories are in in_review status
8. Merge both stories to staging via the dashboard as in Release 3
9. Run the meeting prep command and confirm the agenda includes
   the two staging stories in the client approval section
10. Confirm the agenda uses staging URLs, not feature branch URLs
11. Run the ClickUp sync command and confirm tasks are created in ClickUp
    for all stories that did not already have a ClickUp task ID
12. Update the status of one of those tasks directly in ClickUp
13. Confirm the corresponding story status updates in Agency Flow
    via the inbound ClickUp webhook
14. Mark both staging stories as shipped via the dashboard
15. Run the release summary command for the current release
16. Confirm the summary describes each shipped story from the client perspective
17. Confirm production links appear in the summary
18. Confirm the release is marked as shipped after the summary is generated
19. Run the map current state command and confirm a valid Mermaid diagram is produced
20. Run the map target state command and confirm a second valid Mermaid diagram
    is produced with shipped stories annotated differently from planned stories
21. Run the map current state command a second time and confirm
    a new versioned document is created rather than the previous one being overwritten
22. Send the same ClickUp webhook payload a second time and confirm
    no duplicate status changes or notes are created

---

## Definition of Done

Release 4 is complete when every item below is checked.
Release 5 does not begin until this list is fully verified.

**Release grouping**
- [ ] Releases table created and migrated
- [ ] New stories automatically assigned to the current open release
- [ ] Release detail screen shows all stories grouped by status
- [ ] Stories can be manually moved between releases
- [ ] Marking a release shipped triggers creation of the next release
- [ ] Past releases are accessible from the project navigation

**Debrief**
- [ ] Debrief command runs manually via slash command
- [ ] Debrief triggers automatically after Fathom webhook story generation
- [ ] Output identifies decisions, commitments, blockers, and open questions
  as distinct sections
- [ ] New stories created for uncovered commitments with correct source_ref
- [ ] Blocked stories updated to needs_client with debrief note
- [ ] Debrief document saved to platform documents correctly

**Meeting prep**
- [ ] Meeting prep command runs manually via slash command
- [ ] Agenda includes shipped stories with staging or production links
- [ ] Agenda includes staging stories needing client approval
- [ ] Agenda includes needs_client stories
- [ ] Agenda includes open questions from last debrief
- [ ] Drafted and queued stories do not appear in agenda
- [ ] Agenda document saved to platform documents correctly

**Release summary**
- [ ] Release summary command runs from workspace and release detail screen
- [ ] Summary describes each shipped story from client perspective
- [ ] Summary includes preview or production links for shipped stories
- [ ] Summary includes next sprint and client action items sections
- [ ] Summary saved to release record in platform
- [ ] Release marked shipped when summary generated and all stories complete

**Mermaid diagrams**
- [ ] Map current state command produces valid Mermaid syntax
- [ ] Map target state command produces valid Mermaid syntax
- [ ] Shipped stories annotated differently from planned stories in target diagram
- [ ] Both commands produce new versioned documents on subsequent runs
- [ ] Diagrams render in the platform document viewer without errors
- [ ] Diagrams do not contain content absent from source material

**ClickUp**
- [ ] ClickUp configuration fields available in project settings
- [ ] Sync command creates tasks for stories without ClickUp task IDs
- [ ] Sync is idempotent — no duplicates on re-run
- [ ] Story records updated with ClickUp task IDs after creation
- [ ] Inbound ClickUp webhook updates story status using configured mapping
- [ ] Unmapped ClickUp statuses logged as notes without changing story status
- [ ] ClickUp webhook validates signature and handles redelivery idempotently

**End-to-end**
- [ ] Full acceptance test scenario passes all twenty-two steps
- [ ] At least one full weekly cycle has been run end-to-end:
  meeting webhook, debrief, story approval, build, staging, release summary
- [ ] ClickUp sync verified with a real ClickUp workspace
- [ ] Both Mermaid diagrams reviewed and confirmed accurate to source material
- [ ] All tests pass in CI before release is signed off
