import { Controller } from '@hotwired/stimulus';

export default class extends Controller {
    static targets = ['input', 'icon'];

    toggle() {
        const type = this.inputTarget.getAttribute('type') === 'password' ? 'text' : 'password';
        this.inputTarget.setAttribute('type', type);
        
        // Optional: toggle icon class if needed
        if (this.hasIconTarget) {
            this.iconTarget.classList.toggle('hidden');
        }
    }
}
