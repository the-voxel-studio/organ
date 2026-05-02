import { Controller } from '@hotwired/stimulus';
import { trans } from '../translator.js';
import Sortable from 'sortablejs';

export default class extends Controller {
    static targets = [
        'kanbanView', 'listView', 'linksPanel', 'kanbanBtn', 'listBtn', 
        'topScrollContainer', 'topScrollThumb',
        'todoCol', 'inProgressCol', 'waitingCol', 'doneCol', 'canceledCol',
        'todoCount', 'inProgressCount', 'waitingCount', 'doneCount', 'canceledCount',
        'listBody', 'filterMenu', 'sortIndicator', 'priorityFilter'
    ];
    static values = {
        projectUuid: String,
        organUuid: String,
        apiUrl: { type: String, default: '/api' },
        currentUserUuid: String
    };

    connect() {
        // Default state
        this.allTasks = [];
        this.sortBy = null; // 'priority', 'date'
        this.sortOrder = 'asc';
        this.filterMe = false;
        this.minPriority = 0;

        // Default view is Kanban
        this.showKanban();
        this.setupTopScroll();
        this.loadTasks();
        this.setupDragAndDrop();

        this.onTaskSaved = (event) => {
            if (event.detail.organUuid === this.organUuidValue) {
                this.loadTasks();
            }
        };
        window.addEventListener('task-saved', this.onTaskSaved);

        // Check for task in URL hash
        this.checkUrlHash();

        // Close menu on click outside
        this.closeMenuHandler = (e) => {
            if (this.hasFilterMenuTarget && !this.filterMenuTarget.classList.contains('hidden')) {
                if (!this.filterMenuTarget.contains(e.target) && !e.target.closest('[data-action="click->organ-view#toggleFilterMenu"]')) {
                    this.filterMenuTarget.classList.add('hidden');
                }
            }
        };
        window.addEventListener('click', this.closeMenuHandler);
    }

    disconnect() {
        if (this.resizeObserver) {
            this.resizeObserver.disconnect();
        }
        window.removeEventListener('task-saved', this.onTaskSaved);
        window.removeEventListener('click', this.closeMenuHandler);
        if (this.sortables) {
            this.sortables.forEach(s => s.destroy());
        }
    }

    checkUrlHash() {
        const hash = window.location.hash;
        if (hash.startsWith('#task-')) {
            const taskUuid = hash.replace('#task-', '');
            setTimeout(() => {
                const modalEl = document.querySelector('[data-controller~="task-modal"]');
                if (modalEl) {
                    const controller = this.application.getControllerForElementAndIdentifier(modalEl, 'task-modal');
                    if (controller) {
                        controller.openForEdit({ 
                            currentTarget: { 
                                dataset: { taskUuid: taskUuid } 
                            },
                            params: { uuid: taskUuid }
                        });
                    }
                }
            }, 500);
        }
    }

    toggleFilterMenu() {
        this.filterMenuTarget.classList.toggle('hidden');
    }

    setSort(event) {
        const sort = event.currentTarget.dataset.sort;
        if (this.sortBy === sort) {
            this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
        } else {
            this.sortBy = sort;
            this.sortOrder = 'asc';
        }
        this.updateSortUI();
        this.applyAll();
    }

    updateSortUI() {
        this.sortIndicatorTargets.forEach(el => {
            el.classList.toggle('hidden', el.dataset.sort !== this.sortBy);
            if (el.dataset.sort === this.sortBy) {
                // Default chevron is down. Descending = Down (no rotate), Ascending = Up (rotate 180)
                el.style.transform = this.sortOrder === 'asc' ? 'rotate(180deg)' : '';
            }
        });
    }

    applyFilters(event) {
        const filter = event.currentTarget.dataset.filter;
        if (filter === 'me') {
            this.filterMe = event.currentTarget.checked;
        }
        this.applyAll();
    }

    setPriorityFilter(event) {
        const p = parseInt(event.currentTarget.dataset.priority);
        this.minPriority = this.minPriority === p ? 0 : p;
        this.updatePriorityUI();
        this.applyAll();
    }

    updatePriorityUI() {
        this.priorityFilterTargets.forEach(el => {
            const p = parseInt(el.dataset.priority);
            const active = p === this.minPriority;
            const highlight = this.element.style.getPropertyValue('--highlight-color');
            
            el.style.backgroundColor = active ? highlight : '';
            el.style.color = active ? 'white' : '';
            el.style.borderColor = active ? highlight : '';
        });
    }

    resetFilters() {
        this.sortBy = null;
        this.sortOrder = 'asc';
        this.filterMe = false;
        this.minPriority = 0;

        // Reset UI
        const meCheckbox = this.element.querySelector('[data-filter="me"]');
        if (meCheckbox) meCheckbox.checked = false;
        
        this.updateSortUI();
        this.updatePriorityUI();
        this.applyAll();
    }

    applyAll() {
        let tasks = [...this.allTasks];

        // 1. Filter
        if (this.filterMe) {
            tasks = tasks.filter(t => 
                t.manager?.uuid === this.currentUserUuidValue || 
                t.assignees?.some(a => a.uuid === this.currentUserUuidValue) ||
                t.createdBy?.uuid === this.currentUserUuidValue
            );
        }

        if (this.minPriority > 0) {
            tasks = tasks.filter(t => t.priority >= this.minPriority);
        }

        // 2. Sort
        if (this.sortBy) {
            tasks.sort((a, b) => {
                let valA, valB;
                if (this.sortBy === 'priority') {
                    valA = a.priority;
                    valB = b.priority;
                } else if (this.sortBy === 'date') {
                    valA = new Date(a.createdAt).getTime();
                    valB = new Date(b.createdAt).getTime();
                }

                if (valA < valB) return this.sortOrder === 'asc' ? -1 : 1;
                if (valA > valB) return this.sortOrder === 'asc' ? 1 : -1;
                return 0;
            });
        }

        this.renderTasks(tasks);
    }

    setupDragAndDrop() {
        const cols = [
            { target: this.todoColTarget, status: 'TODO' },
            { target: this.inProgressColTarget, status: 'IN_PROGRESS' },
            { target: this.waitingColTarget, status: 'WAITING' },
            { target: this.doneColTarget, status: 'DONE' },
            { target: this.canceledColTarget, status: 'CANCELED' }
        ];

        this.sortables = cols.map(col => {
            return new Sortable(col.target, {
                group: 'tasks',
                animation: 150,
                ghostClass: 'opacity-20',
                dragClass: 'rotate-2',
                handle: '.cursor-pointer',
                onEnd: async (evt) => {
                    const taskId = evt.item.dataset.taskUuid;
                    const newStatus = evt.to.dataset.status;
                    const oldStatus = evt.from.dataset.status;

                    if (newStatus === oldStatus) return;

                    try {
                        const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${taskId}`, {
                            method: 'PATCH',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ status: newStatus }),
                            credentials: 'include'
                        });

                        if (res.ok) {
                            window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
                        } else {
                            // Revert on failure
                            this.loadTasks();
                            const error = await res.json();
                            const modal = this.application.getControllerForElementAndIdentifier(this.element, 'task-modal');
                            if (modal) {
                                let message = error.message || trans('organ.view.error.move_task');
                                if (res.status === 403) message = trans('task.modal.error.access_denied');
                                modal.showError(trans('task.modal.error.save_title'), message);
                            } else {
                                alert(error.message || trans('organ.view.error.move_task'));
                            }
                        }
                    } catch (e) {
                        console.error(e);
                        this.loadTasks();
                    }
                }
            });
        });
    }

    setupTopScroll() {
        if (!this.hasTopScrollContainerTarget || !this.hasKanbanViewTarget) return;

        const topScroll = this.topScrollContainerTarget;
        const kanban = this.kanbanViewTarget;
        const thumb = this.topScrollThumbTarget;

        const syncThumb = () => {
            thumb.style.width = `${kanban.scrollWidth}px`;
        };

        syncThumb();
        this.resizeObserver = new ResizeObserver(syncThumb);
        this.resizeObserver.observe(kanban);

        topScroll.onscroll = () => {
            if (this.isSyncing) {
                this.isSyncing = false;
                return;
            }
            this.isSyncing = true;
            kanban.scrollLeft = topScroll.scrollLeft;
        };

        kanban.onscroll = () => {
            if (this.isSyncing) {
                this.isSyncing = false;
                return;
            }
            this.isSyncing = true;
            topScroll.scrollLeft = kanban.scrollLeft;
        };
    }

    async loadTasks() {
        try {
            const response = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks`, {
                credentials: 'include'
            });
            if (response.ok) {
                this.allTasks = await response.json();
                this.applyAll();
            }
        } catch (e) {
            console.error("Failed to load tasks", e);
        }
    }

    renderTasks(tasks) {
        // Clear columns
        const cols = {
            'TODO': this.todoColTarget,
            'IN_PROGRESS': this.inProgressColTarget,
            'WAITING': this.waitingColTarget,
            'DONE': this.doneColTarget,
            'CANCELED': this.canceledColTarget
        };
        const counts = {
            'TODO': this.todoCountTarget,
            'IN_PROGRESS': this.inProgressCountTarget,
            'WAITING': this.waitingCountTarget,
            'DONE': this.doneCountTarget,
            'CANCELED': this.canceledCountTarget
        };

        Object.values(cols).forEach(col => col.innerHTML = '');
        Object.values(counts).forEach(count => count.innerText = '0');
        this.listBodyTarget.innerHTML = '';

        const stats = { 'TODO': 0, 'IN_PROGRESS': 0, 'WAITING': 0, 'DONE': 0, 'CANCELED': 0 };

        tasks.forEach(task => {
            stats[task.status]++;
            const card = this.createTaskCard(task);
            if (cols[task.status]) {
                cols[task.status].appendChild(card);
            }
            this.listBodyTarget.appendChild(this.createTaskRow(task));
        });

        Object.keys(stats).forEach(status => {
            if (counts[status]) counts[status].innerText = stats[status];
        });

        // If a column is empty, show "No tasks"
        Object.keys(cols).forEach(status => {
            if (stats[status] === 0) {
                cols[status].innerHTML = `
                    <div class="py-12 text-center">
                        <p class="text-xs text-gray-400 font-medium italic">Aucune tâche</p>
                    </div>
                `;
            }
        });
        
        if (tasks.length === 0) {
            this.listBodyTarget.innerHTML = `
                <tr>
                    <td colspan="4" class="px-8 py-20 text-center text-sm text-gray-400 italic font-medium">
                        Aucune tâche à afficher.
                    </td>
                </tr>
            `;
        }
    }

    createTaskCard(task) {
        const div = document.createElement('div');
        div.className = 'bg-white p-5 rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-all cursor-pointer group';
        div.dataset.action = 'click->task-modal#openForEdit';
        div.dataset.taskUuid = task.uuid;
        
        // Dynamic hover border using inline style to ensure it uses the CSS variable
        div.onmouseenter = () => div.style.borderColor = 'var(--highlight-color)';
        div.onmouseleave = () => div.style.borderColor = '#f3f4f6';
        
        const priorityColors = { 
            1: 'bg-slate-100 text-slate-600', 
            2: 'bg-blue-50 text-blue-600', 
            3: 'bg-indigo-50 text-indigo-600',
            4: 'bg-cyan-50 text-cyan-600',
            5: 'bg-teal-50 text-teal-600',
            6: 'bg-emerald-50 text-emerald-600',
            7: 'bg-amber-50 text-amber-600', 
            8: 'bg-orange-50 text-orange-600', 
            9: 'bg-red-50 text-red-600',
            10: 'bg-rose-50 text-rose-600' 
        };

        div.innerHTML = `
            <div class="flex flex-col gap-4">
                <div class="flex items-start justify-between gap-4">
                    <h4 class="text-sm font-bold text-gray-900 leading-tight group-hover:text-black">${task.title}</h4>
                    <span class="px-2 py-0.5 rounded-lg text-[10px] font-black uppercase tracking-wider ${priorityColors[task.priority] || priorityColors[1]} shrink-0">
                        P${task.priority}
                    </span>
                </div>
                ${task.tags && task.tags.length > 0 ? `
                    <div class="flex flex-wrap gap-1.5">
                        ${task.tags.map(t => `
                            <span class="px-2 py-0.5 bg-gray-50 text-[9px] font-black uppercase tracking-widest rounded border border-gray-100" style="color: ${t.color}; border-color: ${t.color}20">${t.name}</span>
                        `).join('')}
                    </div>
                ` : ''}
                ${task.description ? `<p class="text-xs text-gray-400 line-clamp-2">${task.description}</p>` : ''}
                <div class="flex items-center justify-between pt-3 border-t border-gray-50">
                    <div class="flex -space-x-2">
                        ${task.assignees.map(a => `
                            <div class="w-7 h-7 rounded-full border-2 border-white bg-gray-100 flex items-center justify-center text-[10px] font-bold text-gray-600 uppercase" title="${a.firstName} ${a.lastName}">
                                ${a.firstName[0]}${a.lastName[0]}
                            </div>
                        `).join('')}
                        ${task.assignees.length === 0 ? '<div class="w-7 h-7 rounded-full border-2 border-white bg-gray-50 flex items-center justify-center text-[10px] text-gray-300"><svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg></div>' : ''}
                    </div>
                    <div class="flex items-center gap-3">
                        ${task.expiresAt ? `
                            <div class="flex items-center gap-1.5 px-2 py-1 bg-rose-50 text-rose-600 rounded-lg text-[10px] font-black uppercase tracking-widest">
                                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 00-2 2z" /></svg>
                                ${new Date(task.expiresAt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })}
                            </div>
                        ` : ''}
                        ${task.commentCount > 0 ? `<div class="flex items-center gap-1 text-[9px] font-bold text-gray-400"><svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"></path></svg>${task.commentCount}</div>` : ''}
                    </div>
                </div>
            </div>
        `;
        return div;
    }

    createTaskRow(task) {
        const tr = document.createElement('tr');
        tr.className = 'hover:bg-gray-50/50 transition-colors cursor-pointer group';
        tr.dataset.action = 'click->task-modal#openForEdit';
        tr.dataset.taskUuid = task.uuid;

        const highlightColor = this.element.style.getPropertyValue('--highlight-color');
        const statusLabels = { 'TODO': 'À faire', 'IN_PROGRESS': 'En cours', 'WAITING': 'En attente', 'DONE': 'Terminé', 'CANCELED': 'Annulé' };
        
        const getStatusBadge = (status) => {
            if (status === 'IN_PROGRESS') {
                return `<span class="px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-wider" style="background-color: ${highlightColor}1a; color: ${highlightColor}">${statusLabels[status]}</span>`;
            }
            const statusColors = { 'TODO': 'bg-slate-100 text-slate-600', 'WAITING': 'bg-amber-100 text-amber-600', 'DONE': 'bg-green-100 text-green-600', 'CANCELED': 'bg-rose-100 text-rose-600' };
            return `<span class="px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-wider ${statusColors[status]}">${statusLabels[status]}</span>`;
        };

        tr.innerHTML = `
            <td class="px-8 py-5">
                <div class="flex flex-col">
                    <span class="text-sm font-bold text-gray-900 group-hover:text-black">${task.title}</span>
                    <span class="text-[10px] text-gray-400 font-medium">${task.uuid.slice(0, 8)}</span>
                </div>
            </td>
            <td class="px-8 py-5">
                ${getStatusBadge(task.status)}
            </td>
            <td class="px-8 py-5">
                <span class="text-xs font-bold text-gray-600">Priorité ${task.priority}</span>
            </td>
            <td class="px-8 py-5">
                ${task.expiresAt ? `
                    <div class="flex items-center gap-1.5 text-[10px] font-black uppercase tracking-widest text-rose-500">
                        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 00-2 2z" /></svg>
                        ${new Date(task.expiresAt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })}
                    </div>
                ` : '<span class="text-[10px] font-black uppercase tracking-widest text-gray-300">-</span>'}
            </td>
            <td class="px-8 py-5 text-right">
                <button class="p-2 text-gray-400 hover:text-gray-900 transition-colors">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" /></svg>
                </button>
            </td>
        `;
        return tr;
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
}
