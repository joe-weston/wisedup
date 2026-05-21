"use client";

export function DateRangeForm({
  from,
  to,
  actionPath,
  extraHidden,
}: {
  from: string;
  to: string;
  /** Path for GET submit, e.g. `/school` or `/students` */
  actionPath: string;
  /** Additional fields merged into the GET request (e.g. reset pagination). */
  extraHidden?: Record<string, string>;
}) {
  return (
    <form
      method="get"
      action={actionPath}
      className="flex flex-wrap items-end gap-3 rounded-lg border border-white/10 bg-[var(--card)] p-4"
    >
      {extraHidden
        ? Object.entries(extraHidden).map(([name, value]) => (
            <input key={name} type="hidden" name={name} value={value} />
          ))
        : null}
      <label className="flex flex-col gap-1 text-xs text-[var(--muted)]">
        From (UTC)
        <input
          type="date"
          name="from"
          defaultValue={from}
          className="rounded border border-white/10 bg-black/30 px-2 py-1.5 text-sm text-white"
        />
      </label>
      <label className="flex flex-col gap-1 text-xs text-[var(--muted)]">
        To (UTC)
        <input
          type="date"
          name="to"
          defaultValue={to}
          className="rounded border border-white/10 bg-black/30 px-2 py-1.5 text-sm text-white"
        />
      </label>
      <button
        type="submit"
        className="rounded bg-white/10 px-3 py-1.5 text-sm font-medium text-white hover:bg-white/15"
      >
        Apply range
      </button>
    </form>
  );
}
