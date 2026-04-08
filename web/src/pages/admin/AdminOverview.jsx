import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

function AdminOverview() {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [summary, setSummary] = useState({
        users: 0,
        items: 0,
        transactions: 0,
        pending: 0,
        held: 0,
    });

    const fetchSummary = async () => {
        setLoading(true);
        setError("");
        try {
            const [usersRes, itemsRes, txRes] = await Promise.all([
                axios.get("http://localhost:8080/api/users/admin"),
                axios.get("http://localhost:8080/api/items"),
                axios.get("http://localhost:8080/api/transactions"),
            ]);
            const users = Array.isArray(usersRes.data) ? usersRes.data : [];
            const items = Array.isArray(itemsRes.data) ? itemsRes.data : [];
            const transactions = Array.isArray(txRes.data) ? txRes.data : [];

            setSummary({
                users: users.length,
                items: items.length,
                transactions: transactions.length,
                pending: transactions.filter((tx) => tx.status === "PENDING").length,
                held: transactions.filter((tx) => tx.status === "PAYMENT_HELD").length,
            });
        } catch (err) {
            setSummary({
                users: 0,
                items: 0,
                transactions: 0,
                pending: 0,
                held: 0,
            });
            setError(
                err?.response?.data?.message ||
                err?.response?.data ||
                "Could not load admin metrics."
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchSummary();
    }, []);

    return (
        <div className="admin-stack">
            {loading ? (
                <p>Loading admin overview...</p>
            ) : error ? (
                <div className="admin-card">
                    <h3>Admin Access Error</h3>
                    <p>{error}</p>
                    <button className="apple-btn primary small" onClick={fetchSummary}>
                        Retry
                    </button>
                </div>
            ) : (
                <>
                    <div className="admin-metric-grid">
                        <div className="admin-card">
                            <p className="admin-metric-label">Users</p>
                            <h3>{summary.users}</h3>
                        </div>
                        <div className="admin-card">
                            <p className="admin-metric-label">Listings</p>
                            <h3>{summary.items}</h3>
                        </div>
                        <div className="admin-card">
                            <p className="admin-metric-label">Transactions</p>
                            <h3>{summary.transactions}</h3>
                        </div>
                        <div className="admin-card">
                            <p className="admin-metric-label">Pending Escrow</p>
                            <h3>{summary.pending}</h3>
                        </div>
                        <div className="admin-card">
                            <p className="admin-metric-label">Held Payments</p>
                            <h3>{summary.held}</h3>
                        </div>
                        <div className="admin-card">
                            <p className="admin-metric-label">Needs Action</p>
                            <h3>{summary.pending + summary.held}</h3>
                        </div>
                    </div>

                    <div className="admin-card">
                        <h3>Quick Actions</h3>
                        <div className="admin-inline-actions">
                            <button
                                className="apple-btn primary small"
                                onClick={() => navigate("/admin/transactions")}
                            >
                                Review Escrow Queue
                            </button>
                            <button
                                className="apple-btn primary small"
                                onClick={() => navigate("/admin/listings")}
                            >
                                Moderate Listings
                            </button>
                            <button
                                className="apple-btn primary small"
                                onClick={() => navigate("/admin/users")}
                            >
                                Manage User Roles
                            </button>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}

export default AdminOverview;
