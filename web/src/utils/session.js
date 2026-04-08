export const getStoredUser = () => {
    try {
        return JSON.parse(localStorage.getItem("user") || "null");
    } catch {
        return null;
    }
};

export const getCurrentRole = () => {
    const storedUser = getStoredUser();
    return (storedUser?.role || localStorage.getItem("role") || "USER").toUpperCase();
};

export const getCurrentEmail = () => {
    const storedUser = getStoredUser();
    return (storedUser?.email || localStorage.getItem("email") || "").trim();
};

export const isAuthenticated = () => Boolean(getCurrentEmail());

export const isAdminUser = () => getCurrentRole() === "ADMIN";

export const getSessionToken = () =>
    (localStorage.getItem("sessionToken") || "").trim();

export const clearSessionStorage = () => {
    localStorage.removeItem("sessionToken");
    localStorage.removeItem("user");
    localStorage.removeItem("displayName");
    localStorage.removeItem("fullName");
    localStorage.removeItem("email");
    localStorage.removeItem("role");
};
