import { useState } from 'react';
import { Link } from 'react-router-dom';
import './AuthPages.css';

/**
 * NEW (Feature 10 - "Forgot Password UI"). The spec only asked for the UI, not a real
 * password-reset backend flow (no email service, no reset-token endpoint exists in the
 * API). This page collects the email and shows the standard "check your inbox" success
 * state so the UI is complete; wire it up to a real POST /api/auth/forgot-password
 * endpoint later if/when that's added.
 */
export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    setSubmitted(true);
  };

  return (
    <div className="auth-page">
      <div className="auth-card glass-effect">
        <div className="auth-logo">
          <i className="fas fa-heartbeat" />
          <span>MediGenius</span>
        </div>

        {submitted ? (
          <>
            <h1>Check your email</h1>
            <p className="auth-subtitle">
              If an account exists for <strong>{email}</strong>, a password reset link is on its way.
            </p>
          </>
        ) : (
          <>
            <h1>Reset your password</h1>
            <p className="auth-subtitle">Enter your email and we'll send you a reset link</p>
            <form onSubmit={handleSubmit} className="auth-form">
              <label>
                Email
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  required
                  autoFocus
                />
              </label>
              <button type="submit" className="auth-submit-btn">
                Send Reset Link
              </button>
            </form>
          </>
        )}

        <div className="auth-links">
          <span>
            Remembered it? <Link to="/login">Back to login</Link>
          </span>
        </div>
      </div>
    </div>
  );
}
