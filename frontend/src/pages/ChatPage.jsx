/**
 * MediGenius — Chat Page
 * Formerly the entire contents of App.jsx (see App.jsx, now a thin router shell).
 * All original anonymous-chat behavior (welcome screen, sessions, typing indicator,
 * dark mode, responsive sidebar, toasts) is preserved exactly. Additions:
 *   - Feature 6:  PDF upload button (attach icon) for logged-in users
 *   - Feature 7:  Voice input via the Web Speech API (mic button)
 *   - Feature 8:  Text-to-speech "Speak" button on every AI response
 *   - Feature 11: Sidebar now also shows Uploaded PDFs + auth-aware Profile/Login section
 *   - Feature 15: all network calls go through src/api/endpoints.js (axios + JWT), instead
 *                 of bare `fetch()` - so a logged-in user's messages are attributed to their
 *                 account automatically, with zero behavior change for anonymous users.
 */

// ══════════════════════════════════════════════════════════════
// SECTION 1 — IMPORTS
// ══════════════════════════════════════════════════════════════
import { useState, useEffect, useRef, useCallback } from 'react';
import { Link } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import { useAuth } from '../context/AuthContext';
import {
  sendChatMessage, clearChat as apiClearChat, createNewChat as apiCreateNewChat,
  fetchHistory, fetchSessions, fetchSession, deleteSessionById, uploadPdf,
} from '../api/endpoints';
import '../index.css';

// ══════════════════════════════════════════════════════════════
// SECTION 2 — UTILITY HELPERS
// ══════════════════════════════════════════════════════════════
function formatTimeAgo(timestamp) {
  const now = new Date();
  const past = new Date(timestamp);
  const diffMs = now - past;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;
  return past.toLocaleDateString();
}

function buildDownloadText(chatHistory) {
  let content = 'MediGenius Chat Export\n';
  content += '='.repeat(50) + '\n\n';
  chatHistory.forEach((msg) => {
    content += `[${msg.timestamp}] ${msg.type === 'user' ? 'You' : 'MediGenius'}:\n`;
    content += msg.content + '\n';
    if (msg.source) content += `Source: ${msg.source}\n`;
    content += '\n';
  });
  return content;
}

// NEW (Feature 7/8) - Web Speech API feature detection, done once at module load.
const SpeechRecognitionImpl =
  typeof window !== 'undefined' ? window.SpeechRecognition || window.webkitSpeechRecognition : null;
const speechSynthesisSupported = typeof window !== 'undefined' && 'speechSynthesis' in window;

// ══════════════════════════════════════════════════════════════
// SECTION 3 — SIDEBAR COMPONENT
// ══════════════════════════════════════════════════════════════
function Sidebar({
  sidebarOpen, sessions, currentSessionId, onNewChat, onLoadSession, onDeleteSession,
  onToggleTheme, theme, user, uploadedDocuments, onLogout,
}) {
  return (
    <aside className={`sidebar glass-effect${sidebarOpen ? '' : ' collapsed'}`}>
      <div className="sidebar-content">

        {/* Logo + New Chat */}
        <div className="sidebar-header">
          <div className="logo-wrapper">
            <div className="logo-animated">
              <div className="logo-pulse" />
              <i className="fas fa-heartbeat" />
            </div>
            <div className="logo-text">
              <h1>MediGenius</h1>
              <span className="version">AI Assistant v3.0</span>
            </div>
          </div>
          <button className="new-chat-btn" onClick={onNewChat}>
            <i className="fas fa-plus" />
            <span>New Chat</span>
          </button>
        </div>

        {/* Chat History */}
        <div className="chat-history-section">
          <div className="section-header">
            <span>Chat History</span>
            <div className="section-line" />
          </div>
          <div className="chat-list">
            {sessions === null ? (
              <div style={{ textAlign: 'center', padding: '20px', color: 'var(--text-tertiary)', fontSize: '13px' }}>
                <div className="loading-spinner" style={{ margin: '0 auto 10px' }} />
                Loading chats...
              </div>
            ) : sessions.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '20px', color: 'var(--text-tertiary)', fontSize: '13px' }}>
                No chat history yet
              </div>
            ) : (
              sessions.map((session) => (
                <div
                  key={session.session_id}
                  className={`chat-item${currentSessionId === session.session_id ? ' active' : ''}`}
                  onClick={() => onLoadSession(session.session_id)}
                >
                  <i className="fas fa-message" />
                  <div className="chat-item-content">
                    <div className="chat-item-title">{session.preview || 'New conversation'}</div>
                    <div className="chat-item-time">{formatTimeAgo(session.last_active)}</div>
                  </div>
                  <button
                    className="chat-item-delete"
                    onClick={(e) => { e.stopPropagation(); onDeleteSession(session.session_id); }}
                  >
                    <i className="fas fa-trash" />
                  </button>
                </div>
              ))
            )}
          </div>
        </div>

        {/* NEW (Feature 11) - Uploaded PDFs, only shown once logged in */}
        {user && (
          <div className="chat-history-section">
            <div className="section-header">
              <span>Uploaded PDFs</span>
              <div className="section-line" />
            </div>
            <div className="chat-list">
              {(uploadedDocuments || []).length === 0 ? (
                <div style={{ textAlign: 'center', padding: '14px', color: 'var(--text-tertiary)', fontSize: '12px' }}>
                  No PDFs yet — use the attach button
                </div>
              ) : (
                uploadedDocuments.map((doc) => (
                  <div key={doc.id} className="chat-item" style={{ cursor: 'default' }}>
                    <i className="fas fa-file-pdf" style={{ color: 'var(--danger)' }} />
                    <div className="chat-item-content">
                      <div className="chat-item-title">{doc.fileName}</div>
                      <div className="chat-item-time">{doc.chunkCount} chunks indexed</div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        )}

        {/* NEW (Feature 11) - Auth-aware footer: Profile/Logout, or Login/Signup */}
        <div className="sidebar-footer">
          {user ? (
            <div className="developer-card glass-effect">
              <Link to="/profile" className="dev-header" style={{ textDecoration: 'none', cursor: 'pointer' }}>
                <i className="fas fa-user-circle" />
                <span>{user.name}</span>
              </Link>
              <div className="dev-info">
                <p style={{ wordBreak: 'break-all' }}>{user.email}</p>
                <div className="social-links">
                  <Link to="/profile" className="social-link" title="Profile">
                    <i className="fas fa-id-card" />
                  </Link>
                  <button className="social-link" title="Logout" onClick={onLogout} style={{ border: 'none', cursor: 'pointer' }}>
                    <i className="fas fa-right-from-bracket" />
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="developer-card glass-effect">
              <div className="dev-header">
                <i className="fas fa-user" />
                <span>Guest</span>
              </div>
              <div className="dev-info">
                <p>Log in to save chats &amp; upload PDFs</p>
                <div className="social-links">
                  <Link to="/login" className="social-link" title="Log in">
                    <i className="fas fa-right-to-bracket" />
                  </Link>
                  <Link to="/register" className="social-link" title="Sign up">
                    <i className="fas fa-user-plus" />
                  </Link>
                </div>
              </div>
            </div>
          )}
          <button className="theme-btn glass-effect" onClick={onToggleTheme}>
            <i className={`fas ${theme === 'dark' ? 'fa-sun' : 'fa-moon'}`} />
          </button>
        </div>

      </div>
    </aside>
  );
}

// ══════════════════════════════════════════════════════════════
// SECTION 4 — CHAT AREA COMPONENT
// ══════════════════════════════════════════════════════════════
const QUICK_QUESTIONS = [
  { icon: 'fa-thermometer', label: 'Fever Symptoms', q: 'What are the symptoms of fever?' },
  { icon: 'fa-head-side-virus', label: 'Headache Treatment', q: 'How to treat a headache?' },
  { icon: 'fa-heart-pulse', label: 'High Blood Pressure', q: 'What causes high blood pressure?' },
  { icon: 'fa-notes-medical', label: 'Diabetes Management', q: 'Tell me about diabetes management' },
  { icon: 'fa-virus-covid', label: 'COVID Prevention', q: 'COVID-19 prevention tips' },
  { icon: 'fa-pills', label: 'Cold Remedies', q: 'Common cold remedies' },
];

function ChatArea({ messages, isTyping, showWelcome, onQuickQuestion, chatAreaRef }) {
  return (
    <div className="chat-area" ref={chatAreaRef}>

      {/* Welcome Screen */}
      <div className={`welcome-screen${showWelcome ? '' : ' hidden'}`}>
        <div className="welcome-content">
          <div className="logo-3d">
            <i className="fas fa-stethoscope" />
          </div>
          <h1 className="welcome-title">Welcome to MediGenius</h1>
          <p className="welcome-subtitle">Your AI-powered medical assistant is ready to help</p>

          <div className="quick-actions">
            <h3>Quick Questions:</h3>
            <div className="quick-buttons">
              {QUICK_QUESTIONS.map(({ icon, label, q }) => (
                <button key={q} className="quick-btn glass-effect" onClick={() => onQuickQuestion(q)}>
                  <i className={`fas ${icon}`} />
                  <span>{label}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="features">
            {[
              { icon: 'fa-brain', label: 'AI-Powered' },
              { icon: 'fa-database', label: 'Medical Database' },
              { icon: 'fa-shield-alt', label: 'Reliable Info' },
            ].map(({ icon, label }) => (
              <div key={label} className="feature-card glass-effect">
                <i className={`fas ${icon}`} />
                <span>{label}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Messages */}
      <div className="messages-container">
        {messages.map((msg, idx) => (
          <MessageBubble key={idx} msg={msg} />
        ))}
      </div>

      {/* Typing Indicator */}
      <div className={`typing-indicator${isTyping ? ' active' : ''}`}>
        <div className="typing-bubble glass-effect">
          <div className="typing-content">
            <span className="typing-text">MediGenius is thinking</span>
            <div className="typing-dots">
              <span className="dot" />
              <span className="dot" />
              <span className="dot" />
            </div>
          </div>
        </div>
      </div>

    </div>
  );
}

function MessageBubble({ msg }) {
  const [speaking, setSpeaking] = useState(false);

  const copyText = useCallback(() => {
    navigator.clipboard.writeText(msg.content).catch(() => { });
  }, [msg.content]);

  // NEW (Feature 8 - Text To Speech)
  const toggleSpeak = useCallback(() => {
    if (!speechSynthesisSupported) return;
    if (speaking) {
      window.speechSynthesis.cancel();
      setSpeaking(false);
      return;
    }
    window.speechSynthesis.cancel(); // stop any other message currently speaking
    const utterance = new SpeechSynthesisUtterance(msg.content);
    utterance.rate = 1;
    utterance.onend = () => setSpeaking(false);
    utterance.onerror = () => setSpeaking(false);
    window.speechSynthesis.speak(utterance);
    setSpeaking(true);
  }, [msg.content, speaking]);

  if (msg.type === 'user') {
    return (
      <div className="message user-message">
        <div className="message-wrapper">
          <div className="message-avatar"><i className="fas fa-user" /></div>
          <div className="message-content">
            <div className="message-text">
              {msg.content}
              <span className="message-time">{msg.timestamp}</span>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="message bot-message">
      <div className="message-wrapper">
        <div className="message-avatar"><i className="fas fa-robot" /></div>
        <div className="message-content">
          <div className="message-text">
            <ReactMarkdown>{msg.content}</ReactMarkdown>
          </div>
          <span className="message-time">{msg.timestamp}</span>
          <div className="message-footer">
            {msg.source && (
              <span className="message-source">
                <i className="fas fa-database" />
                {msg.source}
              </span>
            )}
            <div className="message-actions">
              {speechSynthesisSupported && (
                <button
                  className="message-action"
                  title={speaking ? 'Stop speaking' : 'Speak response aloud'}
                  onClick={toggleSpeak}
                >
                  <i className={`fas ${speaking ? 'fa-stop' : 'fa-volume-high'}`} />
                </button>
              )}
              <button className="message-action" title="Copy" onClick={copyText}>
                <i className="fas fa-copy" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════
// SECTION 5 — INPUT AREA COMPONENT
// ══════════════════════════════════════════════════════════════
function InputArea({
  inputValue, setInputValue, onSend, isTyping, inputRef,
  isLoggedIn, onUploadPdf, uploading,
  isListening, onToggleVoice, voiceSupported,
}) {
  const fileInputRef = useRef(null);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      onSend();
    }
  };

  const handleInput = (e) => {
    setInputValue(e.target.value);
    e.target.style.height = 'auto';
    e.target.style.height = Math.min(e.target.scrollHeight, 120) + 'px';
  };

  const handleAttachClick = () => {
    if (!isLoggedIn) {
      alert('Log in to upload and search your own PDFs.');
      return;
    }
    fileInputRef.current?.click();
  };

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (file) onUploadPdf(file);
    e.target.value = '';
  };

  return (
    <div className="input-area">
      <div className="input-wrapper">
        <div className="input-container glass-effect">
          <input
            type="file"
            accept="application/pdf"
            ref={fileInputRef}
            style={{ display: 'none' }}
            onChange={handleFileChange}
          />
          <button className="input-btn" title="Attach PDF" onClick={handleAttachClick} disabled={uploading}>
            <i className={`fas ${uploading ? 'fa-spinner fa-spin' : 'fa-paperclip'}`} />
          </button>
          <textarea
            ref={inputRef}
            className="message-input"
            placeholder={isListening ? 'Listening...' : 'Ask your medical question...'}
            rows={1}
            value={inputValue}
            onChange={handleInput}
            onKeyDown={handleKeyDown}
          />
          {voiceSupported && (
            <button
              className="input-btn"
              title={isListening ? 'Stop listening' : 'Voice input'}
              onClick={onToggleVoice}
              style={isListening ? { color: 'var(--danger)' } : undefined}
            >
              <i className={`fas ${isListening ? 'fa-microphone-lines' : 'fa-microphone'}`} />
            </button>
          )}
          <button
            className="send-btn"
            title="Send message"
            aria-label="Send message"
            onClick={onSend}
            disabled={!inputValue.trim() || isTyping}
          >
            <i className="fas fa-paper-plane" />
          </button>
        </div>
        <div className="input-info">
          <i className="fas fa-info-circle" />
          <span>AI can make mistakes. Always consult healthcare professionals for medical advice.</span>
        </div>
      </div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════
// SECTION 6 — CHAT PAGE ROOT (all state + API logic)
// ══════════════════════════════════════════════════════════════

// ── Mobile detection hook ──────────────────────────────────────
function useIsMobile(breakpoint = 768) {
  const [isMobile, setIsMobile] = useState(() => window.innerWidth <= breakpoint);
  useEffect(() => {
    const handler = () => setIsMobile(window.innerWidth <= breakpoint);
    window.addEventListener('resize', handler);
    return () => window.removeEventListener('resize', handler);
  }, [breakpoint]);
  return isMobile;
}

export default function ChatPage() {
  const { user, logout, refreshProfile } = useAuth();

  // ── State ──────────────────────────────────────────────────
  const [theme, setTheme] = useState(() => localStorage.getItem('theme') || 'light');
  const isMobile = useIsMobile();
  // On mobile default to closed; on desktop restore from localStorage
  const [sidebarOpen, setSidebarOpen] = useState(() => {
    if (window.innerWidth <= 768) return false;
    return localStorage.getItem('sidebarOpen') !== 'false';
  });
  const [sessions, setSessions] = useState(null);           // null = loading
  const [currentSessionId, setCurrentSessionId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [chatHistory, setChatHistory] = useState([]);       // for download
  const [showWelcome, setShowWelcome] = useState(true);
  const [isTyping, setIsTyping] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [toast, setToast] = useState({ show: false, message: '', type: 'success' });
  const [uploading, setUploading] = useState(false);
  const [isListening, setIsListening] = useState(false);

  const chatAreaRef = useRef(null);
  const inputRef = useRef(null);
  const toastTimerRef = useRef(null);
  const recognitionRef = useRef(null);

  // ── Theme ──────────────────────────────────────────────────
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => setTheme(t => t === 'light' ? 'dark' : 'light');

  // ── Sidebar ────────────────────────────────────────────────
  const toggleSidebar = () => {
    setSidebarOpen(prev => {
      if (!isMobile) localStorage.setItem('sidebarOpen', !prev);
      return !prev;
    });
  };

  const closeSidebar = () => setSidebarOpen(false);

  // ── Toast ──────────────────────────────────────────────────
  const showToast = useCallback((message, type = 'success') => {
    if (toastTimerRef.current) clearTimeout(toastTimerRef.current);
    setToast({ show: true, message, type });
    toastTimerRef.current = setTimeout(() => setToast(t => ({ ...t, show: false })), 3000);
  }, []);

  // ── Scroll to bottom ───────────────────────────────────────
  const scrollToBottom = useCallback(() => {
    if (chatAreaRef.current) {
      chatAreaRef.current.scrollTo({ top: chatAreaRef.current.scrollHeight, behavior: 'smooth' });
    }
  }, []);

  useEffect(() => { scrollToBottom(); }, [messages, isTyping, scrollToBottom]);

  // ── Load sessions ──────────────────────────────────────────
  const loadSessions = useCallback(async () => {
    try {
      const data = await fetchSessions();
      if (data.success && data.sessions) setSessions(data.sessions);
    } catch {
      setSessions([]);
    }
  }, []);

  // ── Load current history on mount ──────────────────────────
  useEffect(() => {
    loadSessions();
    (async () => {
      try {
        const data = await fetchHistory();
        if (data.success && data.messages && data.messages.length > 0) {
          const msgs = data.messages.map(m => ({
            type: m.role === 'user' ? 'user' : 'assistant',
            content: m.content,
            timestamp: m.timestamp || '',
            source: m.source || null,
          }));
          setMessages(msgs);
          setChatHistory(msgs.map(m => ({ ...m })));
          setShowWelcome(false);
        }
      } catch { /* silent */ }
    })();
  }, [loadSessions]);

  // ── Load session ───────────────────────────────────────────
  const loadSession = useCallback(async (sessionId) => {
    try {
      const data = await fetchSession(sessionId);
      if (data.success) {
        setCurrentSessionId(sessionId);
        const msgs = data.messages.map(m => ({
          type: m.role === 'user' ? 'user' : 'assistant',
          content: m.content,
          timestamp: m.timestamp || '',
          source: m.source || null,
        }));
        setMessages(msgs);
        setChatHistory(msgs.map(m => ({ ...m })));
        setShowWelcome(false);
        showToast('Chat loaded successfully', 'success');
      }
    } catch {
      showToast('Failed to load chat', 'error');
    }
  }, [showToast]);

  // ── Delete session ─────────────────────────────────────────
  const deleteSession = useCallback(async (sessionId) => {
    if (!window.confirm('Are you sure you want to delete this chat?')) return;
    try {
      await deleteSessionById(sessionId);
      await loadSessions();
      if (currentSessionId === sessionId) createNewChat();
      showToast('Chat deleted successfully', 'success');
    } catch {
      showToast('Failed to delete chat', 'error');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentSessionId, loadSessions, showToast]);

  // ── New chat ───────────────────────────────────────────────
  const createNewChat = useCallback(async () => {
    try {
      await apiCreateNewChat();
      setMessages([]);
      setChatHistory([]);
      setCurrentSessionId(null);
      setShowWelcome(true);
      await loadSessions();
      showToast('New chat created', 'success');
    } catch {
      showToast('Failed to create new chat', 'error');
    }
  }, [loadSessions, showToast]);

  // ── Clear chat ─────────────────────────────────────────────
  const clearChat = useCallback(async () => {
    if (!window.confirm('Are you sure you want to clear this conversation?')) return;
    try {
      await apiClearChat();
      setMessages([]);
      setChatHistory([]);
      setShowWelcome(true);
      showToast('Conversation cleared', 'success');
    } catch {
      showToast('Failed to clear conversation', 'error');
    }
  }, [showToast]);

  // ── Download chat ──────────────────────────────────────────
  const downloadChat = useCallback(() => {
    if (chatHistory.length === 0) { showToast('No messages to download', 'error'); return; }
    const content = buildDownloadText(chatHistory);
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `medigenius-chat-${Date.now()}.txt`;
    a.click();
    URL.revokeObjectURL(url);
    showToast('Chat downloaded successfully', 'success');
  }, [chatHistory, showToast]);

  // ── Send message ───────────────────────────────────────────
  const sendMessage = useCallback(async (overrideText) => {
    const message = (overrideText ?? inputValue).trim();
    if (!message || isTyping) return;

    setShowWelcome(false);
    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const userMsg = { type: 'user', content: message, timestamp: time, source: null };
    setMessages(prev => [...prev, userMsg]);
    setChatHistory(prev => [...prev, userMsg]);
    setInputValue('');
    if (inputRef.current) { inputRef.current.style.height = 'auto'; }
    setIsTyping(true);

    try {
      const data = await sendChatMessage(message);

      if (data.success) {
        const botMsg = {
          type: 'assistant',
          content: data.response,
          timestamp: data.timestamp || time,
          source: data.source || null,
        };
        setMessages(prev => [...prev, botMsg]);
        setChatHistory(prev => [...prev, botMsg]);
        showToast('Response received', 'success');
        await loadSessions();
        if (user) refreshProfile(); // keeps sidebar/profile conversation count in sync
      } else {
        const errMsg = { type: 'assistant', content: 'Sorry, I encountered an error. Please try again.', timestamp: time, source: null };
        setMessages(prev => [...prev, errMsg]);
        showToast('Error occurred', 'error');
      }
    } catch {
      const errMsg = { type: 'assistant', content: 'Connection error. Please check your internet and try again.', timestamp: time, source: null };
      setMessages(prev => [...prev, errMsg]);
      showToast('Connection error', 'error');
    } finally {
      setIsTyping(false);
    }
  }, [inputValue, isTyping, loadSessions, showToast, user, refreshProfile]);

  // Quick question handler
  const handleQuickQuestion = useCallback((q) => {
    setTimeout(() => sendMessage(q), 200);
  }, [sendMessage]);

  // ── NEW (Feature 6) - PDF upload ────────────────────────────
  const handleUploadPdf = useCallback(async (file) => {
    setUploading(true);
    try {
      await uploadPdf(file);
      showToast(`"${file.name}" uploaded and indexed`, 'success');
      await refreshProfile();
    } catch (err) {
      showToast(err.response?.data?.error || 'Failed to upload PDF', 'error');
    } finally {
      setUploading(false);
    }
  }, [showToast, refreshProfile]);

  // ── NEW (Feature 7) - Voice input via Web Speech API ────────
  const toggleVoiceInput = useCallback(() => {
    if (!SpeechRecognitionImpl) return;

    if (isListening) {
      recognitionRef.current?.stop();
      return;
    }

    const recognition = new SpeechRecognitionImpl();
    recognition.lang = 'en-US';
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      setInputValue(prev => (prev ? prev + ' ' : '') + transcript);
    };
    recognition.onerror = () => showToast('Voice input error - please try again', 'error');
    recognition.onend = () => setIsListening(false);

    recognitionRef.current = recognition;
    recognition.start();
    setIsListening(true);
  }, [isListening, showToast]);

  // ── Toast colors ───────────────────────────────────────────
  const toastColors = {
    success: 'linear-gradient(135deg, #10b981, #059669)',
    error: 'linear-gradient(135deg, #ef4444, #dc2626)',
    info: 'linear-gradient(135deg, #3b82f6, #2563eb)',
  };
  const toastIcons = {
    success: 'fa-check-circle',
    error: 'fa-exclamation-circle',
    info: 'fa-info-circle',
  };

  // ── Render ─────────────────────────────────────────────────
  return (
    <>
      {/* Animated Background */}
      <div className="animated-background">
        <div className="gradient-overlay" />
        <div className="floating-circles">
          <div className="circle circle-1" />
          <div className="circle circle-2" />
          <div className="circle circle-3" />
        </div>
      </div>

      <div className="app-container">

        {/* Sidebar Toggle */}
        <button className="sidebar-toggle-btn" onClick={toggleSidebar}>
          <i className="fas fa-bars" />
        </button>

        {/* Mobile backdrop — closes sidebar on click */}
        {isMobile && sidebarOpen && (
          <div className="sidebar-backdrop" onClick={closeSidebar} />
        )}

        {/* Sidebar */}
        <Sidebar
          sidebarOpen={sidebarOpen}
          sessions={sessions}
          currentSessionId={currentSessionId}
          onNewChat={createNewChat}
          onLoadSession={loadSession}
          onDeleteSession={deleteSession}
          onToggleTheme={toggleTheme}
          theme={theme}
          user={user}
          uploadedDocuments={user?.uploadedDocuments}
          onLogout={logout}
        />

        {/* Main Content */}
        <main className={`main-content${sidebarOpen ? ' sidebar-open' : ''}`}>

          {/* Header */}
          <header className="app-header glass-header">
            <div className="header-content">
              <h2 className="gradient-text">Medical AI Assistant</h2>
              <div className="status-indicator">
                <div className="status-ring">
                  <span className="ring-pulse" />
                </div>
                <span>AI Ready</span>
              </div>
            </div>
            <div className="header-actions">
              <button className="action-btn" title="Clear conversation" onClick={clearChat}>
                <i className="fas fa-trash" />
              </button>
              <button className="action-btn" title="Download chat" onClick={downloadChat}>
                <i className="fas fa-download" />
              </button>
              {!user && (
                <Link className="action-btn" title="Log in" to="/login">
                  <i className="fas fa-right-to-bracket" />
                </Link>
              )}
            </div>
          </header>

          {/* Chat Area */}
          <ChatArea
            messages={messages}
            isTyping={isTyping}
            showWelcome={showWelcome}
            onQuickQuestion={handleQuickQuestion}
            chatAreaRef={chatAreaRef}
          />

          {/* Input Area */}
          <InputArea
            inputValue={inputValue}
            setInputValue={setInputValue}
            onSend={() => sendMessage()}
            isTyping={isTyping}
            inputRef={inputRef}
            isLoggedIn={!!user}
            onUploadPdf={handleUploadPdf}
            uploading={uploading}
            isListening={isListening}
            onToggleVoice={toggleVoiceInput}
            voiceSupported={!!SpeechRecognitionImpl}
          />

        </main>
      </div>

      {/* Toast Notification */}
      <div
        className={`toast${toast.show ? ' show' : ''}`}
        style={{ background: toastColors[toast.type] }}
      >
        <i className={`fas ${toastIcons[toast.type]}`} />
        <span>{toast.message}</span>
      </div>
    </>
  );
}
