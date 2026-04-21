import { Controller } from '@hotwired/stimulus';
import { trans } from '../translator.js';

export default class extends Controller {
    static targets = ['menu', 'list', 'badge', 'emptyState', 'loadMoreBtn'];
    static values = {
        apiUrl: String,
        subscribeUrl: String
    };

    connect() {
        this.allNotifications = [];
        this.displayedCount = 10;
        this.loadNotifications();
        this.setupMercure();
    }

    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
        }
    }

    triggerMarkAsRead(event) {
        // Prevent event from bubbling up to parents if necessary, 
        // but here we want to make sure we catch it.
        this.handleMouseEnter(event.currentTarget);
    }

    stop(event) {
        event.stopPropagation();
    }

    async loadNotifications() {
        try {
            if (!this.apiUrlValue) return;
            
            const commonHeaders = { 'Accept': 'application/json' };

            const notifPromise = fetch(this.apiUrlValue, {
                credentials: 'include',
                headers: commonHeaders
            });

            const apiBase = this.apiUrlValue.replace('/notifications', '');
            const invitePromise = fetch(`${apiBase}/invitations`, {
                credentials: 'include',
                headers: commonHeaders
            });

            const [notifRes, inviteRes] = await Promise.all([notifPromise, invitePromise]);

            let notifications = [];
            let invitations = [];

            if (notifRes.ok) notifications = await notifRes.json();
            if (inviteRes.ok) invitations = await inviteRes.json();

            const inviteNotifications = invitations.map(inv => ({
                uuid: inv.uuid,
                type: 'PROJECT_INVITATION',
                message: `Vous avez été invité à rejoindre le projet "${inv.projectName}" par ${inv.invitedBy}`,
                isRead: false,
                createdAt: inv.createdAt,
                isRealInvite: true
            }));

            this.allNotifications = [...inviteNotifications, ...notifications];
            this.renderNotifications();

        } catch (error) {
            console.error('Failed to load notifications:', error);
        }
    }

    async setupMercure() {
        if (!this.subscribeUrlValue) return;

        try {
            const response = await fetch(this.subscribeUrlValue, {
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });
            if (response.ok) {
                const data = await response.json();
                const hubUrl = new URL(data.hubUrl);
                hubUrl.searchParams.set('topic', data.topic);
                hubUrl.searchParams.set('authorization', data.token);

                this.eventSource = new EventSource(hubUrl.toString());
                this.eventSource.onmessage = (event) => {
                    const notification = JSON.parse(event.data);
                    if (notification.type === 'PROJECT_INVITATION') {
                        this.loadNotifications();
                    } else {
                        this.allNotifications.unshift(notification);
                        this.renderNotifications();
                    }
                };
            }
        } catch (error) {
            console.error('Failed to setup Mercure:', error);
        }
    }

    toggle() {
        this.menuTarget.classList.toggle('hidden');
    }

    hide(event) {
        if (!this.element.contains(event.target) && !this.menuTarget.classList.contains('hidden')) {
            this.menuTarget.classList.add('hidden');
        }
    }

    renderNotifications() {
        // --- Sorting Priority Logic ---
        this.allNotifications.sort((a, b) => {
            // 1. Project Invitations (isRealInvite) first
            if (a.isRealInvite && !b.isRealInvite) return -1;
            if (!a.isRealInvite && b.isRealInvite) return 1;

            // 2. Unread before Read
            if (!a.isRead && b.isRead) return -1;
            if (a.isRead && !b.isRead) return 1;

            // 3. By Date DESC
            return new Date(b.createdAt) - new Date(a.createdAt);
        });

        if (this.allNotifications.length === 0) {
            this.emptyStateTarget.classList.remove('hidden');
            this.listTarget.innerHTML = '';
            if (this.hasLoadMoreBtnTarget) this.loadMoreBtnTarget.classList.add('hidden');
            this.updateBadge(0);
            return;
        }

        this.emptyStateTarget.classList.add('hidden');
        
        // Pagination: slice the array
        const toDisplay = this.allNotifications.slice(0, this.displayedCount);
        this.listTarget.innerHTML = toDisplay.map(n => this.notificationTemplate(n)).join('');
        
        // Show/Hide "Load More" button
        if (this.hasLoadMoreBtnTarget) {
            if (this.allNotifications.length > this.displayedCount) {
                this.loadMoreBtnTarget.classList.remove('hidden');
            } else {
                this.loadMoreBtnTarget.classList.add('hidden');
            }
        }

        const unreadCount = this.allNotifications.filter(n => !n.isRead).length;
        this.updateBadge(unreadCount);
    }

    loadMore() {
        this.displayedCount += 10;
        this.renderNotifications();
    }

    updateBadge(count) {
        if (count > 0) {
            this.badgeTarget.innerText = count > 9 ? '9+' : count;
            this.badgeTarget.classList.remove('hidden');
        } else {
            this.badgeTarget.classList.add('hidden');
        }
    }

    handleMouseEnter(target) {
        const uuid = target.dataset.uuid;
        const isRead = target.getAttribute('data-is-read') === 'true';
        const isInvite = target.getAttribute('data-is-invite') === 'true';
        
        if (isRead || isInvite || target.dataset.marking === 'true') return;

        this.markAsRead(uuid, target);
    }

    async markAsRead(uuid, element) {
        if (element) element.dataset.marking = 'true';

        try {
            const response = await fetch(`${this.apiUrlValue}/${uuid}/read`, {
                method: 'PATCH',
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });
            
            if (response.ok) {
                // Update internal state
                const notif = this.allNotifications.find(n => n.uuid === uuid);
                if (notif) notif.isRead = true;
                
                this.updateLocalNotificationState(uuid);
            }
        } catch (error) {
            console.error('Failed to mark as read:', error);
        } finally {
            if (element) delete element.dataset.marking;
        }
    }

    async markAllAsRead() {
        const unreadNotifications = this.allNotifications.filter(n => !n.isRead && !n.isRealInvite);
        
        for (const n of unreadNotifications) {
            await this.markAsRead(n.uuid, null);
        }
        
        this.renderNotifications();
    }

    updateLocalNotificationState(uuid) {
        const allItems = document.querySelectorAll(`[data-uuid="${uuid}"]`);
        allItems.forEach(item => {
            item.classList.remove('bg-bubblegum/5', 'border-bubblegum/20', 'border-bubblegum/10');
            item.classList.add('bg-white', 'border-gray-100');
            item.setAttribute('data-is-read', 'true');
            const dot = item.querySelector('.unread-dot');
            if (dot) dot.classList.add('hidden');
        });

        this.refreshUnreadCount();
    }

    refreshUnreadCount() {
        const unreadCount = this.allNotifications.filter(n => !n.isRead).length;
        this.updateBadge(unreadCount);
    }

    async acceptInvitation(event) {
        event.stopPropagation();
        const inviteUuid = event.currentTarget.dataset.inviteUuid;

        try {
            const apiBase = this.apiUrlValue.replace('/notifications', '');
            const response = await fetch(`${apiBase}/invitations/${inviteUuid}/accept`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });

            if (response.ok) {
                window.location.reload();
            } else {
                const data = await response.json();
                alert(data.message || "Impossible d'accepter l'invitation");
            }
        } catch (error) {
            console.error('Failed to accept invitation:', error);
        }
    }

    async refuseInvitation(event) {
        event.stopPropagation();
        const inviteUuid = event.currentTarget.dataset.inviteUuid;

        try {
            const apiBase = this.apiUrlValue.replace('/notifications', '');
            const response = await fetch(`${apiBase}/invitations/${inviteUuid}/refuse`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });

            if (response.ok) {
                this.loadNotifications();
            }
        } catch (error) {
            console.error('Failed to refuse invitation:', error);
        }
    }

    notificationTemplate(n) {
        const date = new Date(n.createdAt);
        const relativeDate = this.formatRelativeDate(date);
        const isRead = n.isRead;
        
        if (n.type === 'PROJECT_INVITATION') {
            const unreadClass = isRead ? 'bg-white border-gray-100' : 'bg-bubblegum/5 border-bubblegum/20';
            const dotClass = isRead ? 'hidden' : '';
            
            return `
                <div class="p-4 ${unreadClass} rounded-xl border relative mb-2 transition-all" 
                     data-uuid="${n.uuid}" 
                     data-is-read="${isRead}" 
                     data-is-invite="true"
                     data-action="mouseenter->notifications#triggerMarkAsRead click->notifications#triggerMarkAsRead">
                    <div class="absolute top-4 right-4 w-2 h-2 rounded-full bg-bubblegum unread-dot ${dotClass}"></div> 
                    <div class="flex items-start gap-3">
                        <div class="w-10 h-10 rounded-full bg-bubblegum/10 flex-shrink-0 flex items-center justify-center text-bubblegum font-bold text-sm ring-2 ring-white">
                            PJ
                        </div>
                        <div class="flex-1 pt-0.5">
                            <p class="text-sm text-black leading-snug font-medium">
                                ${n.message}
                            </p>
                            <span class="text-xs text-black/40 mt-1 block">${relativeDate}</span>
                            
                            ${n.isRealInvite ? `
                                <div class="flex gap-2 mt-3">
                                    <button data-action="click->notifications#acceptInvitation" 
                                            data-invite-uuid="${n.uuid}"
                                            class="flex-1 px-3 py-1.5 bg-bubblegum text-white text-xs font-bold rounded-lg hover:bg-bubblegum/90 transition-all shadow-sm active:scale-95">
                                        Accepter
                                    </button>
                                    <button data-action="click->notifications#refuseInvitation" 
                                            data-invite-uuid="${n.uuid}"
                                            class="flex-1 px-3 py-1.5 bg-white text-black/70 border border-gray-200 text-xs font-bold rounded-lg hover:bg-gray-50 hover:text-black transition-all shadow-sm active:scale-95">
                                        Refuser
                                    </button>
                                </div>
                            ` : ''}
                        </div>
                    </div>
                </div>
            `;
        }

        const bgClass = isRead ? 'bg-white border-gray-100 hover:bg-gray-50/50' : 'bg-bubblegum/5 border-bubblegum/10';
        return `
            <div class="p-4 ${bgClass} rounded-xl transition-all cursor-pointer border mb-2 relative group" 
                 data-uuid="${n.uuid}"
                 data-is-read="${isRead}"
                 data-is-invite="false"
                 data-action="mouseenter->notifications#triggerMarkAsRead click->notifications#triggerMarkAsRead">
                ${!isRead ? '<div class="absolute top-4 right-4 w-1.5 h-1.5 rounded-full bg-bubblegum unread-dot"></div>' : ''}
                <div class="flex items-start gap-3">
                    <div class="w-10 h-10 rounded-full bg-gray-50 flex-shrink-0 flex items-center justify-center text-black/30 ring-2 ring-white transition-colors group-hover:bg-white">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                    </div>
                    <div class="flex-1 pt-0.5">
                        <p class="text-sm text-black/70 leading-snug font-medium transition-colors group-hover:text-black">
                            ${n.message}
                        </p>
                        <span class="text-xs text-black/30 mt-1 block group-hover:text-black/50">${relativeDate}</span>
                    </div>
                </div>
            </div>
        `;
    }

    formatRelativeDate(date) {
        const now = new Date();
        const diffInSeconds = Math.floor((now - date) / 1000);
        if (diffInSeconds < 60) return 'À l\'instant';
        if (diffInSeconds < 3600) return `Il y a ${Math.floor(diffInSeconds / 60)} min`;
        if (diffInSeconds < 86400) return `Il y a ${Math.floor(diffInSeconds / 3600)} h`;
        if (diffInSeconds < 604800) return `Il y a ${Math.floor(diffInSeconds / 86400)} j`;
        return date.toLocaleDateString();
    }
}
