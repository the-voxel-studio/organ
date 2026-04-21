import { Controller } from '@hotwired/stimulus';

export default class extends Controller {
    static targets = ['buttonContainer', 'spinner', 'button', 'icon', 'agreement'];
    static values = {
        clientId: String,
        loginUrl: String,
        redirectUrl: String,
        mode: { type: String, default: 'login' }
    };

    connect() {
        if (typeof google === 'undefined') {
            setTimeout(() => this.connect(), 100);
            return;
        }
        this.initializeGoogle();
    }

    initializeGoogle() {
        google.accounts.id.initialize({
            client_id: this.clientIdValue,
            callback: this.handleCredentialResponse.bind(this),
            ux_mode: 'popup',
            auto_select: false,
        });

        if (this.hasButtonContainerTarget) {
            google.accounts.id.renderButton(
                this.buttonContainerTarget,
                { theme: 'outline', size: 'large' }
            );
        }
    }

    login(event) {
        if (event) event.preventDefault();
        
        // Check agreement
        if (this.hasAgreementTarget && !this.agreementTarget.checked) {
            this.dispatch('error', { detail: 'Vous devez accepter les conditions pour continuer' });
            return;
        }

        this.startLoading();

        // Fallback: stop loading after 30 seconds if nothing happened (popup closed)
        this.loadingTimeout = setTimeout(() => this.stopLoading(), 30000);

        const googleButton = this.buttonContainerTarget.querySelector('div[role="button"]') 
                          || this.buttonContainerTarget.querySelector('iframe');
        
        if (googleButton) {
            googleButton.click();
        } else {
            google.accounts.id.prompt((notification) => {
                if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
                    this.stopLoading();
                }
            });
        }
    }

    async handleCredentialResponse(response) {
        if (this.loadingTimeout) clearTimeout(this.loadingTimeout);
        const idToken = response.credential;

        try {
            const res = await fetch(this.loginUrlValue, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({ idToken: idToken }),
                credentials: 'include'
            });

            if (res.ok) {
                if (this.modeValue === 'link') {
                    window.location.href = this.redirectUrlValue;
                } else {
                    window.location.href = this.redirectUrlValue;
                }
            } else {
                this.stopLoading();
                const errorData = await res.json().catch(() => ({}));
                this.dispatch('error', { detail: errorData.message || errorData.error || 'Authentication failed' });
            }
        } catch (error) {
            this.stopLoading();
            this.dispatch('error', { detail: 'Server error during Google authentication' });
        }
    }

    startLoading() {
        if (this.hasButtonTarget) this.buttonTarget.disabled = true;
        if (this.hasSpinnerTarget) this.spinnerTarget.classList.remove('hidden');
        if (this.hasIconTarget) this.iconTarget.classList.add('hidden');
        if (this.hasButtonTarget) this.buttonTarget.classList.add('opacity-70', 'cursor-wait');
    }

    stopLoading() {
        if (this.hasButtonTarget) this.buttonTarget.disabled = false;
        if (this.hasSpinnerTarget) this.spinnerTarget.classList.add('hidden');
        if (this.hasIconTarget) this.iconTarget.classList.remove('hidden');
        if (this.hasButtonTarget) this.buttonTarget.classList.remove('opacity-70', 'cursor-wait');
    }
}
