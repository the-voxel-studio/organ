import { Controller } from '@hotwired/stimulus';
import { trans } from '@/translator';

export default class extends Controller {
    static targets = ['output'];

    connect() {
        this.outputTarget.textContent = trans('app.hello', { '%name%': 'Utilisateur JS' });
    }
}
