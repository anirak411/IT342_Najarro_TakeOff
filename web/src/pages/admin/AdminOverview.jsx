import React, { useEffect, useMemo, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { getCurrentEmail } from "../../utils/session";

function AdminOverview() {
    const navigate = useNavigate();
    const adminEmail = useMemo(() => getCurrentEmail(), []);
    const [loading, setLoading] = useState(true);
    const [summary, setSummary] = useState({
        users: 0,
        items: 0,
        transactions: 0,
        pending: 0,
        held: 0,
    });

    useEffect(() => {
        const fetchSummary = async () => {
            if (!adminEmail) return;
            setLoading(true);
            try {
                const [usersRes, itemsRes, txRes] = await Promise.all([
                    axios.get("http://localhost:8080/api/users"),
                    axios.get("http://localhost:8080/api/items"),
                    axios.get("http://localhost:8080/api/transactions", {
                        params: { adminEmail },
                    }),
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
            } catch {
                setSummary({
                    users: 0,
                    items: 0,
                    transactions: 0,
                    pending: 0,
                    held: 0,
                });
            } finally {
                setLoading(false);
            }
        };

        fetchSummary();
    }, [adminEmail]);

    return (
        <div className="admin-stack">
            {loading ? (
                <p>Loading admin overview...</p>
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
