# RELEASES.md — Release 2: The Flywheel Input
> Read MISSION.md and RELEASES-R1.md before this document.
> Release 1 must be fully complete before Release 2 begins.
> This release has one outcome: a client meeting ends and user stories
> appear in the dashboard automatically, sourced and ready to approve.
> The senior dev's only job is to review and click approve.

---

## Release Goal

Remove the last significant manual step from the weekly cycle.
After Release 2, a client meeting captured by Fathom produces drafted user stories
in the dashboard without any human intervention between the meeting ending
and the stories appearing. Each story is linked back to the exact meeting turn
or SOW clause that originated it. The senior dev reviews, approves,
and the Release 1 build loop fires immediately.

---

## What This Release Is Not

- No ClickUp synchronization (Release 4)
- No client-facing portal or views (Release 5)
- No studio chat interface for polish edits (Release 3)
- No release grouping or weekly cycle management (Release 4)
- No Mermaid architecture diagrams (Release 4)
- No multi-tenancy or authentication (Release 5)
- Story approval still requires a human — this release does not automate approval

The flywheel this release creates is:
meeting ends → stories appear → human approves → Release 1 loop fires.
The human approval gate is intentional and permanent.

---

## Personas

**The Senior Dev (primary user of Release 2)**
Attends or reviews notes from client meetings.
Wants to open the dashboard after a meeting and see drafted stories already waiting,
each with a clear origin so they can quickly judge whether to approve or dismiss.
Does not want to spend time writing stories from scratch after every meeting.

**The Junior Dev (secondary user of Release 2)**
May be asked to review drafted stories before the senior dev approves them.
Needs to understand where each story came from without reading the full transcript.

**The Platform Operator**
Manages the Fathom webhook configuration per project.
Needs to be able to ingest a SOW document for a project so that stories
generated from meetings can reference it.
May need to re-trigger story generation if the initial run produced poor results.

---

## Context: How Fathom Works

Fathom is a meeting recording and transcription tool.
After every recorded meeting it sends a webhook payload to a configured URL.
The payload contains the full transcript broken into speaker-labeled turns,
a Fathom-generated summary, and a list of action items Fathom extracted.

Each transcript turn identifies the speaker by name and email,
the start and end time in seconds, and the text of what was said.

Agency Flow receives this webhook, stores the full transcript,
and uses it as the primary input for automated story generation.

---

## User Stories

### Story 2.1 — Receive and store a Fathom meeting transcript
As the platform, I want to receive the Fathom webhook after every client meeting,
validate it, and store the full transcript with speaker labels and timing,
so that the meeting is permanently available for story generation and future reference.

Acceptance criteria:
- The platform exposes a webhook endpoint that Fathom can be configured to call
- The endpoint validates the webhook signature to confirm it came from Fathom
- The endpoint is idempotent — if Fathom sends the same meeting twice,
  the second delivery has no effect
- The meeting title, date, duration, and participant list are stored
- Every transcript turn is stored with speaker name, speaker email,
  start time, end time, turn text, and turn order index
- Action items extracted by Fathom are stored with assignee and due date
- The full raw Fathom payload is stored for re-processing if needed
- The endpoint returns a 200 response immediately without waiting for
  story generation to complete
- A new meeting appears in the meetings list on the dashboard
  shortly after the webhook fires

---

### Story 2.2 — Generate user stories automatically from a meeting transcript
As a senior dev, I want user stories to be drafted automatically from the
Fathom transcript after every meeting, so that I do not need to write stories
manually from my notes.

Acceptance criteria:
- Story generation begins automatically after the transcript is stored
- Each identifiable feature request, change, or commitment in the transcript
  produces a drafted story
- Each story has a title, description, and acceptance criteria drawn from
  what was discussed in the meeting
- Each story is linked to the specific transcript turn where it was discussed
- Stories requested by the client are noted as client-originated
- Stories that appear to fall outside the current SOW are flagged with
  a visible warning but are still created
- Action items from Fathom that are not already covered by a drafted story
  also produce drafted stories
- Generated stories appear in the dashboard with status drafted
- The meeting record is updated to show that stories have been generated
- Story generation does not produce duplicate stories for features
  that already exist in the project

---

### Story 2.3 — View the meeting transcript and its drafted stories together
As a senior dev, I want to see the full meeting transcript alongside the stories
it generated, so that I can quickly verify each story is an accurate
representation of what was discussed before approving it.

Acceptance criteria:
- There is a meeting detail screen accessible from the meetings list
- The screen shows the meeting title, date, duration, and participants
- The full transcript is displayed with speaker labels and timestamps
- Transcript turns that originated a story are visually distinguished
  from turns that did not
- Clicking a highlighted turn scrolls to or highlights the story it produced
- The drafted stories from this meeting are shown alongside the transcript
- Each story shows its title, description, and the source turn it came from
- Out-of-scope flagged stories are visually distinguished
- I can approve individual stories from this screen
- I can approve all non-flagged stories from this screen with a single action
- I can dismiss a story from this screen if it should not be built

---

### Story 2.4 — Trace any story back to its origin
As a senior dev, I want every story to show where it came from,
so that I can always answer the question of why we are building something
and tie it back to a client conversation or a contractual commitment.

Acceptance criteria:
- Every story card on the project board shows a source badge
- The source badge indicates whether the story came from a meeting,
  a SOW clause, an action item, or was created manually
- The badge shows enough context to identify the source at a glance
- Clicking the source badge navigates directly to the meeting turn,
  SOW clause, or document that originated the story
- This traceability is never lost when a story moves through the lifecycle
- Stories that reach the build pipeline without a source reference
  are treated as a data error and must be resolved before building

---

### Story 2.5 — Ingest a Statement of Work document
As a platform operator, I want to ingest a SOW document for a project
so that stories generated from meetings can reference specific SOW clauses
and the out-of-scope flag works accurately.

Acceptance criteria:
- I can upload or reference a SOW document from the project documents screen
- The platform accepts PDF and Markdown formats
- The document is parsed into individual clauses based on its heading structure
- Each clause is stored with its heading, content, and order index
- After ingestion I can see the clause list and how many stories reference each one
- When a story is generated from a meeting and it matches a SOW clause,
  the story is linked to that clause as a secondary source reference
- Re-ingesting a SOW creates a new version and does not delete prior clauses
- I can view the full document and navigate to individual clauses

---

### Story 2.6 — Re-process a meeting to regenerate stories
As a platform operator, I want to re-trigger story generation for a meeting
that has already been processed, so that I can recover from a poor initial
generation run or generate stories again after a SOW has been updated.

Acceptance criteria:
- There is a reprocess action available on every meeting
- Reprocessing re-runs the full story generation workflow for that meeting
- Stories that were already approved or in the build pipeline are not
  affected by reprocessing
- Only stories still in drafted status from the previous run are replaced
- Dismissed stories are not recreated during reprocessing
- The meeting record reflects that reprocessing occurred and when

---

### Story 2.7 — View a list of all meetings for a project
As a senior dev, I want to see all meetings for a project in one place
with a summary of what was generated from each, so that I have a complete
record of every client conversation and its outcomes.

Acceptance criteria:
- There is a meetings list screen accessible from the project navigation
- The list shows meetings in reverse chronological order
- Each meeting shows the title, date, duration, and number of stories generated
- Meetings where story generation is still pending show that status
- I can navigate to the meeting detail from the list
- I can trigger reprocessing from the list without opening the detail screen

---

### Story 2.8 — View all documents for a project
As a senior dev, I want to see all documents ingested for a project,
so that I know what reference material has been loaded and can navigate
to specific clauses when reviewing stories.

Acceptance criteria:
- There is a documents screen accessible from the project navigation
- The list shows all documents with their type, ingestion date, and clause count
- I can navigate to the document detail from the list
- The document detail shows the full content and a clause index in the sidebar
- Each clause in the index shows how many stories reference it
- Clicking a clause count shows the stories that reference it

---

## Data Requirements

The following information must be stored permanently in Supabase.
Agents must write to Supabase — not only to local agent memory.
All data written in this release must survive a session restart.

**Per meeting:**
- Unique ID and project reference
- Fathom's own meeting ID (used for idempotency)
- Title, date, duration in seconds
- Fathom-generated summary
- Full raw payload stored for re-processing
- Whether stories have been generated from this meeting and when

**Per transcript turn:**
- Meeting reference and turn order index
- Speaker name and speaker email
- Start and end time in seconds
- Full text of the turn

**Per action item:**
- Meeting reference
- Action item text, assignee name, assignee email, due date
- Reference to the story created from this action item (nullable)

**Per document:**
- Project reference, title, document type
- Full parsed text content
- Path to original file in storage
- Date parsed

**Per SOW clause:**
- Document reference and clause order index
- Heading and full content

**Story additions in Release 2:**
- Stories generated from meetings must have source_ref_type set to meeting_turn
  and source_ref_id pointing to the primary turn
- Stories generated from action items must have source_ref_type set to action_item
- Stories flagged as out of scope must carry that flag permanently
- The out-of-scope flag does not prevent building — it surfaces a warning only
- Dismissed stories must be retained in the database with a dismissed status
  and a record of who dismissed them and when

---

## Agent Behavior Requirements

**Context agent**
Must fetch the full transcript, all SOW clauses, and existing story titles
before the story drafter runs.
Must seed AgentDB with all three before drafting begins.
Must not allow story drafting to begin without this context loaded.
Must fetch from Supabase at the start of every session — never assume
prior session context is still available.

**Story drafter agent**
Must read the transcript speaker by speaker in turn order.
Must not skip turns or batch-summarize groups of turns.
Must produce a story for every identifiable request, not only the obvious ones.
Must link each story to the specific turn ID where it was primarily discussed.
Must note when a request originates from the client side versus the dev side.
Must flag stories that do not appear covered by any SOW clause.
Must also review action items and produce stories for any not already covered
by a transcript story.
Must not produce duplicate stories for things already in the project story list.
Must always populate acceptance criteria — if AC cannot be extracted from the
transcript directly, the agent must write reasonable AC based on context
and mark it as agent-inferred so the senior dev knows to review it.

**Dedup agent**
Must compare drafted stories against existing stories semantically, not by title alone.
Must not auto-discard near-matches — flag them for human review.
Must only auto-discard stories that are clearly identical to an existing story.
Must log the reason for every discard decision so it is reviewable.

**All agents in Release 2**
Must write all outputs to Supabase before ending the session.
Must not assume a prior session's AgentDB context is still available.
Must seed from Supabase at the start of every session.
Must update the meeting record when story generation is complete.

---

## Integration Requirements

**Fathom**
- The Fathom webhook URL must be configurable per project in platform settings
- The webhook endpoint must validate the Fathom HMAC signature on every request
  using a per-project secret stored securely
- The endpoint must handle Fathom redelivery gracefully via idempotency check
  on Fathom's own meeting ID
- The platform must return 200 to Fathom immediately without waiting for
  story generation to complete

**Platform API additions for Release 2**
The workspace communicates with the platform via the existing API pattern.
The following new capabilities are needed:

- Retrieve the meetings list for a project
- Retrieve full meeting detail including all turns and action items
- Retrieve the documents list for a project
- Submit a new document for ingestion and parsing
- Trigger reprocessing for a specific meeting
- The workspace must use these endpoints rather than querying
  the platform Supabase database directly

---

## QA and Testing Requirements

**Fathom webhook handler**
- Test that a valid webhook payload is accepted and all fields stored correctly
- Test that a duplicate Fathom meeting ID is rejected with no data changes
- Test that an invalid or missing HMAC signature returns 401
- Test that all transcript turns are stored in correct turn index order
- Test that action items are stored with correct assignee and due date
- Test that story generation is triggered after successful storage
- Test that the endpoint returns 200 before story generation completes

**Story generation**
- Test that a transcript containing five identifiable requests produces five stories
- Test that each story has source_ref_type of meeting_turn and a valid source_ref_id
- Test that an action item not covered by a transcript story produces its own story
  with source_ref_type of action_item
- Test that a story matching an existing story title is not duplicated
- Test that a near-match story is flagged for human review rather than discarded
- Test that a story discussing something outside the SOW receives the out-of-scope flag
- Test that all generated stories have acceptance criteria populated
- Test that agent-inferred acceptance criteria is marked as such

**SOW ingest**
- Test that a Markdown SOW is parsed into correct clauses preserving heading structure
- Test that a PDF SOW has text extracted and is parsed into clauses
- Test that clause order index is preserved correctly
- Test that re-ingesting the same document creates a new version
  without deleting prior clauses or breaking story references

**Reprocessing**
- Test that reprocessing does not affect stories in queued or later status
- Test that reprocessing does not recreate dismissed stories
- Test that reprocessing replaces only drafted stories from the prior run
- Test that the meeting record is updated to reflect the reprocess event

**Dashboard screens**
- The meetings list renders in reverse chronological order
- Meeting detail shows transcript turns in correct order with speaker labels
- Turns that produced stories are visually distinguished from those that did not
- Clicking a source badge on a story card navigates to the correct meeting turn
- Approve individual story from meeting detail page triggers the build loop
- Approve All triggers the build loop for every non-flagged drafted story
- Dismiss removes a story from view without destroying the record
- The documents screen shows correct clause count per document
- The document detail shows the stories linked to each clause

**End-to-end acceptance test**
The following scenario must pass completely before Release 2 is signed off:

1. Configure a Fathom webhook URL for the test project
2. Send a synthetic Fathom payload containing at least three identifiable
   feature requests, two action items, and one topic clearly outside the SOW
3. Confirm the meeting appears in the meetings list shortly after the webhook fires
4. Confirm all transcript turns are stored in correct speaker order
5. Confirm drafted stories appear — one per identifiable request plus
   any action items not already covered
6. Confirm the out-of-scope story is created and flagged with a warning
7. Confirm each story shows the correct source badge on the project board
8. Confirm clicking a source badge navigates to the correct transcript turn
9. Open the meeting detail and verify the transcript is readable with
   story-linked turns visually distinguished
10. Approve two stories individually from the meeting detail screen
11. Confirm both stories move to queued and the Release 1 build loop fires
12. Send the identical Fathom payload a second time
13. Confirm no duplicate meetings or stories are created
14. Ingest a SOW document and confirm clauses are parsed and stored correctly
15. Trigger reprocessing on the original meeting
16. Confirm a new story is not created for a feature already in queued status
17. Confirm dismissed stories are not recreated by reprocessing

---

## Definition of Done

Release 2 is complete when every item below is checked.
Release 3 does not begin until this list is fully verified.

**Schema and storage**
- [ ] Meetings table created and migrated
- [ ] Meeting turns table created with turn_index ordering enforced
- [ ] Action items table created with story link field
- [ ] Documents table created with storage path field
- [ ] SOW clauses table created with clause_index ordering enforced
- [ ] Realtime enabled on the meetings table
- [ ] Every meeting created during testing has all turns stored in order
- [ ] Every story generated has source_ref_type and source_ref_id populated

**Webhook and ingestion**
- [ ] Fathom webhook endpoint validates HMAC signature correctly
- [ ] Webhook is idempotent on duplicate delivery with no data side effects
- [ ] Webhook returns 200 before story generation completes
- [ ] All transcript fields stored correctly including speaker email and timing
- [ ] Action items stored with all Fathom-provided fields
- [ ] SOW ingest works for PDF format
- [ ] SOW ingest works for Markdown format
- [ ] SOW re-ingestion creates new document version without breaking prior story references

**Story generation**
- [ ] All identifiable requests in a test transcript produce drafted stories
- [ ] All stories have acceptance criteria populated
- [ ] Agent-inferred acceptance criteria is marked as such
- [ ] All stories have source_ref_type and source_ref_id set correctly
- [ ] Out-of-scope stories are flagged and created
- [ ] Duplicate stories are not created for existing project features
- [ ] Near-match stories are flagged for review and not auto-discarded
- [ ] Dismissed stories are retained with dismissal record
- [ ] Reprocessing does not affect approved or in-pipeline stories

**Dashboard**
- [ ] Meetings list renders in reverse chronological order
- [ ] Meeting detail shows full transcript in correct speaker-labeled turn order
- [ ] Story-linked turns are visually distinguished in the transcript
- [ ] Approve individual story from meeting detail fires build loop
- [ ] Approve All fires build loop for all non-flagged drafted stories
- [ ] Dismiss removes story from drafted list without deleting the record
- [ ] Source badge on story card links to the correct meeting turn or SOW clause
- [ ] Documents list shows correct clause count per document
- [ ] Document detail shows stories linked per clause with correct counts

**End-to-end**
- [ ] Full acceptance test scenario passes all seventeen steps
- [ ] Duplicate webhook delivery produces no duplicate data
- [ ] At least one meeting with an out-of-scope flagged story has been verified
- [ ] SOW traceability verified end-to-end: story links back to clause correctly
- [ ] All tests pass in CI before release is signed off
