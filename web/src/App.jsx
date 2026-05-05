import React, { useState } from "react";
import { Routes, Route, Navigate, useLocation } from "react-router-dom";

import Register from "./features/auth/Register";
import Login from "./features/auth/Login";
import AuthCallback from "./features/auth/AuthCallback";
import Dashboard from "./features/dashboard/Dashboard";
import LandingPage from "./features/dashboard/LandingPage";
import Profile from "./features/profile/Profile";
import ItemDetails from "./features/item/ItemDetails";
import Settings from "./features/profile/Settings";
import SellerProfile from "./features/profile/SellerProfile";
import ChatWidget from "./features/chat/ChatWidget";
import MyItems from "./features/item/MyItems";
import Sidebar from "./features/dashboard/Sidebar";
import Transactions from "./features/transaction/Transactions";
import AdminLayout from "./features/admin/AdminLayout";
import AdminOverview from "./features/admin/AdminOverview";
import AdminTransactions from "./features/admin/AdminTransactions";
import AdminListings from "./features/admin/AdminListings";
import AdminUsers from "./features/admin/AdminUsers";
import { getSessionToken, isAdminUser, isAuthenticated } from "./utils/session";

function App() {
    const location = useLocation();
    const storedUser = localStorage.getItem("user");
    const authed = isAuthenticated();
    const admin = isAdminUser();
    const hasSessionToken = Boolean(getSessionToken());
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const hideChatOn = ["/landing", "/login", "/register"];
    const showChat = Boolean(storedUser) && !hideChatOn.includes(location.pathname);
    const hideSidebarTriggerOn = ["/dashboard", "/profile"];
    const showUniversalSidebar =
        showChat && !hideSidebarTriggerOn.includes(location.pathname);

    const RequireAuth = ({ children }) =>
        authed ? children : <Navigate to="/login" replace />;

    const RequireAdmin = ({ children }) => {
        if (!authed) return <Navigate to="/login" replace />;
        if (!admin || !hasSessionToken) return <Navigate to="/dashboard" replace />;
        return children;
    };

    return (
        <>
            <Routes>
                <Route path="/" element={<Navigate to="/landing" />} />

                <Route path="/landing" element={<LandingPage />} />
                <Route path="/register" element={<Register />} />
                <Route path="/login" element={<Login />} />
                <Route path="/auth/callback" element={<AuthCallback />} />

                <Route path="/dashboard" element={<RequireAuth><Dashboard /></RequireAuth>} />
                <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />
                <Route path="/my-items" element={<RequireAuth><MyItems /></RequireAuth>} />

                <Route path="/item/:id" element={<RequireAuth><ItemDetails /></RequireAuth>} />

                <Route path="/seller/:sellerName" element={<RequireAuth><SellerProfile /></RequireAuth>} />

                <Route path="/settings" element={<RequireAuth><Settings /></RequireAuth>} />
                <Route path="/transactions" element={<RequireAuth><Transactions /></RequireAuth>} />

                <Route path="/admin" element={<RequireAdmin><AdminLayout /></RequireAdmin>}>
                    <Route index element={<AdminOverview />} />
                    <Route path="transactions" element={<AdminTransactions />} />
                    <Route path="listings" element={<AdminListings />} />
                    <Route path="users" element={<AdminUsers />} />
                </Route>
            </Routes>

            {showUniversalSidebar && (
                <>
                    <button
                        className="global-sidebar-trigger"
                        onClick={() => setSidebarOpen((prev) => !prev)}
                        aria-label="Toggle sidebar"
                    >
                        ☰
                    </button>
                    <Sidebar
                        isOpen={sidebarOpen}
                        onToggle={() => setSidebarOpen((prev) => !prev)}
                    />
                </>
            )}

            {showChat && <ChatWidget />}
        </>
    );
}

export default App;
