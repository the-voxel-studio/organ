import { Controller } from '@hotwired/stimulus';
import { trans } from '../translator.js';

export default class extends Controller {
    static targets = [
        'item', 'searchInput', 'organsSection', 'membersSection', 'tagsSection', 'content', 'chevron',
        'hardDeleteModal', 'deleteSubmitBtn', 'deleteSpinner', 'deleteModalTitle',
        'restoreModal', 'restoreSubmitBtn', 'restoreSpinner', 'restoreModalTitle',
        'memberCheckbox', 'bulkRestoreBtn', 'bulkRestoreModal', 'bulkCount', 'bulkRestoreSubmitBtn', 'bulkRestoreSpinner'
    ];
    static values = {
        apiUrl: String,
        projectUuid: String
    };

    connect() {
        this.itemToManage = null;

        this.escHandler = (e) => {
            if (e.key === 'Escape') {
                if (this.hasHardDeleteModalTarget && !this.hardDeleteModalTarget.classList.contains('hidden')) {
                    this.closeHardDeleteModal();
                }
                if (this.hasRestoreModalTarget && !this.restoreModalTarget.classList.contains('hidden')) {
                    this.closeRestoreModal();
                }
                if (this.hasBulkRestoreModalTarget && !this.bulkRestoreModalTarget.classList.contains('hidden')) {
                    this.closeBulkRestoreModal();
                }
            }
        };
        window.addEventListener('keydown', this.escHandler);
        this.updateBulkBtnVisibility();
    }

    disconnect() {
        window.removeEventListener('keydown', this.escHandler);
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
        this.updateBulkBtnVisibility();
    }

    toggleSection(event) {
        const btn = event.currentTarget;
        const section = btn.closest('.trash-section');
        const content = section.querySelector('[data-project-specific-trash-target="content"]');
        const chevron = section.querySelector('[data-project-specific-trash-target="chevron"]');

        content.classList.toggle('hidden');
        chevron.classList.toggle('rotate-180');
    }

    openRestoreModal(event) {
        const btn = event.currentTarget;
        this.itemToManage = {
            type: btn.dataset.type,
            uuid: btn.dataset.uuid,
            row: btn.closest('.trash-item')
        };
        this.restoreModalTitleTarget.innerText = `"${btn.dataset.title}"`;
        this.restoreModalTarget.classList.remove('hidden');
    }

    closeRestoreModal() {
        this.restoreModalTarget.classList.add('hidden');
        this.itemToManage = null;
    }

    async confirmRestore() {
        if (!this.itemToManage) return;

        const { type, uuid, row } = this.itemToManage;
        let url = '';
        if (type === 'organ') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${uuid}/restore`;
        } else if (type === 'member') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/members/${uuid}/restore`;
        } else if (type === 'tag') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/tags/${uuid}/restore`;
        }

        try {
            this.restoreSubmitBtnTarget.disabled = true;
            this.restoreSpinnerTarget.classList.remove('hidden');

            const response = await fetch(url, {
                method: 'POST',
                credentials: 'include'
            });

            if (!response.ok) throw new Error(trans('organ.trash.error.restore'));

            this.closeRestoreModal();
            row.classList.add('opacity-0', 'scale-95', 'transition-all', 'duration-300');
            setTimeout(() => {
                row.remove();
                this.updateVisibility();
            }, 300);

        } catch (e) {
            alert(e.message);
            this.restoreSubmitBtnTarget.disabled = false;
            this.restoreSpinnerTarget.classList.add('hidden');
        }
    }

    openHardDeleteModal(event) {
        const btn = event.currentTarget;
        this.itemToManage = {
            type: btn.dataset.type,
            uuid: btn.dataset.uuid,
            row: btn.closest('.trash-item')
        };
        this.deleteModalTitleTarget.innerText = `"${btn.dataset.title}"`;
        this.hardDeleteModalTarget.classList.remove('hidden');
    }

    closeHardDeleteModal() {
        this.hardDeleteModalTarget.classList.add('hidden');
        this.itemToManage = null;
    }

    async confirmRemovePermanent() {
        if (!this.itemToManage) return;

        const { type, uuid, row } = this.itemToManage;
        let url = '';
        if (type === 'organ') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${uuid}?permanent=1`;
        } else if (type === 'tag') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/tags/${uuid}?permanent=1`;
        } else if (type === 'member') {
            url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/members/${uuid}?permanent=1`;
        }

        try {
            this.deleteSubmitBtnTarget.disabled = true;
            this.deleteSpinnerTarget.classList.remove('hidden');

            const response = await fetch(url, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (response.ok) {
                this.closeHardDeleteModal();
                row.classList.add('opacity-0', 'scale-95');
                setTimeout(() => {
                    row.remove();
                    this.updateVisibility();
                    this.checkGlobalEmptyState();
                }, 300);
            } else {
                throw new Error(trans('organ.trash.error.delete_permanent'));
            }
        } catch (error) {
            alert(error.message);
            this.deleteSubmitBtnTarget.disabled = false;
            this.deleteSpinnerTarget.classList.add('hidden');
        }
    }

    // --- BULK RESTORE ---
    selectAllMembers() {
        this.memberCheckboxTargets.forEach(cb => {
            if (!cb.closest('.trash-item').classList.contains('hidden')) {
                cb.checked = true;
            }
        });
        this.updateBulkBtnVisibility();
    }

    updateBulkBtnVisibility() {
        const selectedCount = this.memberCheckboxTargets.filter(cb => cb.checked).length;
        if (selectedCount > 0) {
            this.bulkRestoreBtnTarget.classList.remove('hidden');
            this.bulkRestoreBtnTarget.innerHTML = trans('project.show.bulk_restore_btn', { count: selectedCount });
        } else {
            this.bulkRestoreBtnTarget.classList.add('hidden');
        }
    }

    openBulkRestoreModal() {
        const selectedCount = this.memberCheckboxTargets.filter(cb => cb.checked).length;
        this.bulkCountTarget.innerText = selectedCount;
        this.bulkRestoreModalTarget.classList.remove('hidden');
    }

    closeBulkRestoreModal() {
        this.bulkRestoreModalTarget.classList.add('hidden');
    }

    async confirmBulkRestore() {
        const selectedCheckboxes = this.memberCheckboxTargets.filter(cb => cb.checked);
        this.bulkRestoreSubmitBtnTarget.disabled = true;
        this.bulkRestoreSpinnerTarget.classList.remove('hidden');

        let successCount = 0;
        let failCount = 0;

        for (const cb of selectedCheckboxes) {
            const row = cb.closest('.trash-item');
            const uuid = row.dataset.uuid;
            const url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/members/${uuid}/restore`;

            try {
                const res = await fetch(url, { method: 'POST', credentials: 'include' });
                if (res.ok) {
                    successCount++;
                    row.classList.add('opacity-0', 'scale-95', 'transition-all', 'duration-300');
                    setTimeout(() => row.remove(), 300);
                } else {
                    failCount++;
                }
            } catch (e) {
                failCount++;
            }
        }

        setTimeout(() => {
            this.closeBulkRestoreModal();
            this.updateVisibility();
            if (failCount > 0) {
                alert(trans('project.show.restore_batch_status', { success: successCount, fail: failCount }));
            }
        }, 400);
    }

    checkGlobalEmptyState() {
        const remainingItems = this.element.querySelectorAll('.trash-item').length;
        if (remainingItems === 0) {
            window.location.reload(); 
        }
    }
}
