/**
 * MODIFIED (Features 1, 15). The original tests mocked global `fetch`, but chat calls now
 * go through the axios-based API layer in src/api/endpoints.js (Feature 15), so we mock
 * that module directly instead - the standard approach for testing components against a
 * typed API layer rather than the raw transport. Test *intent* (render structure, session
 * loading, sending a message, new chat) is unchanged from the original suite.
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import App from './App';
import * as endpoints from './api/endpoints';

describe('App Integration', () => {
    beforeEach(() => {
        vi.spyOn(endpoints, 'fetchSessions').mockResolvedValue({ success: true, sessions: [] });
        vi.spyOn(endpoints, 'fetchHistory').mockResolvedValue({ success: true, messages: [] });
        vi.spyOn(endpoints, 'sendChatMessage').mockResolvedValue({
            success: true, response: 'ok', source: null, timestamp: 'now',
        });
        vi.spyOn(endpoints, 'createNewChat').mockResolvedValue({ success: true, session_id: 'new-session' });
        vi.spyOn(endpoints, 'clearChat').mockResolvedValue({ success: true });
        vi.spyOn(endpoints, 'fetchSession').mockResolvedValue({ success: true, messages: [] });
        vi.spyOn(endpoints, 'deleteSessionById').mockResolvedValue({ success: true });
    });

    afterEach(() => {
        vi.restoreAllMocks();
        localStorage.clear();
    });

    it('renders app structure', async () => {
        render(<App />);
        expect(await screen.findByText('Medical AI Assistant')).toBeInTheDocument();
        expect(screen.getByText('MediGenius')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Ask your medical question...')).toBeInTheDocument();
    });

    it('loads sessions on mount', async () => {
        const mockSessions = [{ session_id: '1', preview: 'Flu symptoms', last_active: '2023' }];
        endpoints.fetchSessions.mockResolvedValue({ success: true, sessions: mockSessions });

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Flu symptoms')).toBeInTheDocument();
        });
    });

    it('sends a message and displays response', async () => {
        endpoints.sendChatMessage.mockResolvedValue({
            success: true, response: 'I can help with that.', source: 'test-source', timestamp: 'now',
        });

        render(<App />);

        const input = await screen.findByPlaceholderText('Ask your medical question...');
        fireEvent.change(input, { target: { value: 'Headache' } });

        const sendBtn = screen.getByRole('button', { name: /Send message/i });
        fireEvent.click(sendBtn);

        expect(screen.getByText('Headache')).toBeInTheDocument(); // Optimistic UI

        await waitFor(() => {
            expect(screen.getByText('I can help with that.')).toBeInTheDocument();
        });
    });

    it('creates new chat', async () => {
        render(<App />);

        const newChatBtn = await screen.findByText('New Chat');
        fireEvent.click(newChatBtn);

        await waitFor(() => {
            expect(endpoints.createNewChat).toHaveBeenCalled();
        });
    });
});
