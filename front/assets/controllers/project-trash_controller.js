import { Controller } from '@hotwired/stimulus';

export default class extends Controller {
    static targets = [
        'hardDeleteModal', 'deleteSubmitBtn', 'deleteSpinner',
        'restoreModal', 'restoreSubmitBtn', 'restoreSpinner'
    ];
    static values = {
        apiUrl: String
    };

    connect() {
        this.projectToManageUuid = null;
        this.projectToManageRow = null;

        // Handle ESC key to close modals
        this.escHandler = (e) => {
            if (e.key === 'Escape') {
                if (this.hasHardDeleteModalTarget && !this.hardDeleteModalTarget.classList.contains('hidden')) {
                    this.closeHardDeleteModal();
                }
                if (this.hasRestoreModalTarget && !this.restoreModalTarget.classList.contains('hidden')) {
                    this.closeRestoreModal();
                }
            }
        };
        window.addEventListener('keydown', this.escHandler);
    }

    disconnect() {
        window.removeEventListener('keydown', this.escHandler);
    }

    restore(event) {
        this.projectToManageUuid = event.currentTarget.dataset.uuid;
        this.projectToManageRow = event.currentTarget.closest('.trash-item');
        this.restoreModalTarget.classList.remove('hidden');
    }

    closeRestoreModal() {
        this.restoreModalTarget.classList.add('hidden');
        this.projectToManageUuid = null;
        this.projectToManageRow = null;
    }

    async confirmRestore() {
        if (!this.projectToManageUuid) return;

        const uuid = this.projectToManageUuid;
        const row = this.projectToManageRow;

        this.restoreSubmitBtnTarget.disabled = true;
        this.restoreSpinnerTarget.classList.remove('hidden');

        try {
            const response = await fetch(`${this.apiUrlValue}/projects/${uuid}/restore`, {
                method: 'POST',
                credentials: 'include'
            });

            if (!response.ok) throw new Error("Erreur lors de la restauration.");
            
            this.closeRestoreModal();
            this.animateOut(row);
            
            // Redirect to project trash to restore members
            setTimeout(() => {
                window.location.href = `/projects/${uuid}/trash?restored=1`;
            }, 600);
        } catch (e) {
            alert(e.message);
            this.restoreSubmitBtnTarget.disabled = false;
            this.restoreSpinnerTarget.classList.add('hidden');
        }
    }

    removePermanent(event) {
        this.projectToManageUuid = event.currentTarget.dataset.uuid;
        this.projectToManageRow = event.currentTarget.closest('.trash-item');
        this.hardDeleteModalTarget.classList.remove('hidden');
    }

    closeHardDeleteModal() {
        this.hardDeleteModalTarget.classList.add('hidden');
        this.projectToManageUuid = null;
        this.projectToManageRow = null;
    }

    async confirmRemovePermanent() {
        if (!this.projectToManageUuid) return;

        const uuid = this.projectToManageUuid;
        const row = this.projectToManageRow;

        this.deleteSubmitBtnTarget.disabled = true;
        this.deleteSpinnerTarget.classList.remove('hidden');

        try {
            const response = await fetch(`${this.apiUrlValue}/projects/${uuid}?permanent=1`, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (!response.ok) throw new Error("Erreur lors de la suppression définitive.");
            
            this.closeHardDeleteModal();
            this.animateOut(row);
        } catch (e) {
            alert(e.message);
            this.deleteSubmitBtnTarget.disabled = false;
            this.deleteSpinnerTarget.classList.add('hidden');
        }
    }

    animateOut(row) {
        row.classList.add('opacity-0', 'translate-x-4', 'transition-all', 'duration-500');
        setTimeout(() => {
            row.remove();
            if (document.querySelectorAll('.trash-item').length === 0) {
                window.location.reload();
            }
        }, 500);
    }
}
