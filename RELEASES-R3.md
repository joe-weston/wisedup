# RELEASES.md — Release 3: The Polish Layer
> Read MISSION.md, RELEASES-R1.md, and RELEASES-R2.md before this document.
> Releases 1 and 2 must be fully complete before Release 3 begins.
> This release has one outcome: after the agent swarm builds a story and
> the preview is live, a human can make targeted changes via a chat interface
> inside the platform without switching tools, and merge the result
> through git controls in the dashboard.

---

## Release Goal

Close the gap between "agents built it" and "it is ready for the client."
After Release 2 the build loop runs automatically, but the output still
requires human judgment on the last 20 percent — layout adjustments,
copy changes, color corrections, interaction polish.
After Release 3 that judgment can be applied directly in the platform
via a chat interface that proposes and applies targeted code changes,
with a live preview refreshing in real time alongside the conversation.

The junior dev can run a full polish session independently.
The senior dev can review, approve, and merge to staging without
opening a terminal, switching to GitHub, or leaving the dashboard.

---

## What This Release Is Not

- No automated story generation from meetings — that is Release 2
- No ClickUp synchronization — that is Release 4
- No meeting prep or debrief commands — that is Release 4
- No Mermaid architecture diagrams — that is Release 4
- No client-facing portal — that is Release 5
- No multi-tenancy or authentication — that is Release 5
- The studio does not replace the agent swarm build pipeline.
  The swarm still does the primary build in Release 1.
  The studio handles targeted human-directed polish only.

---

## Personas

**The Junior Dev (primary user of Release 3)**
Has the story in front of them and a preview URL to look at.
Wants to make small targeted changes without touching the codebase directly.
Should be able to describe a change in plain English and see it applied
to the preview without needing to understand the full project structure.
Should be able to run an entire polish session without senior dev involvement.

**The Senior Dev (secondary user of Release 3)**
Reviews stories after the junior dev polishes them.
Wants to approve and merge to staging from the dashboard without
opening GitHub or a terminal.
Wants to request changes that go back to the junior dev as a pre-filled
studio session rather than a Slack message.

**The Client (future awareness only)**
Not logging in yet. But the client portal in Release 5 will be built
on top of the staging approval flow that Release 3 establishes.
Design the approval flow with a client-facing future in mind.

---

## User Stories

### Story 3.1 — View a story and its live preview side by side
As a junior dev, I want to see the current story details and its
Vercel preview in the same screen, so that I can assess what needs
to be polished without switching between tabs.

Acceptance criteria:
- There is a studio screen accessible from the project navigation
  and from any story card
- The studio shows the active story title, description,
  and acceptance criteria in a panel
- The studio shows the Vercel preview URL rendered in an iframe
  alongside the story panel
- The preview iframe loads the feature branch deployment, not main
- I can switch between stories in the current release from the studio
  without navigating away
- The preview iframe shows which environment it is rendering
- If no preview URL is set for the selected story, the studio shows
  a clear message explaining why and what to do

---

### Story 3.2 — Request a targeted code change via chat
As a junior dev, I want to describe a change I want to make in plain English
and see the proposed code change before it is applied,
so that I can confirm the agent understood my intent before committing anything.

Acceptance criteria:
- There is a chat input in the studio where I can describe a change
- After I submit a request the agent proposes the change as a diff
  showing what will be added, changed, or removed
- The diff is shown before anything is committed
- I can read the diff and understand what will change
  without needing to know the codebase
- I can approve the diff to apply it or discard it to cancel
- If I discard the diff nothing is committed and I can try again
- The agent scopes its changes to the files relevant to the story
  and does not modify unrelated files
- I can focus the agent on a specific file by selecting it
  before submitting my chat message
- The agent asks a clarifying question rather than guessing
  if the request is ambiguous

---

### Story 3.3 — See the preview refresh after a change is applied
As a junior dev, I want the preview iframe to update automatically after
a change is applied and Vercel deploys it, so that I can see the result
of my change without manually refreshing or waiting to check a URL.

Acceptance criteria:
- After a diff is approved and committed the preview iframe shows
  a deploying indicator
- When the Vercel deploy for the story branch completes
  the preview iframe refreshes automatically
- The refresh happens without a full page reload
- I do not need to manually trigger the refresh or copy a new URL
- The time between approving a diff and seeing the result in the iframe
  is only limited by how long Vercel takes to deploy
- If a deploy fails the studio shows an error state on the preview
  with enough information to understand what happened

---

### Story 3.4 — View and navigate files changed in the story branch
As a junior dev, I want to see which files have been changed in the
story branch compared to main, so that I can understand the scope of
the build and focus my polish requests on the right files.

Acceptance criteria:
- The studio shows a list of files changed in the current story branch
- The file list distinguishes between files added, modified, and deleted
- I can click a file to see its diff against main
- I can select a file to scope my next chat message to that file
- The file list updates after each polish edit is applied
- The file list is collapsible so it does not crowd the chat interface

---

### Story 3.5 — Request changes from a story card without opening the studio manually
As a senior dev, I want to request a change on a story that is in review
and have that request open the studio pre-filled with my description,
so that the junior dev receives clear context rather than a vague instruction.

Acceptance criteria:
- Any story in in_review or staging status has a Request Changes button
- Clicking Request Changes opens a modal where I describe the needed change
- Submitting the modal navigates to the studio with that story selected
  and my change description pre-filled in the chat input
- The agent reads the pre-filled request immediately and proposes a diff
  without me needing to submit again
- The change request is logged to the story history with my name,
  the description I wrote, and the timestamp
- The story status does not change when a change is requested —
  it stays in in_review or staging

---

### Story 3.6 — Approve a story and merge it to staging
As a senior dev, I want to merge an approved story branch to staging
from the dashboard, so that the client can see it in the staging environment
without me using git on the command line.

Acceptance criteria:
- The story detail page shows the current branch name and PR status
- I can see the list of commits on the story branch
- There is an Approve and Merge to Staging button on stories in in_review status
- Clicking it merges the story branch to the staging branch
  and updates the story status to staging
- For Tier 1 projects where there is no separate staging branch,
  merging to staging means merging to main
  and the story moves directly to staging status
- For Tier 2 projects the staging branch receives the merge
  and a staging Vercel deployment is triggered
- The merge is recorded in the story history with my name and timestamp
- I cannot merge a story that has a pending unresolved migration proposal
- I cannot merge a story whose test suite results show failures

---

### Story 3.7 — Approve a story for production after staging review
As a senior dev, I want to mark a staged story as approved for production
and merge it, so that completed and reviewed work moves to the live environment.

Acceptance criteria:
- Stories in staging status show an Approve for Production button
- Clicking it requires a confirmation step to prevent accidental merges
- For Tier 1 projects the story is already on main so approval
  updates the status to approved and then shipped with no additional merge
- For Tier 2 projects the staging branch is merged to main,
  the production Vercel deployment is triggered,
  and the story status updates to shipped
- The shipped timestamp and the user who approved it are recorded
- I cannot approve for production a story that has not first been in staging

---

### Story 3.8 — Switch between environments in the preview iframe
As a junior dev, I want to switch the preview iframe between dev, staging,
and production environments, so that I can compare the story branch output
against what is already live.

Acceptance criteria:
- The studio shows an environment selector near the preview iframe
- I can switch between dev, staging, and production
- Dev loads the feature branch Vercel preview URL
- Staging loads the staging deployment URL on Tier 2 projects
  or is greyed out with an explanation on Tier 1 projects
- Production loads the production URL in a read-only state
- When production is selected the chat input is disabled
  and a clear warning is shown that I am viewing production
- No code changes can be committed while production is selected in the studio

---

### Story 3.9 — Upgrade a project to Environment Tier 2
As a platform operator, I want to upgrade a project from Tier 1 to Tier 2
so that staging and production use separate Supabase branches
and merges to staging no longer risk affecting production data.

Acceptance criteria:
- The project settings screen explains the difference between Tier 1 and Tier 2
- There is a guided upgrade path that tells me exactly what steps to take
  in the Supabase dashboard before completing the upgrade in Agency Flow
- I can enter the Supabase staging branch reference in project settings
- After saving, the project moves to Tier 2 behavior for all future merges
- Existing stories are not affected by the tier upgrade
- The environment selector in the studio reflects the new tier immediately
- The settings screen clearly states that the upgrade cannot be reversed automatically

---

## Data Requirements

The following must be stored permanently in Supabase.
No polish session data lives only in agent memory.

**Per polish edit applied in the studio:**
- Story reference
- The chat message that described the request
- The commit SHA that was created
- The file or files that were changed
- Who applied it and when
- Stored as a story_status_history entry with type polish_edit

**Per change request submitted:**
- Story reference
- The description written by the senior dev
- Who submitted it and when
- Stored as a story_status_history entry with type change_request

**Per merge action:**
- Story reference
- Target branch or environment
- Commit SHA of the merge
- Who performed it and when
- New story status after merge
- Stored as a story_status_history entry with type merge

**Project additions for Tier 2:**
- Staging branch name
- Staging Vercel deployment URL
- Production Vercel deployment URL
- Supabase staging branch reference
- Current environment tier

The studio chat conversation itself does not need to be stored
beyond the applied edits and the change request text.
The conversation is ephemeral. The commits are permanent.

---

## Agent Behavior Requirements

**Studio agent**
Must read the full story including acceptance criteria before responding
to any polish request.
Must read the file tree of the story branch before proposing any change.
Must scope all changes to files already touched in the story branch
unless the operator explicitly references a file outside that scope.
Must show a diff and wait for approval before committing anything.
Must never commit directly without showing a diff first.
Must ask a clarifying question rather than guessing when a request is ambiguous.
Must not modify test files during a polish session —
tests are the build pipeline's domain, not the studio's.
Must write a meaningful conventional commit message summarizing the change.
Must post the polish_edit record to the platform API after every applied change.
Must not accept or process any request while the production environment
is selected in the studio.

**Git operations in Release 3**
All merge operations must go through the platform API before touching GitHub.
The platform validates that the story is in the correct status
and has no blocking conditions before the merge is allowed.
No agent or UI action merges directly to a branch without
the platform recording the intent first.

Blocking conditions that must prevent any merge:
- Pending migration proposal on the story that has not been approved
- Test suite results showing failures on the story
- Story not in the expected pre-merge status

---

## Integration Requirements

**GitHub**
- The platform must read the file diff of a story branch versus main
- The platform must trigger merges via the GitHub API
- The platform must read the commit list for a branch
- Branch protection rules on main must be compatible with API-triggered merges

**Vercel**
- The platform must read the latest deployment status for a branch
- The existing Release 1 webhook continues to handle preview URL updates
- Staging deployments triggered by Tier 2 merges must also fire the webhook
  so the story card preview URL updates to the staging URL after merge

**Supabase branching for Tier 2**
- The platform stores the staging branch reference entered by the operator
- The platform does not create or manage Supabase branches directly
- Supabase branch setup is a manual step performed in the Supabase dashboard
  before completing the Tier 2 upgrade in Agency Flow
- This constraint must be clearly communicated in the upgrade UI

---

## QA and Testing Requirements

**Studio interface**
- Test that the studio loads with the correct story selected
- Test that the preview iframe renders the feature branch URL
- Test that selecting a different story updates both the story detail
  and the preview iframe
- Test that the environment selector is present and switches the iframe URL
- Test that the chat input is disabled when production is selected
- Test that the warning banner appears when production is selected
- Test that the file list shows files changed in the story branch
  with correct added, modified, and deleted distinction
- Test that clicking a file shows its diff against main
- Test that the file list is collapsible

**Studio agent behavior**
- Test that submitting a clear change request produces a diff before committing
- Test that approving the diff results in a commit on the story branch
- Test that discarding the diff results in no commit and no file changes
- Test that a polish_edit record is written to story history after each applied change
- Test that the agent does not modify files outside the story branch scope
  unless explicitly referenced
- Test that an ambiguous request prompts a clarifying question
  rather than a speculative diff

**Git controls**
- Test that Approve and Merge to Staging is only visible on in_review stories
- Test that a Tier 1 merge succeeds targeting main
- Test that a Tier 2 merge succeeds targeting the staging branch
- Test that story status updates to staging after a successful merge
- Test that a merge is blocked when a pending migration proposal exists
- Test that a merge is blocked when test results show failures
- Test that Approve for Production is only visible on staging stories
- Test that the confirmation step is required before production merge proceeds
- Test that the story status updates to shipped after a Tier 2 production merge
- Test that the shipped timestamp and actor are recorded correctly in story history

**Change request flow**
- Test that Request Changes is visible on in_review and staging stories
- Test that submitting the modal navigates to the studio
  with the story selected and description pre-filled
- Test that the agent proposes a diff immediately from the pre-filled request
- Test that the change request is logged in story_status_history
  with correct actor, description, and timestamp
- Test that the story status is unchanged after a change request is submitted

**Environment tier upgrade**
- Test that a Tier 1 project can be upgraded to Tier 2 via project settings
- Test that after upgrade the merge buttons target the staging branch
- Test that the environment selector in the studio shows staging as available
  after the upgrade
- Test that the Supabase staging branch reference is stored on the project record

**Preview refresh**
- Test that a deploying indicator appears in the iframe after a diff is applied
- Test that the iframe refreshes automatically when the Vercel deploy completes
- Test that no manual action from the operator is required to trigger the refresh
- Test that a failed deploy shows an error state in the preview area

**End-to-end acceptance test**
The following scenario must pass completely before Release 3 is signed off:

1. Select a story that has been built by the Release 1 swarm
   and has a Vercel preview URL set
2. Open the studio and confirm the preview iframe loads the story branch preview
3. Confirm the file list shows the files changed in the story branch
4. Submit a chat request describing a simple visual change
5. Confirm the agent proposes a diff without committing
6. Approve the diff and confirm a commit appears on the story branch
7. Confirm the preview iframe shows a deploying indicator
8. Wait for Vercel deploy to complete and confirm the iframe refreshes
   without a page reload
9. Submit a second chat request that is intentionally ambiguous
10. Confirm the agent asks a clarifying question rather than guessing
11. Navigate back to the project board and click Request Changes
    on a story card in in_review status
12. Write a change description in the modal and submit
13. Confirm the studio opens with the story selected
    and the description pre-filled in the chat input
14. Confirm the change request is logged in story history
15. Click Approve and Merge to Staging from the git controls
16. Confirm the story branch is merged and status updates to staging
17. For a Tier 2 project confirm the staging Vercel deployment triggers
    and the preview iframe shows the staging URL when staging is selected
18. Click Approve for Production and confirm the confirmation dialog appears
19. Complete the confirmation and verify the story status updates to shipped
20. Verify the shipped timestamp and actor are recorded in the story history timeline

---

## Definition of Done

Release 3 is complete when every item below is checked.
Release 4 does not begin until this list is fully verified.

**Studio interface**
- [ ] Studio screen renders with three panel layout at full viewport height
- [ ] Story selector in left panel loads correct story details and preview
- [ ] Preview iframe renders feature branch Vercel URL correctly
- [ ] Environment selector switches iframe between dev, staging, and production
- [ ] Production environment disables chat input and shows warning banner
- [ ] File list shows changed files with added, modified, deleted distinction
- [ ] File diff view renders on file selection
- [ ] File list is collapsible

**Studio agent**
- [ ] Chat request produces a diff before any commit is made
- [ ] Approving diff commits to story branch with a meaningful commit message
- [ ] Discarding diff produces no commit and no file changes
- [ ] Polish edit logged to story_status_history after every applied change
- [ ] Agent does not modify files outside story branch scope without explicit reference
- [ ] Ambiguous request prompts clarifying question rather than a guess
- [ ] No changes accepted while production environment is selected

**Git controls**
- [ ] Branch name and PR status visible on story detail page
- [ ] Commit list visible on story detail page
- [ ] Approve and Merge to Staging only visible on in_review stories
- [ ] Tier 1 merge targets main and story moves to staging status
- [ ] Tier 2 merge targets staging branch and story moves to staging status
- [ ] Merge blocked when pending migration proposal exists
- [ ] Merge blocked when test results show failures
- [ ] Approve for Production only visible on staging stories
- [ ] Confirmation step required before production merge proceeds
- [ ] Story status updates to shipped after production merge
- [ ] Shipped timestamp and actor recorded in story history

**Change request flow**
- [ ] Request Changes button visible on in_review and staging stories
- [ ] Modal accepts free text description and submits correctly
- [ ] Submission navigates to studio with story selected and text pre-filled
- [ ] Change request logged in story_status_history with correct fields
- [ ] Story status unchanged after change request submission

**Environment tier**
- [ ] Tier 2 upgrade flow available and guided in project settings
- [ ] Upgrade stores staging branch reference and staging Vercel URL
- [ ] Post-upgrade merge behavior correctly targets staging branch
- [ ] Environment selector in studio reflects the project tier correctly

**Preview refresh**
- [ ] Deploying indicator appears in iframe after diff is applied
- [ ] Iframe refreshes automatically when Vercel deploy completes
- [ ] No manual action required from operator to trigger refresh
- [ ] Failed deploy shows error state in the preview area

**End-to-end**
- [ ] Full acceptance test scenario passes all twenty steps
- [ ] At least one full polish session completed by a junior dev
  without senior dev assistance
- [ ] At least one story merged to production via dashboard
  without using the command line
- [ ] All tests pass in CI before release is signed off
