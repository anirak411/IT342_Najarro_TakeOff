import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { clearSessionStorage } from "../utils/session";
import { isSupabaseConfigured, supabase } from "../utils/supabaseClient";

function AuthCallback() {
    const navigate = useNavigate();

    useEffect(() => {
        let mounted = true;

        const completeGoogleLogin = async () => {
            if (!isSupabaseConfigured || !supabase) {
                alert("Supabase is not configured. Set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY.");
                navigate("/login", { replace: true });
                return;
            }

            const { data, error } = await supabase.auth.getSession();
            if (error || !data?.session) {
                alert(error?.message || "Google login failed.");
                navigate("/login", { replace: true });
                return;
            }

            const user = data.session.user;
            const email = (user?.email || "").trim();
            const meta = user?.user_metadata || {};
            const displayName = (meta.preferred_username || meta.name || email.split("@")[0] || "User").trim();
            const fullName = (meta.full_name || meta.name || displayName).trim();
            const profilePicUrl = (meta.avatar_url || "").trim();

            clearSessionStorage();
            localStorage.setItem(
                "user",
                JSON.stringify({
                    displayName,
                    fullName,
                    email,
                    profilePicUrl,
                    coverPicUrl: "",
                    role: "USER",
                })
            );
            localStorage.setItem("displayName", displayName);
            localStorage.setItem("fullName", fullName);
            localStorage.setItem("email", email);
            localStorage.setItem("role", "USER");

            if (mounted) {
                navigate("/dashboard", { replace: true });
            }
        };

        completeGoogleLogin();

        return () => {
            mounted = false;
        };
    }, [navigate]);

    return (
        <div className="auth-page glass-bg">
            <div className="auth-glass-card">
                <div className="auth-left">
                    <h2>Signing you in with Google...</h2>
                    <p>Please wait while we complete your login.</p>
                </div>
                <div className="auth-right" />
            </div>
        </div>
    );
}

export default AuthCallback;
