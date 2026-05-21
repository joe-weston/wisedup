"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createBrowserSupabase } from "@/lib/supabase/browser";

export function LoginForm() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      const supabase = createBrowserSupabase();
      const { error: signErr } = await supabase.auth.signInWithPassword({
        email: email.trim(),
        password,
      });
      if (signErr) {
        setError(signErr.message);
        return;
      }
      router.push("/school");
      router.refresh();
    } finally {
      setPending(false);
    }
  }

  return (
    <form
      onSubmit={onSubmit}
      className="mx-auto flex w-full max-w-sm flex-col gap-4 rounded-xl border border-white/10 bg-[var(--card)] p-8 shadow-lg"
    >
      <h1 className="text-xl font-semibold text-white">Admin sign in</h1>
      <p className="text-sm text-[var(--muted)]">
        Use the email and password provisioned for your school.
      </p>
      <label className="flex flex-col gap-1 text-sm">
        <span className="text-[var(--muted)]">Email</span>
        <input
          className="rounded border border-white/10 bg-black/30 px-3 py-2 text-white outline-none focus:border-[var(--accent)]"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
      </label>
      <label className="flex flex-col gap-1 text-sm">
        <span className="text-[var(--muted)]">Password</span>
        <input
          className="rounded border border-white/10 bg-black/30 px-3 py-2 text-white outline-none focus:border-[var(--accent)]"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
      </label>
      {error ? (
        <p className="text-sm text-red-400" role="alert">
          {error}
        </p>
      ) : null}
      <button
        type="submit"
        disabled={pending}
        className="rounded bg-[var(--accent)] py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {pending ? "Signing in…" : "Sign in"}
      </button>
    </form>
  );
}
