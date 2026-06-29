import { Controller } from '@hotwired/stimulus';
import { trans } from '../translator.js';

export default class extends Controller {
    static targets = ['error', 'errorMessage', 'submitButton', 'spinner', 'form', 'agreement', 'success', 'successMessage'];
    static values = {
        url: String,
        redirectUrl: String,
        type: String // 'login' or 'register'
    };

    connect() {
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.has('registered') && this.typeValue === 'login') {
            this.showSuccess(trans('register.success', {}, 'auth'));
        }
    }

    async submit(event) {
        event.preventDefault();
        
        this.errorTarget.classList.add('hidden');
        if (this.hasSuccessTarget) this.successTarget.classList.add('hidden');
        this.submitButtonTarget.disabled = true;
        this.spinnerTarget.classList.remove('hidden');
        this.formTarget.classList.add('opacity-50', 'pointer-events-none');

        // Check agreement
        if (this.hasAgreementTarget && !this.agreementTarget.checked) {
            this.showError(trans('errors.must_agree', {}, 'auth'));
            return;
        }

        const formData = new FormData(this.formTarget);
        let data = Object.fromEntries(formData.entries());

        // Map keys and validations if register
        if (this.typeValue === 'register') {
            // Client-side check for password matching
            if (data.password !== data.confirm_password) {
                this.showError(trans('errors.password_mismatch', {}, 'auth'));
                return;
            }

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
                let redirectUrl = this.redirectUrlValue;
                if (this.typeValue === 'register') {
                    // Append success flag
                    redirectUrl += (redirectUrl.includes('?') ? '&' : '?') + 'registered=1';
                }
                window.location.href = redirectUrl;
                return;
            }

            // LexikJWT / API responses
            const result = await response.json().catch(() => ({}));

            const errorMap = {
                'Password must be at least 8 characters': 'errors.password_too_short',
                'User already exists': 'errors.user_exists',
                'Email is not valid': 'errors.email_invalid',
                'This value is not a valid email address.': 'errors.email_invalid',
                'This value should not be blank.': 'errors.field_blank',
                'Invalid credentials.': 'errors.invalid_credentials',
                'This account uses Google Login. Please use the "Sign in with Google" button.': 'errors.google_only_account',
                'Token is required': 'errors.token_required',
                'Invalid Google token': 'errors.invalid_google_token',
                'Your account is scheduled for deletion. Please log in again to reactivate it.': 'errors.account_deleted',
                'There is already an account with this email': 'errors.email_taken'
            };

            let errorMsgKey = errorMap[result.error] || errorMap[result.message] || 'errors.generic_error';

            // Handle Symfony Validation Errors (ConstraintViolationList)
            if (Array.isArray(result)) {
                // Sometimes it's just a raw array of violations
                const firstError = result[0]?.message || result[0]?.title;
                if (firstError) errorMsgKey = errorMap[firstError] || firstError;
            } else if (result.violations && Array.isArray(result.violations)) {
                // Sometimes it's an object with a violations property
                const firstError = result.violations[0]?.title || result.violations[0]?.message;
                if (firstError) errorMsgKey = errorMap[firstError] || firstError;
            }

            let errorMsg = trans(errorMsgKey, {}, 'auth');
            
            // If the message is still the key (translation not found) and we have a raw message
            if (errorMsg === errorMsgKey && (result.error || result.message)) {
                errorMsg = result.error || result.message;
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
        
        this.errorTarget.classList.add('animate-shake');
        setTimeout(() => this.errorTarget.classList.remove('animate-shake'), 500);
    }

    showSuccess(message) {
        if (!this.hasSuccessTarget) return;
        this.successMessageTarget.innerText = message;
        this.successTarget.classList.remove('hidden');
    }

    showErrorFromEvent(event) {
        this.showError(event.detail);
    }
}
