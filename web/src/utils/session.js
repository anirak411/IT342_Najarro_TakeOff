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
