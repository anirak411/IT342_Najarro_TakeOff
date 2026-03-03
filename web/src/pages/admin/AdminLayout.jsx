import React from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import BackButton from "../../components/BackButton";
import "../../css/admin.css";

function AdminLayout() {
    const navigate = useNavigate();

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
                    <button
                        className="apple-btn primary small"
                        onClick={() => navigate("/dashboard")}
                    >
                        Open Marketplace
                    </button>
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
