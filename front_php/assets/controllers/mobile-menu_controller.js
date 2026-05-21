import { Controller } from '@hotwired/stimulus';

export default class extends Controller {
    static targets = ['menu'];

    toggle() {
        if (this.menuTarget.classList.contains('hidden')) {
            this.open();
        } else {
            this.close();
        }
    }

    open() {
        this.menuTarget.classList.remove('hidden');
        document.body.classList.add('overflow-hidden');
        
        // Petit délai pour déclencher la transition CSS
        setTimeout(() => {
            this.menuTarget.classList.replace('opacity-0', 'opacity-100');
            this.menuTarget.classList.replace('-translate-y-full', 'translate-y-0');
        }, 10);
    }

    close() {
        this.menuTarget.classList.replace('opacity-100', 'opacity-0');
        this.menuTarget.classList.replace('translate-y-0', '-translate-y-full');
        
        // Attendre la fin de l'animation pour cacher l'élément
        setTimeout(() => {
            this.menuTarget.classList.add('hidden');
            document.body.classList.remove('overflow-hidden');
        }, 300);
    }
}
