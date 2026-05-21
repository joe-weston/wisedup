import Link from "next/link";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { signOut } from "@/app/actions";

export default async function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) {
    redirect("/login");
  }

  const { data: adminRow } = await supabase
    .from("school_admins")
    .select("school_id")
    .eq("user_id", user.id)
    .maybeSingle();

  if (!adminRow?.school_id) {
    return (
      <main className="mx-auto max-w-lg p-8 text-center">
        <h1 className="text-lg font-semibold">No school linked</h1>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Your account is signed in but has no{" "}
          <code className="rounded bg-white/10 px-1">school_admins</code> row.
          See{" "}
          <code className="rounded bg-white/10 px-1">docs/R3_ADMIN_PROVISIONING.md</code>.
        </p>
        <form action={signOut} className="mt-6">
          <button
            type="submit"
            className="text-sm text-[var(--accent)] underline underline-offset-2"
          >
            Sign out
          </button>
        </form>
      </main>
    );
  }

  const { data: school } = await supabase
    .from("schools")
    .select("name, code")
    .eq("id", adminRow.school_id)
    .single();

  const title = school?.name ?? "School";
  const code = school?.code ?? "";

  return (
    <div className="min-h-screen">
      <header className="border-b border-white/10 bg-[var(--card)]">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-4">
          <div>
            <p className="text-xs uppercase tracking-wide text-[var(--muted)]">
              WizedUp Admin
            </p>
            <p className="text-lg font-semibold text-white">
              {title}
              {code ? (
                <span className="ml-2 text-sm font-normal text-[var(--muted)]">
                  ({code})
                </span>
              ) : null}
            </p>
          </div>
          <nav className="flex flex-wrap items-center gap-4 text-sm">
            <Link className="text-[var(--muted)] hover:text-white" href="/school">
              School overview
            </Link>
            <Link className="text-[var(--muted)] hover:text-white" href="/students">
              Students
            </Link>
            <form action={signOut}>
              <button
                type="submit"
                className="rounded border border-white/15 px-3 py-1.5 text-[var(--muted)] hover:border-white/30 hover:text-white"
              >
                Sign out
              </button>
            </form>
          </nav>
        </div>
      </header>
      <div className="mx-auto max-w-6xl px-4 py-8">{children}</div>
    </div>
  );
}
