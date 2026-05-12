# RELEASES.md — Release 5: Multi-Tenancy and Auth
> Read MISSION.md and RELEASES-R1.md through RELEASES-R4.md before this document.
> Releases 1 through 4 must be fully complete before Release 5 begins.
> This release has one outcome: the platform becomes a real multi-tenant SaaS.
> Multiple agencies can onboard independently, each seeing only their own data,
> with authentication enforced at every layer and a published workspace
> template any agency can install with a single command.

---

## Release Goal

Everything built in Releases 1 through 4 works for one agency running
one project with no login. Release 5 turns that into a product.

After Release 5:
- Any dev shop can sign up, onboard their agency, invite their team,
  create their clients and projects, and be running the full workflow
  within a single session
- Every data boundary between agencies, clients, and projects is enforced
  at the database layer via Row Level Security — not just at the UI layer
- A senior dev at Agency A cannot see, access, or affect anything
  belonging to Agency B under any circumstance
- A user can belong to multiple agencies simultaneously with different
  roles in each — the co-founder scenario is handled gracefully
- Any project gets a workspace template with one command:
  npx agency-flow init
- The platform Tier 3 environment option is available for high-stakes
  projects that need fully isolated Supabase projects per environment
- Credentials for high-stakes projects move from .env files
  to a proper secrets manager

This is the release where Agency Flow becomes something you can hand
to another founder and say: sign up and run it yourself.

---

## What This Release Is Not

- No billing or subscription management — that is Release 6
- No white-label branding options — that is Release 6
- No client-facing portal login — that is Release 6
- No usage tracking dashboard — that is Release 6
- The client_viewer role exists and is enforced in this release
  but the polished client portal experience is Release 6

---

## Personas

**The Agency Admin (new primary persona in Release 5)**
Owns or runs a dev shop.
Signs up for Agency Flow, creates the agency, invites the team,
sets up the first client and project.
Manages team member roles and access.
Is the only person who can delete projects or remove team members.
May also be a developer on projects — roles are not mutually exclusive.

**The Developer (existing persona, now with auth)**
Is invited to the agency by the admin.
Has access to the projects they are assigned to.
Cannot see projects they are not assigned to, even within the same agency.
Runs the workspace locally and calls the platform API with a project API key.

**The Client Viewer (new persona in Release 5)**
Is a stakeholder at the client company — not a developer.
Is invited to view one or more projects at their client organization.
Can see project status, story summaries, release notes, and documents.
Cannot create, edit, or approve anything.
Cannot see any other client's data.
Logs in via magic link — no password required.

**The Platform Operator (you, running multiple agencies)**
Has accounts in multiple agencies with different roles in each.
The platform handles this gracefully — a single login can switch
between agency contexts without logging out.
In the co-founder scenario, can be both an agency member
and a client stakeholder on the same project.

---

## User Stories

### Story 5.1 — Sign up and create an agency
As a founder, I want to sign up for Agency Flow and create my agency
so that I have a tenant context to work within before doing anything else.

Acceptance criteria:
- There is a public sign-up page that does not require an invitation
- I can sign up with email and password
- After sign-up I am prompted to create my agency with a name and slug
- The slug must be unique across all agencies on the platform
- After creating the agency I am the agency admin automatically
- I am redirected to the onboarding flow after agency creation
- If I try to access the dashboard before completing onboarding
  I am redirected back to continue the onboarding flow
- A single email address can only have one platform account
  but that account can belong to multiple agencies

---

### Story 5.2 — Complete the agency onboarding flow
As an agency admin, I want a guided onboarding flow that walks me through
setting up everything I need before I can run the workflow,
so that I do not have to figure out the setup order myself.

Acceptance criteria:
- The onboarding flow is a step-by-step wizard I cannot skip
- Step one: agency details already completed at sign-up
- Step two: invite at least one team member by email with a role assignment,
  or skip with a single confirmation click
- Step three: create the first client with a name
- Step four: create the first project with a name, GitHub repo,
  Vercel project ID, and Supabase project reference
- Step five: configure integrations — Fathom webhook URL displayed
  for me to copy into Fathom settings, ClickUp fields optional and skippable
- Step six: copy the project API key to use in the workspace .env file,
  with a copy to clipboard button
- After completing all steps I land on the project dashboard
- I can return to any completed step to update settings
- Progress is saved between sessions — if I leave mid-onboarding
  I can resume where I left off

---

### Story 5.3 — Invite team members with roles
As an agency admin, I want to invite team members to my agency
and assign them roles, so that each person has the right level of access
for their responsibilities.

Acceptance criteria:
- I can invite team members from the agency settings screen
  by entering their email address and selecting a role
- Available roles are: agency_admin, developer, and client_viewer
- An invitation email is sent to the invitee with a magic link
- The invitee clicks the link and is prompted to set a password
  if they do not already have a platform account
- If the invitee already has a platform account they are added
  to the agency automatically when they click the link
- I can see pending invitations and resend or cancel them
- I can change a team member's role after they have joined
- I can remove a team member from the agency
- Removing a team member does not delete any data they created
- An agency must always have at least one agency_admin —
  the last admin cannot be removed or downgraded

---

### Story 5.4 — Log in and switch between agency contexts
As a user who belongs to multiple agencies, I want to log in once
and switch between my agency contexts without logging out,
so that I can work across multiple clients or employers efficiently.

Acceptance criteria:
- There is a login page with email and password
- There is a magic link login option for users who prefer it
- After login I land on the dashboard for my primary agency context
- If I belong to multiple agencies there is a context switcher
  visible in the navigation
- Switching agency context changes all data in the dashboard
  to reflect the selected agency
- My role may differ between agencies and the UI reflects
  the correct permissions for the active context
- I remain logged in across browser sessions until I explicitly log out
- A user in the co-founder scenario who is both an agency member
  and a client stakeholder on the same project can access both views
  without needing two accounts

---

### Story 5.5 — Enforce data isolation between agencies
As an agency admin, I want to be certain that no user from another agency
can ever see or access my agency's data, so that I can confidently
onboard clients whose data is sensitive.

Acceptance criteria:
- A logged-in user from Agency A cannot see any agencies, clients,
  projects, stories, meetings, or documents belonging to Agency B
- This isolation is enforced at the database layer via Row Level Security,
  not only at the UI layer
- An API request made with Agency A's credentials that attempts
  to read Agency B's data receives a 404 or empty result,
  not an authorization error that confirms the resource exists
- A project API key from Project X cannot be used to read or write
  data belonging to Project Y even if both are in the same agency
- RLS policies cover every table in the schema
- The isolation is verified by an automated test suite that
  attempts cross-agency reads and writes and confirms they fail

---

### Story 5.6 — Assign developers to specific projects
As an agency admin, I want to control which developers have access
to which projects, so that a developer working on one client account
cannot see work for another client.

Acceptance criteria:
- Developers are not automatically given access to all projects in the agency
- I can assign a developer to one or more projects from the project settings screen
- A developer can only see projects they have been explicitly assigned to
- Agency admins have access to all projects in their agency by default
- Client viewers are assigned to specific projects, not the whole agency
- Removing a developer from a project does not affect their agency membership
- A developer assigned to zero projects can log in but sees an empty dashboard
  with a message explaining they have not been assigned to any projects yet

---

### Story 5.7 — Log in as a client viewer and see project status
As a client stakeholder, I want to log in and see the current status
of my project without being able to change anything,
so that I can stay informed without needing to ask my dev team for updates.

Acceptance criteria:
- I can log in via magic link sent to my email — no password required
- After login I see only the project or projects I have been given access to
- I can see the current release and which stories are in progress,
  staged, and shipped
- I can see the release summary document when it has been generated
- I can see documents the agency has shared with me such as
  the meeting agenda and release summary
- I can see the Mermaid diagrams for my project
- I cannot create, edit, approve, or delete any stories, releases,
  documents, or settings
- I cannot see any other client's data
- I cannot see internal agency notes or documents not intended for clients
- I cannot access the workspace, studio, or git controls

---

### Story 5.8 — Install the workspace template with npx agency-flow init
As a developer, I want to install the Agency Flow workspace template
into a client project repository with a single command,
so that I can start running the Ruflo workflow without manually
creating configuration files.

Acceptance criteria:
- Running npx agency-flow init in a project directory starts
  an interactive setup wizard in the terminal
- The wizard asks for the platform URL, the project API key,
  and the preferred install mode (full Ruflo or Claude Code plugins only)
- The wizard detects the current directory's GitHub remote and
  pre-fills the repo name where possible
- After completing the wizard the following files exist in the project:
  CLAUDE.md with correct project ID and platform URL populated,
  all slash command files under .claude/commands/,
  all agent configuration files under .ruflo/agents/,
  the story-build workflow file under .ruflo/workflows/,
  and a .env.example file documenting all required variables
- The wizard does not overwrite existing files without confirmation
- After installation the developer is shown the next steps:
  copy .env.example to .env.local, fill in credentials,
  open Claude Code, and run /build:release
- The package is published to npm as agency-flow
- The package version is pinned to the platform version
  to prevent compatibility issues

---

### Story 5.9 — Upgrade a project to Environment Tier 3
As an agency admin, I want to upgrade a high-stakes project to
Environment Tier 3 so that the production Supabase project is
completely isolated from non-production environments and credentials
are managed through a secrets manager rather than .env files.

Acceptance criteria:
- The project settings screen explains Tier 3 and when it is appropriate
- The upgrade path guides me through creating a separate Supabase project
  for production and configuring the secrets manager integration
- The platform supports Doppler and Infisical as secrets manager options
- After upgrade the production Supabase connection string is never
  stored in Agency Flow or in any .env file — it is fetched from
  the secrets manager at deploy time via Vercel integration
- The staging and development environments continue to use the
  Tier 2 Supabase branching approach unless also upgraded
- Existing stories and releases are not affected by the tier upgrade
- The upgrade cannot be reversed automatically —
  the settings screen explains this clearly before the upgrade is confirmed
- The migration approval flow adapts correctly to Tier 3:
  migrations run on the staging Supabase project first,
  the operator promotes them to the production project manually

---

### Story 5.10 — Manage agency and project settings
As an agency admin, I want a settings area where I can manage
all configuration for my agency and its projects from one place,
so that I do not have to hunt through the application to update things.

Acceptance criteria:
- There is an agency settings screen covering:
  agency name and slug, team members and their roles,
  pending invitations, and danger zone actions
- There is a project settings screen covering:
  project name, GitHub repo, Vercel configuration,
  Supabase project references, environment tier,
  Fathom webhook URL, ClickUp configuration,
  project API key with a regenerate option, and environment URLs
- Regenerating a project API key immediately invalidates the old key
- A warning is shown before regenerating that the workspace .env
  must be updated immediately
- Danger zone actions such as deleting a project require
  typing the project name to confirm

---

## Data Requirements

The following must be added to the Supabase schema in Release 5.
All existing permissive RLS policies from prior releases are replaced
with proper enforcement policies.

**Authentication and profiles:**
- User profiles linked to Supabase Auth user IDs
- Full name and avatar URL on profile
- Profile created automatically on first login

**Agency membership:**
- Agency members table linking users to agencies with a role
- Roles: agency_admin, developer, client_viewer
- A user can have one role per agency
- The same user can have different roles in different agencies

**Project access:**
- Project members table linking users to specific projects
- Agency admins have implicit access to all projects in their agency
- Developers and client viewers must be explicitly assigned
- Client viewers have read-only access enforced at the RLS level

**Invitations:**
- Pending invitations table with email, agency, role, and expiry
- Invitation token used for the magic link
- Invitations expire after 7 days
- Accepted invitations are retained for audit purposes

**Secrets manager configuration (Tier 3):**
- Per-project secrets manager type: doppler or infisical
- Project slug or identifier in the secrets manager
- Whether production credentials are managed externally
- The actual credentials are never stored in Agency Flow

**RLS policy requirements:**
Every table must have RLS policies that enforce the following:
- A user can only read rows belonging to agencies they are a member of
- A user can only read project-level rows for projects they have access to
- A client_viewer can only read, never insert, update, or delete
- A developer can read and write to projects they are assigned to
- An agency_admin can read and write to all resources in their agency
- Project API keys grant write access to specific platform API endpoints
  only — they do not bypass RLS for direct database access
- Cross-agency reads and writes are impossible regardless of
  how the request is constructed

---

## Agent Behavior Requirements

**Workspace init agent (npx agency-flow init)**
Must detect the existing project structure before creating any files.
Must not overwrite existing CLAUDE.md, agent configs, or command files
without explicit confirmation from the operator.
Must validate that the provided platform URL is reachable
before completing setup.
Must validate that the provided project API key returns a valid project
from the platform API before completing setup.
Must populate CLAUDE.md with the correct project ID and platform URL
from the wizard responses — not placeholder values.
Must produce a .env.example that documents every required variable
with a description of what each one is and where to find it.
Must print clear next steps after installation completes.

**All agents in Release 5**
Must use authenticated API calls — the project API key in every request.
Must handle 401 responses gracefully and instruct the operator
to check their project API key rather than retrying indefinitely.
Must not log or output the project API key or any other credential
in any report, log, or console output.

---

## Integration Requirements

**Supabase Auth**
- Email and password authentication enabled
- Magic link authentication enabled
- Email confirmation required for new sign-ups
- Password reset via email enabled
- Session tokens stored in httpOnly cookies, not localStorage
- Auth state accessible server-side in Next.js middleware
  to protect all dashboard routes

**Email delivery**
- Transactional emails sent via Resend or equivalent
- Invitation emails sent when a team member is invited
- Magic link emails sent on request
- Email confirmation sent on sign-up
- All emails sent from a configured agency-flow domain
- Email templates are plain and professional —
  no complex HTML that breaks in email clients

**npm package publication**
- The agency-flow-workspace template is published to npm
  as the agency-flow package
- Package includes the init CLI wizard
- Package version is aligned with the platform release version
- The package README documents all prerequisites:
  Node version, Claude Code, Ruflo, required environment variables

**Doppler and Infisical for Tier 3**
- The platform stores the secrets manager type and project identifier
- The Vercel integration for Tier 3 projects pulls production credentials
  from the secrets manager at build time
- Agency Flow never reads the production credentials directly —
  it only stores the reference and the secrets manager handles injection

---

## QA and Testing Requirements

**Authentication**
- Test that sign-up with email and password creates a user and profile
- Test that login with correct credentials succeeds
- Test that login with incorrect credentials fails with a clear error
- Test that magic link login sends an email and authenticates on click
- Test that accessing a protected route while logged out
  redirects to the login page
- Test that session persists across browser refresh
- Test that logout clears the session and redirects to login

**Agency and onboarding**
- Test that creating an agency after sign-up sets the user as agency_admin
- Test that the onboarding wizard saves progress between sessions
- Test that completing onboarding redirects to the project dashboard
- Test that the agency slug must be unique across the platform
- Test that the last agency_admin cannot be removed or downgraded

**Invitations and team management**
- Test that inviting a new email sends an invitation email
- Test that clicking the invitation link creates an account and joins the agency
- Test that inviting an existing platform user adds them to the agency
  without requiring a new account
- Test that pending invitations can be cancelled
- Test that invitations expire after 7 days
- Test that a team member can be removed without affecting their data
- Test that a developer assigned to zero projects sees an empty dashboard
  with the correct message

**Data isolation — these tests are mandatory and must all pass**
- Test that a user from Agency A cannot read any row from Agency B's tables
- Test that a project API key from Project X cannot read data from Project Y
- Test that a client_viewer cannot insert, update, or delete any row
- Test that a developer cannot access projects they are not assigned to
  within the same agency
- Test that a cross-agency read attempt returns 404 or empty,
  not a permission error that reveals the resource exists
- Test that RLS policies are in place on every table in the schema
- Test that removing a user from an agency immediately revokes their access

**npx agency-flow init**
- Test that running the command in a new directory creates all required files
- Test that CLAUDE.md contains the correct project ID and platform URL
- Test that .env.example documents all required variables
- Test that the command does not overwrite existing files without confirmation
- Test that an invalid project API key produces a clear error before setup completes
- Test that an unreachable platform URL produces a clear error

**Environment Tier 3**
- Test that a project can be upgraded to Tier 3 in project settings
- Test that after upgrade the production connection string is not stored
  in the Agency Flow database
- Test that the secrets manager reference is stored correctly
- Test that the migration approval flow instructions reflect Tier 3 correctly

**Multi-agency user**
- Test that a user belonging to two agencies sees only their own agency's data
  in each context
- Test that switching agency context changes all dashboard data
- Test that a user with different roles in two agencies has the correct
  permissions enforced in each context

**End-to-end acceptance test**
The following scenario must pass completely before Release 5 is signed off:

1. Sign up with a new email address on the public sign-up page
2. Create an agency with a unique name and slug
3. Complete the onboarding wizard through all six steps including
   creating a client, a project, and copying the project API key
4. Invite a second user as a developer via email
5. Accept the invitation as the second user and confirm agency membership
6. Assign the developer to the newly created project
7. Log in as the developer and confirm only the assigned project is visible
8. Log in as the agency admin and confirm all projects are visible
9. Create a second agency with a different email address
10. Confirm that the second agency cannot see any data from the first agency
11. Attempt to use the first agency's project API key to read the second
    agency's project data and confirm the attempt fails
12. Invite a third user as a client_viewer and assign them to the project
13. Log in as the client_viewer and confirm read-only access to the project
14. Attempt to create a story as the client_viewer and confirm it is blocked
15. Run npx agency-flow init in a test repository using the project API key
    from step 3 and confirm all workspace files are created correctly
16. Confirm CLAUDE.md contains the correct project ID and platform URL
17. Open Claude Code in the test repository and confirm the session start
    protocol fetches project context successfully
18. Log in as the original agency admin again and confirm all prior data
    from Releases 1 through 4 is intact and accessible
19. Upgrade the project to Tier 2 if not already done in Release 3 testing
20. Confirm the Tier 3 upgrade path is visible and the instructions
    are accurate before completing the upgrade

---

## Definition of Done

Release 5 is complete when every item below is checked.
Release 6 does not begin until this list is fully verified.

**Authentication**
- [ ] Email and password sign-up and login working
- [ ] Magic link login working
- [ ] Email confirmation on sign-up working
- [ ] Password reset working
- [ ] Session persists across browser refresh
- [ ] Protected routes redirect to login when unauthenticated
- [ ] Logout clears session correctly

**Agency and onboarding**
- [ ] Agency creation sets user as agency_admin
- [ ] Onboarding wizard saves progress between sessions
- [ ] All six onboarding steps complete and redirect to dashboard
- [ ] Agency slug uniqueness enforced
- [ ] Last agency_admin cannot be removed or downgraded

**Invitations and team**
- [ ] Invitation email sent on invite
- [ ] Invitation link creates account or joins existing account
- [ ] Pending invitations can be cancelled
- [ ] Invitations expire after 7 days
- [ ] Team member removal revokes access without deleting data
- [ ] Developer with no project assignments sees correct empty state

**Data isolation**
- [ ] RLS policies in place on every table in the schema
- [ ] Cross-agency reads return empty or 404
- [ ] Cross-project API key reads fail
- [ ] client_viewer cannot insert, update, or delete any row
- [ ] Developer access limited to assigned projects
- [ ] Automated cross-agency isolation test suite passes entirely

**Project access**
- [ ] Agency admins have implicit access to all projects
- [ ] Developers require explicit project assignment
- [ ] Client viewers require explicit project assignment with read-only enforcement

**npx agency-flow init**
- [ ] Package published to npm as agency-flow
- [ ] Init wizard creates all required workspace files
- [ ] CLAUDE.md populated with correct project ID and platform URL
- [ ] .env.example documents all required variables
- [ ] Existing files not overwritten without confirmation
- [ ] Invalid API key or unreachable platform URL produces clear error

**Environment Tier 3**
- [ ] Tier 3 upgrade path available in project settings
- [ ] Production connection string not stored in Agency Flow after upgrade
- [ ] Secrets manager reference stored correctly
- [ ] Migration approval instructions reflect Tier 3 correctly

**Multi-agency user**
- [ ] User belonging to multiple agencies sees correct data per context
- [ ] Context switcher in navigation works correctly
- [ ] Role-based permissions enforced correctly per agency context

**End-to-end**
- [ ] Full acceptance test scenario passes all twenty steps
- [ ] Automated isolation test suite passes with zero failures
- [ ] npx agency-flow init verified in a clean repository
- [ ] All data from Releases 1 through 4 intact after Release 5 migration
- [ ] All tests pass in CI before release is signed off
