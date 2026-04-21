import { Controller } from '@hotwired/stimulus';

export default class extends Controller {
    static targets = ['kanbanView', 'listView', 'linksPanel', 'kanbanBtn', 'listBtn', 'topScrollContainer', 'topScrollThumb'];

    connect() {
        // Default view is Kanban
        this.showKanban();
        this.setupTopScroll();
    }

    setupTopScroll() {
        if (!this.hasKanbanViewTarget || !this.hasTopScrollContainerTarget) return;

        this.isSyncing = false;

        // Synchronize top scroll -> kanban
        this.topScrollContainerTarget.addEventListener('scroll', () => {
            if (this.isSyncing) {
                this.isSyncing = false;
                return;
            }
            this.isSyncing = true;
            this.kanbanViewTarget.scrollLeft = this.topScrollContainerTarget.scrollLeft;
        });

        // Synchronize kanban -> top scroll
        this.kanbanViewTarget.addEventListener('scroll', () => {
            if (this.isSyncing) {
                this.isSyncing = false;
                return;
            }
            this.isSyncing = true;
            this.topScrollContainerTarget.scrollLeft = this.kanbanViewTarget.scrollLeft;
        });

        // Update thumb width based on content
        const updateWidth = () => {
            if (this.hasTopScrollThumbTarget && this.hasKanbanViewTarget) {
                const innerContent = this.kanbanViewTarget.firstElementChild;
                if (innerContent) {
                    const scrollWidth = innerContent.scrollWidth;
                    this.topScrollThumbTarget.style.width = `${scrollWidth}px`;
                    console.log('Kanban scrollWidth:', scrollWidth);
                }
            }
        };

        // Initial and on resize
        updateWidth();
        window.addEventListener('resize', updateWidth);
        
        // Observer for dynamic content changes within the Kanban view
        if (this.kanbanViewTarget.firstElementChild) {
            this.resizeObserver = new ResizeObserver(() => updateWidth());
            this.resizeObserver.observe(this.kanbanViewTarget.firstElementChild);
        }
    }

    disconnect() {
        if (this.resizeObserver) {
            this.resizeObserver.disconnect();
        }
    }

    toggleView(event) {
        const view = event.currentTarget.dataset.view;
        if (view === 'kanban') {
            this.showKanban();
        } else {
            this.showList();
        }
    }

    showKanban() {
        this.kanbanViewTarget.classList.remove('hidden');
        if (this.hasTopScrollContainerTarget) {
            this.topScrollContainerTarget.classList.remove('hidden');
        }
        this.listViewTarget.classList.add('hidden');
        
        const highlightColor = this.element.style.getPropertyValue('--highlight-color');
        
        this.kanbanBtnTarget.style.backgroundColor = highlightColor;
        this.kanbanBtnTarget.classList.add('text-white', 'shadow-sm');
        this.kanbanBtnTarget.classList.remove('text-gray-500', 'hover:text-gray-900');
        
        this.listBtnTarget.style.backgroundColor = '';
        this.listBtnTarget.classList.remove('text-white', 'shadow-sm');
        this.listBtnTarget.classList.add('text-gray-500', 'hover:text-gray-900');
    }

    showList() {
        this.kanbanViewTarget.classList.add('hidden');
        if (this.hasTopScrollContainerTarget) {
            this.topScrollContainerTarget.classList.add('hidden');
        }
        this.listViewTarget.classList.remove('hidden');
        
        const highlightColor = this.element.style.getPropertyValue('--highlight-color');
        
        this.listBtnTarget.style.backgroundColor = highlightColor;
        this.listBtnTarget.classList.add('text-white', 'shadow-sm');
        this.listBtnTarget.classList.remove('text-gray-500', 'hover:text-gray-900');
        
        this.kanbanBtnTarget.style.backgroundColor = '';
        this.kanbanBtnTarget.classList.remove('text-white', 'shadow-sm');
        this.kanbanBtnTarget.classList.add('text-gray-500', 'hover:text-gray-900');
    }

    toggleLinks() {
        this.linksPanelTarget.classList.toggle('hidden');
    }
}
