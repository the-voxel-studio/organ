import { Controller } from '@hotwired/stimulus';
import { trans } from '@/translator';

export default class extends Controller {
    static targets = ['list', 'urlInput', 'descriptionInput', 'form', 'submitBtn', 'spinner'];
    static values = {
        projectUuid: String,
        organUuid: String,
        canManage: Boolean
    };

    connect() {
        this.editingUuid = null;
        this.loadLinks();
    }

    async loadLinks() {
        try {
            const response = await fetch(`http://localhost:8001/api/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/links`, {
                credentials: 'include'
            });
            if (response.ok) {
                const links = await response.json();
                this.renderLinks(links);
            }
        } catch (error) {
            console.error('Failed to load links:', error);
        }
    }

    toggleForm() {
        this.formTarget.classList.toggle('hidden');
        if (this.formTarget.classList.contains('hidden')) {
            this.clearForm();
        }
    }

    clearForm() {
        this.urlInputTarget.value = '';
        this.descriptionInputTarget.value = '';
        this.editingUuid = null;
    }

    cancelEdit() {
        this.clearForm();
        this.formTarget.classList.add('hidden');
    }

    edit(event) {
        const btn = event.currentTarget;
        const uuid = btn.dataset.linkUuid;
        const url = btn.dataset.linkUrl;
        const description = btn.dataset.linkDescription;

        this.urlInputTarget.value = url;
        this.descriptionInputTarget.value = description === 'null' ? '' : description;
        this.editingUuid = uuid;

        this.formTarget.classList.remove('hidden');
        this.urlInputTarget.focus();
    }

    renderLinks(links) {
        if (links.length === 0) {
            this.listTarget.innerHTML = `<p class="text-sm text-gray-500 italic">${trans('organ.show.no_links')}</p>`;
            return;
        }

        const highlightColor = document.querySelector('[data-controller="organ-view"]')?.style.getPropertyValue('--highlight-color') || '#FF7DD4';

        this.listTarget.innerHTML = links.map(link => `
            <div class="flex items-center justify-between p-3 bg-white rounded-xl border border-gray-100 shadow-sm group">
                <div class="flex flex-col">
                    <a href="${link.url}" target="_blank" class="text-sm font-semibold hover:underline flex items-center gap-1" style="color: ${highlightColor}">
                        ${link.url}
                        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"></path></svg>
                    </a>
                    ${link.description ? `<span class="text-xs text-gray-500">${link.description}</span>` : ''}
                </div>
                ${this.canManageValue ? `
                    <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-all">
                        <button data-action="click->organ-links#edit" 
                                data-link-uuid="${link.uuid}" 
                                data-link-url="${link.url}" 
                                data-link-description="${link.description}" 
                                class="p-2 text-gray-400 hover:text-highlight transition-colors cursor-pointer">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path></svg>
                        </button>
                        <button data-action="click->organ-links#delete" data-link-uuid="${link.uuid}" class="p-2 text-gray-400 hover:text-red-500 transition-colors cursor-pointer">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                        </button>
                    </div>
                ` : ''}
            </div>
        `).join('');
    }

    async saveLink(event) {
        event.preventDefault();
        const url = this.urlInputTarget.value;
        const description = this.descriptionInputTarget.value;

        if (!url) return;

        // Start loading
        this.submitBtnTarget.disabled = true;
        this.spinnerTarget.classList.remove('hidden');

        try {
            const method = this.editingUuid ? 'PUT' : 'POST';
            const endpoint = this.editingUuid 
                ? `http://localhost:8001/api/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/links/${this.editingUuid}`
                : `http://localhost:8001/api/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/links`;

            const response = await fetch(endpoint, {
                method: method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ url, description }),
                credentials: 'include'
            });

            if (response.ok) {
                this.clearForm();
                this.formTarget.classList.add('hidden');
                this.loadLinks();
            }
        } catch (error) {
            console.error('Failed to save link:', error);
        } finally {
            // Stop loading
            this.submitBtnTarget.disabled = false;
            this.spinnerTarget.classList.add('hidden');
        }
    }

    async delete(event) {
        const linkUuid = event.currentTarget.dataset.linkUuid;
        if (!confirm(trans('organ.show.delete_link_confirm'))) return;

        try {
            const response = await fetch(`http://localhost:8001/api/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/links/${linkUuid}`, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (response.ok) {
                this.loadLinks();
            }
        } catch (error) {
            console.error('Failed to delete link:', error);
        }
    }
}
