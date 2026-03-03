import React, { useEffect, useMemo, useState } from "react";
import axios from "axios";
import { getCurrentEmail } from "../../utils/session";

function AdminUsers() {
    const adminEmail = useMemo(() => getCurrentEmail(), []);
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [query, setQuery] = useState("");

    const fetchUsers = async () => {
        setLoading(true);
        try {
            const res = await axios.get("http://localhost:8080/api/users");
            setUsers(Array.isArray(res.data) ? res.data : []);
        } catch {
            setUsers([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const updateRole = async (userId, role) => {
        try {
            await axios.put(`http://localhost:8080/api/users/${userId}/role`, {
                adminEmail,
                role,
            });
            await fetchUsers();
        } catch (err) {
            const msg =
                err?.response?.data?.message ||
                err?.response?.data ||
                "Failed to update role.";
            alert(msg);
        }
    };

    const filteredUsers = users.filter((user) => {
        const term = query.trim().toLowerCase();
        if (!term) return true;
        return [
            user.fullName,
            user.displayName,
            user.email,
            user.role,
        ]
            .filter(Boolean)
            .join(" ")
            .toLowerCase()
            .includes(term);
    });

    return (
        <div className="admin-stack">
            <div className="admin-card">
                <h3>User Management</h3>
                <p>Search users and assign ADMIN or USER role access.</p>
                <input
                    className="admin-search"
                    placeholder="Search by name, email, username, or role..."
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                />
            </div>

            {loading ? (
                <p>Loading users...</p>
            ) : filteredUsers.length === 0 ? (
                <p>No users found.</p>
            ) : (
                <div className="admin-grid">
                    {filteredUsers.map((user) => {
                        const role = (user.role || "USER").toUpperCase();
                        const isSelf = user.email?.toLowerCase() === adminEmail.toLowerCase();
                        return (
                            <div className="admin-card" key={user.id}>
                                <h3>{user.fullName || user.displayName || "Unnamed User"}</h3>
                                <p>Username: {user.displayName || "N/A"}</p>
                                <p>Email: {user.email}</p>
                                <p>Role: {role}</p>
                                <div className="admin-inline-actions">
                                    {role !== "ADMIN" && (
                                        <button
                                            className="apple-btn primary small"
                                            onClick={() => updateRole(user.id, "ADMIN")}
                                        >
                                            Make Admin
                                        </button>
                                    )}
                                    {role !== "USER" && !isSelf && (
                                        <button
                                            className="apple-btn danger"
                                            onClick={() => updateRole(user.id, "USER")}
                                        >
                                            Remove Admin
                                        </button>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

export default AdminUsers;
