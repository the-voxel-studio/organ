import { Controller } from '@hotwired/stimulus';

export default class extends Controller {
    static targets = ['item', 'searchInput', 'tasksSection', 'rolesSection', 'membersSection', 'linksSection', 'content', 'chevron'];
    static values = {
        apiUrl: String,
        projectUuid: String,
        organUuid: String
    };

    connect() {
        this.onTaskSaved = () => {
            window.location.reload();
        };
        window.addEventListener('task-saved', this.onTaskSaved);
    }

    disconnect() {
        window.removeEventListener('task-saved', this.onTaskSaved);
    }

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
            { has: this.hasTasksSectionTarget, target: this.tasksSectionTarget, selector: '.trash-item[data-type="task"]:not(.hidden)' },
            { has: this.hasRolesSectionTarget, target: this.rolesSectionTarget, selector: '.trash-item[data-type="role"]:not(.hidden)' },
            { has: this.hasMembersSectionTarget, target: this.membersSectionTarget, selector: '.trash-item[data-type="member"]:not(.hidden)' },
            { has: this.hasLinksSectionTarget, target: this.linksSectionTarget, selector: '.trash-item[data-type="link"]:not(.hidden)' }
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
    }

    toggleSection(event) {
        const btn = event.currentTarget;
        const section = btn.closest('.trash-section');
        const content = section.querySelector('[data-organ-trash-target="content"]');
        const chevron = section.querySelector('[data-organ-trash-target="chevron"]');

        content.classList.toggle('hidden');
        chevron.classList.toggle('rotate-180');
    }

    async restore(event) {
        const btn = event.currentTarget;
        const type = btn.dataset.type;
        const uuid = btn.dataset.uuid;
        const row = btn.closest('.trash-item');

        let url = '';
        if (type === 'task') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${uuid}/restore`;
        } else if (type === 'role') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/roles/${uuid}/restore`;
        } else if (type === 'member') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/roles/members/${uuid}/restore`;
        } else if (type === 'link') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/links/${uuid}/restore`;
        }

        try {
            btn.disabled = true;
            const response = await fetch(url, {
                method: 'POST',
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
                alert('Erreur lors de la restauration.');
            }
        } catch (error) {
            btn.disabled = false;
            console.error('Restore failed', error);
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
        if (type === 'task') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${uuid}?permanent=1`;
        } else if (type === 'role') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/roles/${uuid}?permanent=1`;
        } else if (type === 'member') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/roles/members/${uuid}?permanent=1`;
        } else if (type === 'link') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/links/${uuid}?permanent=1`;
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
            window.location.reload(); // To show the global empty state from Twig
        }
    }
}
