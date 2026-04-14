import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "../css/global.css";
import { isSupabaseConfigured, supabase } from "../utils/supabaseClient";

function Login() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();

        try {
            const res = await axios.post("http://localhost:8080/api/auth/login", {
                email,
                password,
            });

            if (res.data.success) {
                const sessionToken = (res.data?.data?.sessionToken || "").trim();

                localStorage.setItem(
                    "user",
                    JSON.stringify({
                        displayName: res.data.data.displayName,
                        fullName: res.data.data.fullName,
                        email: res.data.data.email,
                        profilePicUrl: res.data.data.profilePicUrl || "",
                        coverPicUrl: res.data.data.coverPicUrl || "",
                        role: res.data.data.role || "USER",
                    })
                );
                localStorage.setItem("displayName", res.data.data.displayName);
                localStorage.setItem("fullName", res.data.data.fullName);
                localStorage.setItem("email", res.data.data.email);
                localStorage.setItem("role", res.data.data.role || "USER");
                if (sessionToken) {
                    localStorage.setItem("sessionToken", sessionToken);
                } else {
                    localStorage.removeItem("sessionToken");
                }

                const nextRole = (res.data?.data?.role || "USER").toUpperCase();
                navigate(nextRole === "ADMIN" ? "/admin" : "/dashboard");
            } else {
                alert(res.data.message);
            }
        } catch (err) {
            const message =
                err?.response?.data?.message ||
                err?.message ||
                "Login failed.";
            alert(message);
        }
    };

    const handleGoogleLogin = async () => {
        if (!isSupabaseConfigured || !supabase) {
            alert("Supabase is not configured. Set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY.");
            return;
        }

        const redirectTo = `${window.location.origin}/auth/callback`;
        const { error } = await supabase.auth.signInWithOAuth({
            provider: "google",
            options: { redirectTo },
        });

        if (error) {
            alert(error.message || "Unable to start Google login.");
        }
    };

    return (
        <div className="auth-page glass-bg">
            <div className="auth-glass-card">
                <div className="auth-left">
                    <img
                        src="/src/images/logo.png"
                        alt="TradeOff Logo"
                        className="auth-logo"
                    />

                    <h2>Welcome Back!</h2>
                    <p>
                        Sign in to explore listings, trade items, and connect with students.
                    </p>

                    <form className="auth-form" onSubmit={handleLogin}>
                        <input
                            className="auth-input"
                            type="email"
                            placeholder="Email Address"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />

                        <input
                            className="auth-input"
                            type="password"
                            placeholder="Password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />

                        <button className="auth-button" type="submit">
                            Sign In
                        </button>

                        <button
                            className="auth-button google-login"
                            type="button"
                            onClick={handleGoogleLogin}
                        >
                            <span className="google-g" aria-hidden="true">G</span>
                            <span>Continue with Gmail</span>
                        </button>

                        <button
                            type="button"
                            className="auth-button outline"
                            onClick={() => navigate("/register")}
                        >
                            Create Account
                        </button>
                    </form>

                    <button className="back-link" onClick={() => navigate("/")}>
                        ← Back to Marketplace
                    </button>
                </div>

                <div className="auth-right">
                </div>
            </div>
        </div>
    );
}

export default Login;
