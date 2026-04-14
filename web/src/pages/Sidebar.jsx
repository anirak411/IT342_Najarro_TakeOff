import React from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "../css/sidebar.css";
import { clearSessionStorage } from "../utils/session";

function Sidebar({ isOpen, onToggle }) {
    const navigate = useNavigate();
    const location = useLocation();
    let storedUser = null;
    try {
        storedUser = JSON.parse(localStorage.getItem("user"));
    } catch {
        storedUser = null;
    }
    const role = (storedUser?.role || "").toUpperCase();

    const goTo = (path) => {
        navigate(path);
        if (onToggle) onToggle();
    };

    const handleLogout = () => {
        clearSessionStorage();
        navigate("/login");
        if (onToggle) onToggle();
    };

    const navItems = [
        { label: "Marketplace", path: "/dashboard" },
        { label: "My Listings", path: "/my-items" },
        { label: "Profile", path: "/profile" },
        { label: "Settings", path: "/settings" },
        { label: "Transactions", path: "/transactions" },
        ...(role === "ADMIN"
            ? [{ label: "Admin Dashboard", path: "/admin" }]
            : []),
    ];

    const isActive = (path) =>
        path === "/admin"
            ? location.pathname === "/admin" || location.pathname.startsWith("/admin/")
            : location.pathname === path;

    return (
        <div className={`sidebar ${isOpen ? "open" : ""}`}>
            <div className="sidebar-header">
                <h2 className="sidebar-logo" onClick={() => goTo("/dashboard")}>
                    TradeOff
                </h2>
                <button
                    className="sidebar-toggle-btn"
                    onClick={onToggle}
                    aria-label="Collapse sidebar"
                >
                    ×
                </button>
            </div>

            <nav className="sidebar-links">
                {navItems.map((item) => (
                    <button
                        key={item.path}
                        onClick={() => goTo(item.path)}
                        className={isActive(item.path) ? "active" : ""}
                    >
                        {item.label}
                    </button>
                ))}
            </nav>

            <div className="sidebar-footer">
                <button className="logout-btn" onClick={handleLogout}>
                    Logout
                </button>
            </div>
        </div>
    );
}

export default Sidebar;
