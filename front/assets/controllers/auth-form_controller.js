import { Controller } from '@hotwired/stimulus';
import { trans } from '../translator.js';

export default class extends Controller {
    static targets = ['error', 'errorMessage', 'submitButton', 'spinner', 'form', 'agreement'];
    static values = {
        url: String,
        redirectUrl: String,
        type: String // 'login' or 'register'
    };

    async submit(event) {
        event.preventDefault();
        
        // Check agreement
        if (this.hasAgreementTarget && !this.agreementTarget.checked) {
            this.showError(trans('errors.must_agree', {}, 'auth'));
            return;
        }

        const formData = new FormData(this.formTarget);
        let data = Object.fromEntries(formData.entries());

        // Map keys if register
        if (this.typeValue === 'register') {
            data = {
                email: data.email,
                password: data.password,
                firstName: data.firstname,
                lastName: data.lastname
            };
        } else if (this.typeValue === 'login') {
            data = {
                username: data._username,
                password: data._password
            };
        }

        try {
            const response = await fetch(this.urlValue, {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(data),
                credentials: 'include'
            });

            if (response.ok) {
                // For login, the API might set cookies or return a token.
                // If it sets HttpOnly cookies, we just redirect.
                window.location.href = this.redirectUrlValue;
                return;
            }

            // LexikJWT / API responses
            const result = await response.json().catch(() => ({}));

            let errorMsg = trans('errors.generic_error', {}, 'auth');
            
            if (result.message === 'This account uses Google Login. Please use the "Sign in with Google" button.') {
                errorMsg = trans('errors.google_only_account', {}, 'auth');
            } else if (response.status === 401) {
                errorMsg = trans('errors.invalid_credentials', {}, 'auth');
            } else if (result.error === 'User already exists') {
                errorMsg = trans('errors.user_exists', {}, 'auth');
            } else if (result.message) {
                errorMsg = result.message;
            }

            this.showError(errorMsg);
        } catch (e) {
            this.showError(trans('errors.server_error', {}, 'auth'));
        }
    }

    showError(message) {
        this.errorMessageTarget.innerText = message;
        this.errorTarget.classList.remove('hidden');
        this.submitButtonTarget.disabled = false;
        this.spinnerTarget.classList.add('hidden');
        this.formTarget.classList.remove('opacity-50', 'pointer-events-none');
        
        // Small shake animation if you want
        this.errorTarget.classList.add('animate-shake');
        setTimeout(() => this.errorTarget.classList.remove('animate-shake'), 500);
    }

    showErrorFromEvent(event) {
        this.showError(event.detail);
    }
}
