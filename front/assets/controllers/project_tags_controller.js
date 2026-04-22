import { Controller } from '@hotwired/stimulus';
import { trans } from '@/translator';

export default class extends Controller {
    static targets = ['panel', 'list', 'form', 'nameInput', 'colorInput', 'submitBtn', 'spinner'];
    static values = {
        projectUuid: String,
        canManage: Boolean
    };

    connect() {
        this.editingUuid = null;
        this.tagsLoaded = false;
    }

    async togglePanel() {
        this.panelTarget.classList.toggle('hidden');
        if (!this.panelTarget.classList.contains('hidden') && !this.tagsLoaded) {
            await this.loadTags();
            this.tagsLoaded = true;
        }
    }

    async loadTags() {
        try {
            const response = await fetch(`http://localhost:8001/api/projects/${this.projectUuidValue}/tags`, {
                credentials: 'include'
            });
            if (response.ok) {
                const tags = await response.json();
                this.renderTags(tags);
            }
        } catch (error) {
            console.error('Failed to load tags:', error);
        }
    }

    renderTags(tags) {
        if (tags.length === 0) {
            this.listTarget.innerHTML = `<p class="text-sm text-gray-500 italic">${trans('project.show.no_tags') || 'Aucun tag disponible.'}</p>`;
            return;
        }

        this.listTarget.innerHTML = tags.map(tag => `
            <div class="flex items-center justify-between p-3 bg-white rounded-xl border border-gray-100 shadow-sm group">
                <div class="flex items-center gap-3">
                    <div class="w-4 h-4 rounded-full shadow-inner" style="background-color: ${tag.color}"></div>
                    <span class="text-sm font-bold text-gray-700">${tag.name}</span>
                </div>
                ${this.canManageValue ? `
                    <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-all">
                        <button data-action="click->project-tags#edit" 
                                data-tag-uuid="${tag.uuid}" 
                                data-tag-name="${tag.name}" 
                                data-tag-color="${tag.color}" 
                                class="p-2 text-gray-400 hover:text-black transition-colors cursor-pointer">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path></svg>
                        </button>
                        <button data-action="click->project-tags#delete" data-tag-uuid="${tag.uuid}" class="p-2 text-gray-400 hover:text-red-500 transition-colors cursor-pointer">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                        </button>
                    </div>
                ` : ''}
            </div>
        `).join('');
    }

    toggleForm() {
        this.formTarget.classList.toggle('hidden');
        if (this.formTarget.classList.contains('hidden')) {
            this.clearForm();
        }
    }

    clearForm() {
        this.nameInputTarget.value = '';
        this.colorInputTarget.value = '#808080';
        this.editingUuid = null;
    }

    edit(event) {
        const btn = event.currentTarget;
        const uuid = btn.dataset.tagUuid;
        const name = btn.dataset.tagName;
        const color = btn.dataset.tagColor;

        this.nameInputTarget.value = name;
        this.colorInputTarget.value = color;
        this.editingUuid = uuid;

        this.formTarget.classList.remove('hidden');
        this.nameInputTarget.focus();
    }

    async saveTag(event) {
        event.preventDefault();
        const name = this.nameInputTarget.value;
        const color = this.colorInputTarget.value;

        if (!name) return;

        this.submitBtnTarget.disabled = true;
        this.spinnerTarget.classList.remove('hidden');

        try {
            const method = this.editingUuid ? 'PUT' : 'POST';
            const endpoint = this.editingUuid 
                ? `http://localhost:8001/api/projects/${this.projectUuidValue}/tags/${this.editingUuid}`
                : `http://localhost:8001/api/projects/${this.projectUuidValue}/tags`;

            const response = await fetch(endpoint, {
                method: method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, color }),
                credentials: 'include'
            });

            if (response.ok) {
                this.clearForm();
                this.formTarget.classList.add('hidden');
                await this.loadTags();
            }
        } catch (error) {
            console.error('Failed to save tag:', error);
        } finally {
            this.submitBtnTarget.disabled = false;
            this.spinnerTarget.classList.add('hidden');
        }
    }

    async delete(event) {
        const tagUuid = event.currentTarget.dataset.tagUuid;
        if (!confirm(trans('project.show.delete_tag_confirm'))) return;

        try {
            const response = await fetch(`http://localhost:8001/api/projects/${this.projectUuidValue}/tags/${tagUuid}`, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (response.ok) {
                await this.loadTags();
            }
        } catch (error) {
            console.error('Failed to delete tag:', error);
        }
    }
}
