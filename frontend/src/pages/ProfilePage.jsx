import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './ProfilePage.css';

export default function ProfilePage() {
  const { user, logout, refreshProfile } = useAuth();
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    setRefreshing(true);
    refreshProfile().finally(() => setRefreshing(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!user) return null;

  const initials = user.name
    ? user.name.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase()
    : '?';

  return (
    <div className="profile-page">
      <div className="profile-topbar">
        <Link to="/" className="profile-back-link">
          <i className="fas fa-arrow-left" /> Back to chat
        </Link>
        <button className="profile-logout-btn" onClick={logout}>
          <i className="fas fa-right-from-bracket" /> Logout
        </button>
      </div>

      <div className="profile-card glass-effect">
        <div className="profile-avatar">{initials}</div>
        <h1>{user.name}</h1>
        <span className="profile-role-badge">{user.role}</span>

        <div className="profile-detail-list">
          <div className="profile-detail-row">
            <i className="fas fa-envelope" />
            <span>{user.email}</span>
          </div>
          <div className="profile-detail-row">
            <i className="fas fa-calendar" />
            <span>Joined {new Date(user.createdAt).toLocaleDateString()}</span>
          </div>
        </div>

        <div className="profile-stats-grid">
          <div className="profile-stat-card">
            <span className="profile-stat-value">{refreshing ? '…' : user.totalConversations}</span>
            <span className="profile-stat-label">Conversations</span>
          </div>
          <div className="profile-stat-card">
            <span className="profile-stat-value">
              {refreshing ? '…' : (user.uploadedDocuments || []).length}
            </span>
            <span className="profile-stat-label">Uploaded PDFs</span>
          </div>
        </div>
      </div>

      <div className="profile-docs-card glass-effect">
        <h2>
          <i className="fas fa-file-pdf" /> Uploaded PDFs
        </h2>
        {(user.uploadedDocuments || []).length === 0 ? (
          <p className="profile-empty-state">No PDFs uploaded yet. Use the attach button in chat to add one.</p>
        ) : (
          <ul className="profile-doc-list">
            {user.uploadedDocuments.map((doc) => (
              <li key={doc.id}>
                <i className="fas fa-file-pdf" />
                <div className="profile-doc-meta">
                  <span className="profile-doc-name">{doc.fileName}</span>
                  <span className="profile-doc-sub">
                    {doc.chunkCount} chunks · {(doc.fileSizeBytes / 1024).toFixed(0)} KB ·{' '}
                    {new Date(doc.uploadedAt).toLocaleDateString()}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
