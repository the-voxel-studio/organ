import { Controller } from '@hotwired/stimulus';

export default class extends Controller {
    static targets = ['item', 'searchInput', 'organsSection', 'membersSection', 'tagsSection', 'content', 'chevron'];
    static values = {
        apiUrl: String,
        projectUuid: String
    };

    search() {
        const query = this.searchInputTarget.value.toLowerCase().trim();
        
        this.itemTargets.forEach(item => {
            const title = item.dataset.search.toLowerCase();
            if (title.includes(query)) {
                item.classList.remove('hidden');
            } else {
                item.classList.add('hidden');
            }
        });

        this.updateVisibility();
    }

    updateVisibility() {
        const sections = [
            { has: this.hasOrgansSectionTarget, target: this.organsSectionTarget, selector: '.trash-item[data-type="organ"]:not(.hidden)' },
            { has: this.hasMembersSectionTarget, target: this.membersSectionTarget, selector: '.trash-item[data-type="member"]:not(.hidden)' },
            { has: this.hasTagsSectionTarget, target: this.tagsSectionTarget, selector: '.trash-item[data-type="tag"]:not(.hidden)' }
        ];

        sections.forEach(s => {
            if (s.has) {
                const hasVisibleItems = this.element.querySelectorAll(s.selector).length > 0;
                if (hasVisibleItems) {
                    s.target.classList.remove('hidden');
                } else {
                    s.target.classList.add('hidden');
                }
            }
        });

        this.checkGlobalEmptyState();
    }

    toggleSection(event) {
        const btn = event.currentTarget;
        const section = btn.closest('.trash-section');
        const content = section.querySelector('[data-project-specific-trash-target="content"]');
        const chevron = section.querySelector('[data-project-specific-trash-target="chevron"]');

        content.classList.toggle('hidden');
        chevron.classList.toggle('rotate-180');
    }

    async restore(event) {
        const btn = event.currentTarget;
        const type = btn.dataset.type;
        const uuid = btn.dataset.uuid;
        const row = btn.closest('.trash-item');

        let url = '';
        if (type === 'organ') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${uuid}/restore`;
        } else if (type === 'member') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/members/${uuid}/restore`;
        } else if (type === 'tag') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/tags/${uuid}/restore`;
        }

        try {
            btn.disabled = true;
            btn.innerHTML = '<svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>';

            const response = await fetch(url, {
                method: 'POST',
                credentials: 'include'
            });

            if (!response.ok) throw new Error("Erreur lors de la restauration.");

            row.classList.add('opacity-0', 'scale-95', 'transition-all', 'duration-300');
            setTimeout(() => {
                row.remove();
                this.updateVisibility();
            }, 300);

        } catch (e) {
            alert(e.message);
            btn.disabled = false;
            btn.innerText = 'Restaurer';
        }
    }

    async removePermanent(event) {
        const btn = event.currentTarget;
        const type = btn.dataset.type;
        const uuid = btn.dataset.uuid;
        const row = btn.closest('.trash-item');

        if (!confirm("Cette action est irréversible. Voulez-vous supprimer cet élément définitivement ?")) {
            return;
        }

        let url = '';
        if (type === 'organ') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${uuid}?permanent=1`;
        } else if (type === 'tag') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/tags/${uuid}?permanent=1`;
        } else if (type === 'member') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/members/${uuid}?permanent=1`;
        }

        try {
            btn.disabled = true;
            const response = await fetch(url, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (response.ok) {
                row.classList.add('opacity-0', 'scale-95');
                setTimeout(() => {
                    row.remove();
                    this.updateVisibility();
                    this.checkGlobalEmptyState();
                }, 300);
            } else {
                btn.disabled = false;
                alert('Erreur lors de la suppression définitive.');
            }
        } catch (error) {
            btn.disabled = false;
            console.error('Hard delete failed', error);
        }
    }

    checkGlobalEmptyState() {
        const remainingItems = this.element.querySelectorAll('.trash-item').length;
        if (remainingItems === 0) {
            window.location.reload(); 
        }
    }
}
