import React, { useEffect, useState } from "react";
import axios from "axios";

function AdminTransactions() {
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchTransactions = async () => {
        setLoading(true);
        try {
            const res = await axios.get("http://localhost:8080/api/transactions");
            setTransactions(Array.isArray(res.data) ? res.data : []);
        } catch {
            setTransactions([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchTransactions();
    }, []);

    const callAdminAction = async (id, action) => {
        try {
            await axios.put(`http://localhost:8080/api/transactions/${id}/${action}`, {});
            await fetchTransactions();
        } catch (err) {
            const msg =
                err?.response?.data?.message ||
                err?.response?.data ||
                `Failed to ${action} transaction.`;
            alert(msg);
        }
    };

    const formatMoney = (value) =>
        Number(value || 0).toLocaleString("en-PH", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
        });

    return (
        <div className="admin-stack">
            <div className="admin-card">
                <h3>Escrow Transactions</h3>
                <p>Hold payment, release funds, or refund based on transaction status.</p>
            </div>

            {loading ? (
                <p>Loading transactions...</p>
            ) : transactions.length === 0 ? (
                <p>No transactions found.</p>
            ) : (
                <div className="admin-grid">
                    {transactions.map((tx) => (
                        <div className="admin-card" key={tx.id}>
                            <h3>{tx.itemTitle}</h3>
                            <p>Amount: ₱{formatMoney(tx.itemPrice)}</p>
                            <p>Status: {tx.status}</p>
                            <p>Buyer: {tx.buyerName}</p>
                            <p>Seller: {tx.sellerName}</p>
                            <div className="admin-inline-actions">
                                {tx.status === "PENDING" && (
                                    <button
                                        className="apple-btn primary small"
                                        onClick={() => callAdminAction(tx.id, "hold")}
                                    >
                                        Hold Payment
                                    </button>
                                )}
                                {tx.status === "DELIVERY_CONFIRMED" && (
                                    <button
                                        className="apple-btn primary small"
                                        onClick={() => callAdminAction(tx.id, "complete")}
                                    >
                                        Release Payment
                                    </button>
                                )}
                                {(tx.status === "PAYMENT_HELD" ||
                                    tx.status === "DELIVERY_CONFIRMED") && (
                                    <button
                                        className="apple-btn danger"
                                        onClick={() => callAdminAction(tx.id, "refund")}
                                    >
                                        Refund
                                    </button>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export default AdminTransactions;
