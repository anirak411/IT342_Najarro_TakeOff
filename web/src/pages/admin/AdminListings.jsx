import React, { useEffect, useState } from "react";
import axios from "axios";

function AdminListings() {
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
                    sellerEmail: item.sellerEmail || "",
                    sellerName: item.sellerName || "",
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

    const formatDate = (value) => {
        if (!value) return "Unknown date";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return "Unknown date";
        return date.toLocaleDateString("en-PH", {
            month: "short",
            day: "numeric",
            year: "numeric",
        });
    };

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
                            <div className="admin-listing-media">
                                {item.imageUrl ? (
                                    <img
                                        className="admin-listing-image"
                                        src={item.imageUrl}
                                        alt={`${item.title || "Listing"} photo`}
                                        loading="lazy"
                                    />
                                ) : (
                                    <div className="admin-listing-fallback">
                                        <span>{(item.title || "Listing").charAt(0).toUpperCase()}</span>
                                    </div>
                                )}
                            </div>
                            <div className="admin-listing-body">
                                <h3>{item.title || "Untitled Listing"}</h3>
                                <p className="admin-listing-price">₱{formatMoney(item.price)}</p>
                                <p className="admin-listing-meta">
                                    Seller: {item.sellerName || item.sellerEmail || "Unknown"}
                                </p>
                                <p className="admin-listing-meta">
                                    Category: {item.category || "N/A"} | Condition: {item.condition || "N/A"}
                                </p>
                                <p className="admin-listing-meta">
                                    Location: {item.location || "N/A"} | Posted: {formatDate(item.createdAt)}
                                </p>
                            </div>
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
