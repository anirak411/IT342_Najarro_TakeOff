import React from "react";
import ReactDOM from "react-dom/client";
import axios from "axios";
import App from "./App";
import { BrowserRouter } from "react-router-dom";
import "./index.css";
import { getSessionToken } from "./utils/session";

axios.defaults.baseURL = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/+$/, "");

axios.interceptors.request.use((config) => {
    const sessionToken = getSessionToken();
    if (sessionToken) {
        config.headers = {
            ...(config.headers || {}),
            "X-Session-Token": sessionToken,
        };
    }
    return config;
});

ReactDOM.createRoot(document.getElementById("root")).render(
    <BrowserRouter>
        <App />
    </BrowserRouter>
);
