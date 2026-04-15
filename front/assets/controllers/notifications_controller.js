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
        
        // Close menu on click outside
        this.clickOutsideHandler = this.clickOutside.bind(this);
        document.addEventListener('click', this.clickOutsideHandler);
    }

    disconnect() {
        document.removeEventListener('click', this.clickOutsideHandler);
        if (this.eventSource) {
            this.eventSource.close();
        }
    }

    async loadNotifications() {
        try {
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
                this.eventSource = new EventSource(`${data.hubUrl}?topic=${encodeURIComponent(data.topic)}`);
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

    clickOutside(event) {
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
        const unreadClass = n.isRead ? '' : 'bg-bubblegum/5 border-l-4 border-l-bubblegum';
        const dotClass = n.isRead ? 'hidden' : 'block';
        
        return `
            <div class="p-4 border-b border-gray-100 hover:bg-bubblegum/[0.08] transition-all cursor-pointer ${unreadClass}" 
                 data-action="click->notifications#markAsRead" 
                 data-uuid="${n.uuid}"
                 data-is-read="${n.isRead}">
                <div class="flex justify-between items-start mb-1 gap-2">
                    <div class="flex items-center gap-2">
                        <span class="w-2 h-2 bg-bubblegum rounded-full ${dotClass}"></span>
                        <span class="text-[11px] font-bold text-bubblegum uppercase tracking-wider">${n.type.replace('_', ' ')}</span>
                    </div>
                    <span class="text-[10px] text-gray-400 whitespace-nowrap font-medium">${relativeDate}</span>
                </div>
                <p class="text-sm text-gray-700 leading-snug">${n.message}</p>
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
