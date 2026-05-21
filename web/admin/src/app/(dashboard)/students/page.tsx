import Link from "next/link";
import { DateRangeForm } from "@/components/date-range-form";
import {
  StudentsRosterCsvButton,
  type RosterRow,
} from "@/components/students-roster-csv-button";
import { parseRangeFromSearchParams } from "@/lib/dateRange";
import { createClient } from "@/lib/supabase/server";

const PAGE_SIZE = 25;

export default async function StudentsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const raw = await searchParams;
  const { from, to } = await parseRangeFromSearchParams(searchParams);
  const pageRaw = Array.isArray(raw.page) ? raw.page[0] : raw.page;
  const page = Math.max(1, Number.parseInt(pageRaw ?? "1", 10) || 1);
  const fromIdx = (page - 1) * PAGE_SIZE;

  const supabase = await createClient();
  const {
    data: rows,
    count,
    error,
  } = await supabase
    .from("students")
    .select("id, display_name, created_at", { count: "exact" })
    .order("display_name", { ascending: true })
    .range(fromIdx, fromIdx + PAGE_SIZE - 1);

  if (error) {
    return (
      <p className="text-sm text-red-400">
        Could not load students: {error.message}
      </p>
    );
  }

  const total = count ?? 0;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const roster: RosterRow[] = (rows ?? []).map((r) => ({
    id: r.id,
    display_name: r.display_name,
    created_at: r.created_at,
  }));

  const qs = new URLSearchParams({ from, to });
  const pageLink = (p: number) => `/students?${qs.toString()}&page=${p}`;

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-white">Students</h1>
          <p className="mt-1 text-sm text-[var(--muted)]">
            {total} enrolled · showing page {page} of {totalPages}
          </p>
        </div>
        <StudentsRosterCsvButton rows={roster} />
      </div>

      <DateRangeForm from={from} to={to} actionPath="/students" extraHidden={{ page: "1" }} />

      <div className="overflow-x-auto rounded-lg border border-white/10">
        <table className="w-full min-w-[32rem] text-left text-sm">
          <thead className="border-b border-white/10 bg-black/20 text-xs uppercase text-[var(--muted)]">
            <tr>
              <th className="px-4 py-3">Display name</th>
              <th className="px-4 py-3">Student ID</th>
              <th className="px-4 py-3">Registered (UTC)</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {roster.map((s) => (
              <tr
                key={s.id}
                className="border-b border-white/5 last:border-0 hover:bg-white/[0.03]"
              >
                <td className="px-4 py-3 text-white">{s.display_name}</td>
                <td className="px-4 py-3 font-mono text-xs text-[var(--muted)]">
                  {s.id}
                </td>
                <td className="px-4 py-3 text-[var(--muted)]">
                  {new Date(s.created_at).toISOString().replace("T", " ").slice(0, 19)}
                </td>
                <td className="px-4 py-3 text-right">
                  <Link
                    className="text-[var(--accent)] hover:underline"
                    href={`/students/${s.id}?${qs.toString()}`}
                  >
                    View
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
        <Link
          className={
            page <= 1
              ? "pointer-events-none text-[var(--muted)] opacity-40"
              : "text-[var(--accent)] hover:underline"
          }
          href={pageLink(page - 1)}
          aria-disabled={page <= 1}
        >
          Previous
        </Link>
        <span className="text-[var(--muted)]">
          Page {page} / {totalPages}
        </span>
        <Link
          className={
            page >= totalPages
              ? "pointer-events-none text-[var(--muted)] opacity-40"
              : "text-[var(--accent)] hover:underline"
          }
          href={pageLink(page + 1)}
          aria-disabled={page >= totalPages}
        >
          Next
        </Link>
      </div>

      <p className="text-xs text-[var(--muted)]">
        Date range applies to the student detail view. Roster CSV reflects the current
        page only (same rows as the table).
      </p>
    </div>
  );
}
