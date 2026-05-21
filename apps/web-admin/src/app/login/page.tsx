import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { LoginForm } from "./login-form";

export default async function LoginPage() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (user) {
    redirect("/school");
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center px-4">
      <LoginForm />
    </main>
  );
}
