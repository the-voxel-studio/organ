import { Controller } from '@hotwired/stimulus';

export default class extends Controller {
    static targets = ['menu', 'list', 'badge', 'emptyState'];
    static values = {
        apiUrl: String,
        subscribeUrl: String
    };

    connect() {
        this.loadNotifications();
        this.setupMercure();
    }

    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
        }
    }

    async loadNotifications() {
        try {
            if (!this.apiUrlValue) return;
            const response = await fetch(this.apiUrlValue, {
                credentials: 'include',
                headers: {
                    'Accept': 'application/json'
                }
            });
            if (response.ok) {
                const notifications = await response.json();
                this.renderNotifications(notifications);
            }
        } catch (error) {
            console.error('Failed to load notifications:', error);
        }
    }

    async setupMercure() {
        if (!this.subscribeUrlValue) return;

        try {
            const response = await fetch(this.subscribeUrlValue, {
                credentials: 'include'
            });
            if (response.ok) {
                const data = await response.json();
                const hubUrl = new URL(data.hubUrl);
                hubUrl.searchParams.set('topic', data.topic);
                hubUrl.searchParams.set('authorization', data.token);

                this.eventSource = new EventSource(hubUrl.toString());
                this.eventSource.onmessage = (event) => {
                    const notification = JSON.parse(event.data);
                    this.addNotification(notification);
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

    renderNotifications(notifications) {
        if (notifications.length === 0) {
            this.emptyStateTarget.classList.remove('hidden');
            this.listTarget.innerHTML = '';
            this.updateBadge(0);
            return;
        }

        this.emptyStateTarget.classList.add('hidden');
        this.listTarget.innerHTML = notifications.map(n => this.notificationTemplate(n)).join('');
        
        const unreadCount = notifications.filter(n => !n.isRead).length;
        this.updateBadge(unreadCount);
    }

    addNotification(notification) {
        this.emptyStateTarget.classList.add('hidden');
        const html = this.notificationTemplate(notification);
        this.listTarget.insertAdjacentHTML('afterbegin', html);
        
        // Update badge
        const currentCount = parseInt(this.badgeTarget.innerText) || 0;
        this.updateBadge(currentCount + 1);
    }

    updateBadge(count) {
        if (count > 0) {
            this.badgeTarget.innerText = count > 9 ? '9+' : count;
            this.badgeTarget.classList.remove('hidden');
        } else {
            this.badgeTarget.classList.add('hidden');
        }
    }

    async markAsRead(event) {
        const uuid = event.currentTarget.dataset.uuid;
        const isRead = event.currentTarget.dataset.isRead === 'true';
        
        if (isRead) return;

        try {
            const response = await fetch(`${this.apiUrlValue}/${uuid}/read`, {
                method: 'PATCH',
                credentials: 'include'
            });
            if (response.ok) {
                event.currentTarget.classList.remove('bg-bubblegum/5', 'border-l-bubblegum');
                event.currentTarget.dataset.isRead = 'true';
                
                const dot = event.currentTarget.querySelector('.bg-bubblegum.rounded-full');
                if (dot) dot.classList.add('hidden');

                const currentCount = parseInt(this.badgeTarget.innerText) || 0;
                this.updateBadge(Math.max(0, currentCount - 1));
            }
        } catch (error) {
            console.error('Failed to mark as read:', error);
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
                <div class="p-3 ${unreadClass} rounded-xl border relative mb-2" data-uuid="${n.uuid}" data-is-read="${isRead}">
                    <div class="absolute top-3 right-3 w-2 h-2 rounded-full bg-bubblegum ${dotClass}"></div> 
                    <div class="flex items-start gap-3">
                        <div class="w-10 h-10 rounded-full bg-light-blue flex-shrink-0 flex items-center justify-center text-white font-bold text-sm ring-2 ring-white">
                            ${n.senderInitials || 'PJ'}
                        </div>
                        <div class="flex-1 pt-0.5">
                            <p class="text-sm text-black leading-snug">
                                ${n.message}
                            </p>
                            <span class="text-xs text-black/50 mt-1 block font-medium">${relativeDate}</span>
                            
                            <div class="flex gap-2 mt-3">
                                <button data-action="click->notifications#markAsRead" data-uuid="${n.uuid}" class="flex-1 px-3 py-1.5 bg-bubblegum text-white text-xs font-bold rounded-lg hover:bg-bubblegum/90 transition-all shadow-sm active:scale-95">
                                    Accepter
                                </button>
                                <button class="flex-1 px-3 py-1.5 bg-white text-black/70 border border-gray-200 text-xs font-bold rounded-lg hover:bg-gray-50 hover:text-black transition-all shadow-sm active:scale-95">
                                    Refuser
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }

        // Standard notification (Info)
        const hoverClass = isRead ? 'hover:bg-white border-transparent hover:border-gray-100' : 'bg-bubblegum/5 border-bubblegum/10';
        return `
            <div class="p-3 ${hoverClass} rounded-xl transition-colors cursor-pointer border mb-1" 
                 data-action="click->notifications#markAsRead" 
                 data-uuid="${n.uuid}"
                 data-is-read="${isRead}">
                <div class="flex items-start gap-3">
                    <div class="w-10 h-10 rounded-full bg-gray-100 flex-shrink-0 flex items-center justify-center text-black/40 ring-2 ring-white">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M10.125 2.25h-4.5c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125v-9M10.125 2.25h.375a9 9 0 0 1 9 9v.375M10.125 2.25A3.375 3.375 0 0 1 13.5 5.625v1.5c0 .621.504 1.125 1.125 1.125h1.5a3.375 3.375 0 0 1 3.375 3.375M9 15l2.25 2.25L15 12" />
                        </svg>
                    </div>
                    <div class="flex-1 pt-0.5">
                        <p class="text-sm text-black/80 leading-snug">
                            ${n.message}
                        </p>
                        <span class="text-xs text-black/40 mt-1 block">${relativeDate}</span>
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
