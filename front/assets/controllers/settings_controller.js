import { Controller } from '@hotwired/stimulus';
import { trans } from '../translator.js';

export default class extends Controller {
    static targets = [
        'form', 'submitButton', 'spinner', 'error', 'errorMessage', 'success', 'successMessage', 
        'deleteModal', 'deleteSpinner', 'connectionsList', 'disconnectModal', 'disconnectSpinner',
        'passwordForm', 'passwordSubmitButton', 'passwordSpinner', 'passwordError', 'passwordErrorMessage', 'passwordSuccess', 'passwordSuccessMessage',
        'googleError', 'googleErrorMessage', 'googleSuccess', 'googleSuccessMessage'
    ];
    static values = {
        apiUrl: String,
        connectionsUrl: String
    };

    connect() {
        if (this.hasConnectionsListTarget) {
            this.loadConnections();
        }
        this.connectionToInvalidate = null;
    }

    async loadConnections() {
        try {
            const response = await fetch(this.connectionsUrlValue, {
                credentials: 'include'
            });
            if (response.ok) {
                const connections = await response.json();
                this.renderConnections(connections);
            }
        } catch (error) {
            console.error('Failed to load connections:', error);
        }
    }

    renderConnections(connections) {
        if (!this.hasConnectionsListTarget) return;

        if (connections.length === 0) {
            this.connectionsListTarget.innerHTML = '<p class="text-sm text-gray-500 italic p-4">Aucune session active trouvée.</p>';
            return;
        }

        this.connectionsListTarget.innerHTML = connections.map(conn => `
            <div class="flex items-center justify-between p-4 bg-gray-50 rounded-2xl border border-gray-100 mb-3 group transition-all hover:bg-white hover:shadow-sm">
                <div class="flex items-center gap-4">
                    <div class="p-2.5 bg-white rounded-xl text-gray-400 border border-gray-100 shadow-sm group-hover:text-highlight transition-colors">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M9 17.25v1.007a3 3 0 0 1-.879 2.122L7.5 21h9l-.621-.621A3 3 0 0 1 15 18.257V17.25m6-12V15a2.25 2.25 0 0 1-2.25 2.25H5.25A2.25 2.25 0 0 1 3 15V5.25m18 0A2.25 2.25 0 0 0 18.75 3H5.25A2.25 2.25 0 0 0 3 5.25m18 0V12a2.25 2.25 0 0 1-2.25 2.25H5.25A2.25 2.25 0 0 1 3 12V5.25" />
                        </svg>
                    </div>
                    <div>
                        <div class="flex items-center gap-2">
                            <p class="font-bold text-black text-sm">${conn.deviceName} • ${conn.browserName}</p>
                            ${conn.isCurrent ? '<span class="px-2 py-0.5 bg-green-100 text-green-700 text-[10px] font-black uppercase tracking-widest rounded-md">Actuelle</span>' : ''}
                        </div>
                        <p class="text-xs text-gray-500 font-medium">
                            ${conn.location} • <span class="text-gray-400">${conn.ipAddress}</span>
                        </p>
                        <p class="text-[10px] text-gray-400 mt-1 uppercase tracking-wider font-bold">
                            Dernière activité : ${this.formatRelativeDate(new Date(conn.lastUsedAt))}
                        </p>
                    </div>
                </div>
                
                <button type="button" 
                        data-action="click->settings#invalidateConnection" 
                        data-uuid="${conn.uuid}"
                        class="p-2 text-gray-300 hover:text-red-500 hover:bg-red-50 rounded-xl transition-all opacity-0 group-hover:opacity-100">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                </button>
            </div>
        `).join('');
    }

    invalidateConnection(event) {
        this.connectionToInvalidate = event.currentTarget.dataset.uuid;
        this.openDisconnectModal();
    }

    openDisconnectModal() {
        this.disconnectModalTarget.classList.remove('hidden');
        document.body.classList.add('overflow-hidden');
    }

    closeDisconnectModal() {
        this.disconnectModalTarget.classList.add('hidden');
        document.body.classList.remove('overflow-hidden');
        this.connectionToInvalidate = null;
    }

    async confirmInvalidateConnection() {
        if (!this.connectionToInvalidate) return;

        if (this.hasDisconnectSpinnerTarget) this.disconnectSpinnerTarget.classList.remove('hidden');

        try {
            const response = await fetch(`${this.connectionsUrlValue}/${this.connectionToInvalidate}`, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (response.ok) {
                this.loadConnections();
                this.closeDisconnectModal();
                window.location.reload();
            } else {
                alert('Échec de la déconnexion de l\'appareil');
            }
        } catch (error) {
            alert('Erreur de connexion');
        } finally {
            if (this.hasDisconnectSpinnerTarget) this.disconnectSpinnerTarget.classList.add('hidden');
        }
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

    async submit(event) {
        event.preventDefault();
        this.clearMessages();
        this.startLoading();

        const formData = new FormData(this.formTarget);
        const data = Object.fromEntries(formData.entries());

        try {
            const response = await fetch(this.apiUrlValue, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(data),
                credentials: 'include'
            });

            const result = await response.json();

            if (response.ok) {
                this.showSuccess(trans('success.profile_updated', {}, 'settings'));
                if (result.refresh) {
                    setTimeout(() => window.location.reload(), 1500);
                }
            } else {
                this.showError(this.mapErrorMessage(result.message || result[0]?.message));
            }
        } catch (error) {
            this.showError(trans('errors.server', {}, 'settings'));
        } finally {
            this.stopLoading();
        }
    }

    async submitPassword(event) {
        event.preventDefault();
        this.clearPasswordMessages();
        this.startPasswordLoading();

        const formData = new FormData(this.passwordFormTarget);
        const data = Object.fromEntries(formData.entries());

        try {
            const response = await fetch(`${this.apiUrlValue}/password`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(data),
                credentials: 'include'
            });

            const result = await response.json();

            if (response.ok) {
                this.showPasswordSuccess(trans('success.password_updated', {}, 'settings'));
                this.passwordFormTarget.reset();
            } else {
                this.showPasswordError(this.mapErrorMessage(result.message || result[0]?.message));
            }
        } catch (error) {
            this.showPasswordError(trans('errors.server', {}, 'settings'));
        } finally {
            this.stopPasswordLoading();
        }
    }

    mapErrorMessage(message) {
        if (!message) return trans('errors.generic', {}, 'settings');
        
        const map = {
            'Invalid current password': 'errors.invalid_current_password',
            'New password must be at least 8 characters': 'errors.password_too_short',
            'Current and new passwords are required': 'errors.generic',
            'Not authenticated': 'errors.generic',
            'This Google account is already linked to another profile': 'errors.link_google_already_linked',
            'The email address provided by Google is already used by another account.': 'errors.link_google_email_taken'
        };

        const key = map[message];
        return key ? trans(key, {}, 'settings') : message;
    }

    startPasswordLoading() {
        this.passwordSubmitButtonTarget.disabled = true;
        this.passwordSpinnerTarget.classList.remove('hidden');
        this.passwordSubmitButtonTarget.classList.add('opacity-70', 'cursor-wait');
    }

    stopPasswordLoading() {
        this.passwordSubmitButtonTarget.disabled = false;
        this.passwordSpinnerTarget.classList.add('hidden');
        this.passwordSubmitButtonTarget.classList.remove('opacity-70', 'cursor-wait');
    }

    startLoading() {
        this.submitButtonTarget.disabled = true;
        this.spinnerTarget.classList.remove('hidden');
        this.submitButtonTarget.classList.add('opacity-70', 'cursor-wait');
    }

    stopLoading() {
        this.submitButtonTarget.disabled = false;
        this.spinnerTarget.classList.add('hidden');
        this.submitButtonTarget.classList.remove('opacity-70', 'cursor-wait');
    }

    showError(message) {
        this.errorMessageTarget.innerText = message;
        this.errorTarget.classList.remove('hidden');
    }

    showSuccess(message) {
        this.successMessageTarget.innerText = message;
        this.successTarget.classList.remove('hidden');
    }

    clearMessages() {
        if (this.hasErrorTarget) this.errorTarget.classList.add('hidden');
        if (this.hasSuccessTarget) this.successTarget.classList.add('hidden');
    }

    showPasswordError(message) {
        this.passwordErrorMessageTarget.innerText = message;
        this.passwordErrorTarget.classList.remove('hidden');
    }

    showPasswordSuccess(message) {
        this.passwordSuccessMessageTarget.innerText = message;
        this.passwordSuccessTarget.classList.remove('hidden');
    }

    clearPasswordMessages() {
        if (this.hasPasswordErrorTarget) this.passwordErrorTarget.classList.add('hidden');
        if (this.hasPasswordSuccessTarget) this.passwordSuccessTarget.classList.add('hidden');
    }

    showGoogleError(message) {
        this.googleErrorMessageTarget.innerText = message;
        this.googleErrorTarget.classList.remove('hidden');
    }

    showGoogleSuccess(message) {
        this.googleSuccessMessageTarget.innerText = message;
        this.googleSuccessTarget.classList.remove('hidden');
    }

    clearGoogleMessages() {
        if (this.hasGoogleErrorTarget) this.googleErrorTarget.classList.add('hidden');
        if (this.hasGoogleSuccessTarget) this.googleSuccessTarget.classList.add('hidden');
    }

    showErrorFromEvent(event) {
        const mappedMessage = this.mapErrorMessage(event.detail);
        
        // If the error is related to Google linking, show it in the Google section
        if (event.detail.includes('Google') || event.detail.includes('google')) {
            this.clearGoogleMessages();
            this.showGoogleError(mappedMessage);
        } else {
            this.clearMessages();
            this.showError(mappedMessage);
        }
    }

    openDeleteModal() {
        this.deleteModalTarget.classList.remove('hidden');
        document.body.classList.add('overflow-hidden');
    }

    closeDeleteModal() {
        this.deleteModalTarget.classList.add('hidden');
        document.body.classList.remove('overflow-hidden');
    }

    async confirmDelete() {
        if (this.hasDeleteSpinnerTarget) this.deleteSpinnerTarget.classList.remove('hidden');
        
        try {
            const response = await fetch(this.apiUrlValue, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (response.ok) {
                window.location.href = '/logout';
            } else {
                const result = await response.json();
                this.showError(result.message || trans('errors.generic', {}, 'settings'));
                this.closeDeleteModal();
            }
        } catch (error) {
            this.showError(trans('errors.server', {}, 'settings'));
            this.closeDeleteModal();
        } finally {
            if (this.hasDeleteSpinnerTarget) this.deleteSpinnerTarget.classList.add('hidden');
        }
    }
}
