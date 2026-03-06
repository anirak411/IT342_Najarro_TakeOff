import React, { useEffect, useMemo, useState } from "react";
import axios from "axios";
import { getCurrentEmail } from "../../utils/session";

function AdminListings() {
    const adminEmail = useMemo(() => getCurrentEmail(), []);
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchItems = async () => {
        setLoading(true);
        try {
            const res = await axios.get("http://localhost:8080/api/items");
            setItems(Array.isArray(res.data) ? res.data : []);
        } catch {
            setItems([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchItems();
    }, []);

    const deleteListing = async (item) => {
        if (!window.confirm(`Delete listing "${item.title}"?`)) return;

        try {
            await axios.delete(`http://localhost:8080/api/items/${item.id}`, {
                params: {
                    sellerEmail: item.sellerEmail || adminEmail,
                    sellerName: item.sellerName || "",
                    adminEmail,
                },
            });
            await fetchItems();
        } catch (err) {
            const msg =
                err?.response?.data?.message ||
                err?.response?.data ||
                "Failed to delete listing.";
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
                <h3>Listing Moderation</h3>
                <p>Review all marketplace listings and remove policy-violating content.</p>
            </div>

            {loading ? (
                <p>Loading listings...</p>
            ) : items.length === 0 ? (
                <p>No listings available.</p>
            ) : (
                <div className="admin-grid">
                    {items.map((item) => (
                        <div className="admin-card" key={item.id}>
                            <h3>{item.title}</h3>
                            <p>Seller: {item.sellerName || item.sellerEmail}</p>
                            <p>Price: ₱{formatMoney(item.price)}</p>
                            <p>Category: {item.category || "N/A"}</p>
                            <button
                                className="apple-btn danger"
                                onClick={() => deleteListing(item)}
                            >
                                Delete Listing
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export default AdminListings;
