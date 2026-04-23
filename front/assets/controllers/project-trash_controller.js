import { Controller } from '@hotwired/stimulus';

export default class extends Controller {
    static values = {
        apiUrl: String
    };

    async restore(event) {
        const uuid = event.currentTarget.dataset.uuid;
        const btn = event.currentTarget;
        const row = btn.closest('.trash-item');

        if (!confirm("Voulez-vous restaurer ce projet ?")) {
            return;
        }

        this.setLoading(btn, true);

        try {
            const response = await fetch(`${this.apiUrlValue}/projects/${uuid}/restore`, {
                method: 'POST',
                credentials: 'include'
            });

            if (!response.ok) throw new Error("Erreur lors de la restauration.");
            this.animateOut(row);
        } catch (e) {
            alert(e.message);
            this.setLoading(btn, false, 'Restaurer');
        }
    }

    async removePermanent(event) {
        const uuid = event.currentTarget.dataset.uuid;
        const btn = event.currentTarget;
        const row = btn.closest('.trash-item');

        if (!confirm("Cette action est irréversible. Le projet et toutes ses données (organs, tâches, fichiers) seront supprimés définitivement. Continuer ?")) {
            return;
        }

        this.setLoading(btn, true);

        try {
            const response = await fetch(`${this.apiUrlValue}/projects/${uuid}?permanent=1`, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (!response.ok) throw new Error("Erreur lors de la suppression définitive.");
            this.animateOut(row);
        } catch (e) {
            alert(e.message);
            this.setLoading(btn, false);
        }
    }

    setLoading(btn, loading, text = '') {
        btn.disabled = loading;
        if (loading) {
            btn.innerHTML = '<svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>';
        } else {
            btn.innerText = text;
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
