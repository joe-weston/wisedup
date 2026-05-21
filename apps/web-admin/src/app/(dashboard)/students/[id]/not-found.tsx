import Link from "next/link";

export default function StudentNotFound() {
  return (
    <div className="mx-auto max-w-md py-16 text-center">
      <h1 className="text-xl font-semibold text-white">Student not found</h1>
      <p className="mt-2 text-sm text-[var(--muted)]">
        This ID is missing or not part of your school.
      </p>
      <Link href="/students" className="mt-6 inline-block text-[var(--accent)] hover:underline">
        Back to students
      </Link>
    </div>
  );
}
