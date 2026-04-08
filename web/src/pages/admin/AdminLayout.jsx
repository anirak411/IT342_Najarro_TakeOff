import React, { useEffect, useState } from "react";
import axios from "axios";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import BackButton from "../../components/BackButton";
import "../../css/admin.css";
import { clearSessionStorage } from "../../utils/session";

function AdminLayout() {
    const navigate = useNavigate();
    const [isVerifying, setIsVerifying] = useState(true);
    const [adminLabel, setAdminLabel] = useState("Admin");

    useEffect(() => {
        let mounted = true;

        const verifyAdminSession = async () => {
            try {
                const res = await axios.get("http://localhost:8080/api/auth/session");
                const data = res?.data?.data || {};
                const role = (data?.role || "").toUpperCase();
                if (role !== "ADMIN") {
                    throw new Error("Admin access required.");
                }

                if (!mounted) return;
                setAdminLabel(data?.displayName || data?.email || "Admin");
            } catch {
                clearSessionStorage();
                if (mounted) {
                    navigate("/login", { replace: true });
                }
            } finally {
                if (mounted) {
                    setIsVerifying(false);
                }
            }
        };

        verifyAdminSession();

        return () => {
            mounted = false;
        };
    }, [navigate]);

    if (isVerifying) {
        return (
            <div className="admin-page">
                <div className="admin-shell">
                    <p>Verifying privileged admin session...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="admin-page">
            <div className="admin-shell">
                <BackButton className="admin-back-btn" fallback="/dashboard" />
                <div className="admin-header">
                    <div>
                        <h2 className="admin-title">Admin Dashboard</h2>
                        <p className="admin-subtitle">
                            Manage users, listings, and escrow transactions.
                        </p>
                    </div>
                    <div className="admin-header-actions">
                        <span className="admin-security-pill">
                            Privileged Mode: {adminLabel}
                        </span>
                        <button
                            className="apple-btn primary small"
                            onClick={() => navigate("/dashboard")}
                        >
                            Open Marketplace
                        </button>
                    </div>
                </div>

                <nav className="admin-nav">
                    <NavLink to="/admin" end>
                        Overview
                    </NavLink>
                    <NavLink to="/admin/transactions">
                        Transactions
                    </NavLink>
                    <NavLink to="/admin/listings">
                        Listings
                    </NavLink>
                    <NavLink to="/admin/users">
                        Users
                    </NavLink>
                </nav>

                <div className="admin-content">
                    <Outlet />
                </div>
            </div>
        </div>
    );
}

export default AdminLayout;
