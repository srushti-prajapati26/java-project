import { useState } from "react";
import "./Auth.css";

const API_BASE = `${import.meta.env.VITE_API_BASE || "http://localhost:8080"}/api/auth`;

export default function Auth({ onLogin }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
  });
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function update(field, value) {
    setForm((previous) => ({ ...previous, [field]: value }));
    setMessage("");
    setError("");
  }

  async function submit(event) {
    event.preventDefault();
    setLoading(true);
    setMessage("");
    setError("");

    const endpoint =
      mode === "login"
        ? `${API_BASE}/login`
        : `${API_BASE}/register`;

    const body =
      mode === "login"
        ? { email: form.email, password: form.password }
        : form;

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      const text = await response.text();
      let data = null;
      try {
        data = text ? JSON.parse(text) : null;
      } catch {
        data = { message: text };
      }

      if (!response.ok) {
        throw new Error(data?.message || "Request failed.");
      }

      if (mode === "register") {
        setMode("login");
        setForm({ name: "", email: form.email, password: "" });
        setMessage("Registration successful. Please login.");
      } else {
        localStorage.setItem("ems_jwt", data.token);
        localStorage.setItem("ems_user", JSON.stringify(data.user));
        onLogin(data.user);
      }
    } catch (err) {
      setError(
        err.message ||
          "Could not connect to the Spring Boot server. Start the backend first."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <div className="auth-brand">EMS</div>
        <div className="auth-subtitle">EMPLOYEE MANAGEMENT SYSTEM</div>

        <h1>{mode === "login" ? "Welcome Back" : "Create Account"}</h1>
        <p className="auth-description">
          {mode === "login"
            ? "Login as an employer to manage employees and payroll."
            : "Create an employer account to manage the system."}
        </p>

        <form onSubmit={submit} className="auth-form">
          {mode === "register" && (
            <div className="auth-field">
              <label>EMPLOYER NAME</label>
              <input
                required
                minLength="2"
                maxLength="100"
                pattern="[A-Za-z][A-Za-z .'-]{1,99}"
                title="Use 2-100 letters, spaces, apostrophes, periods or hyphens."
                value={form.name}
                placeholder="Enter your name"
                onChange={(event) => update("name", event.target.value)}
              />
            </div>
          )}

          <div className="auth-field">
            <label>EMAIL</label>
            <input
              type="email"
              required
              maxLength="150"
              value={form.email}
              placeholder="name@example.com"
              onChange={(event) => update("email", event.target.value)}
            />
          </div>

          <div className="auth-field">
            <label>PASSWORD</label>
            <input
              type="password"
              minLength="8"
              maxLength="72"
              required
              value={form.password}
              placeholder="Minimum 8 characters"
              onChange={(event) => update("password", event.target.value)}
            />
          </div>

          {message && <div className="auth-success">{message}</div>}
          {error && <div className="auth-error">{error}</div>}

          <button className="auth-primary" disabled={loading}>
            {loading
              ? "Please wait..."
              : mode === "login"
                ? "LOGIN"
                : "REGISTER"}
          </button>
        </form>

        <button
          className="auth-switch"
          type="button"
          onClick={() => {
            setMode(mode === "login" ? "register" : "login");
            setMessage("");
            setError("");
          }}
        >
          {mode === "login"
            ? "Don't have an account? Register"
            : "Already have an account? Login"}
        </button>

        {mode === "login" && (
          <div className="demo-account">
            <strong>Demo employer account</strong>
            <span>admin@ems.com</span>
            <span>admin123</span>
          </div>
        )}
      </div>
    </div>
  );
}
