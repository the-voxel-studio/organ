import { Controller } from '@hotwired/stimulus';

export default class extends Controller {
    static targets = ['link'];

    connect() {
        this.observer = new IntersectionObserver(this.onIntersection.bind(this), {
            rootMargin: '-20% 0px -70% 0px', // Déclenche quand la section occupe le centre de l'écran
            threshold: 0
        });

        // On observe chaque section pointée par les liens
        this.linkTargets.forEach(link => {
            const id = link.getAttribute('href').substring(1);
            const element = document.getElementById(id);
            if (element) this.observer.observe(element);
        });
    }

    disconnect() {
        if (this.observer) this.observer.disconnect();
    }

    onIntersection(entries) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const id = entry.target.id;
                this.activateLink(id);
            }
        });
    }

    activateLink(id) {
        this.linkTargets.forEach(link => {
            const isTarget = link.getAttribute('href') === `#${id}`;
            const indicator = link.querySelector('.nav-indicator');
            
            if (isTarget) {
                link.classList.add('text-black');
                link.classList.remove('text-gray-600');
                if (indicator) indicator.classList.remove('scale-x-0');
            } else {
                link.classList.remove('text-black');
                link.classList.add('text-gray-600');
                if (indicator) indicator.classList.add('scale-x-0');
            }
        });
    }
}
