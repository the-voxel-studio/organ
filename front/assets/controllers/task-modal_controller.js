import { Controller } from '@hotwired/stimulus';
import { trans } from '@/translator';

export default class extends Controller {
    static targets = [
        'container', 'backdrop', 'panel', 'modalTitle', 'form', 'loader', 'formContent', 
        'fieldContainer', 'submitBtn', 'submitBtnText', 'managerSelect', 'subResources',
        'taskMeta', 'taskUuid', 'taskCreatedAt', 'deleteBtn',
        'assigneeList', 'userPicker', 'userList',
        'taskTagList', 'tagPicker', 'projectTagList',
        'linkList', 'linkForm', 'linkUrl', 'linkDesc',
        'attachmentList',
        'dependencyList', 'dependencyPicker', 'projectTaskList',
        'commentList', 'commentContent',
        'timelineList', 'statusMessageContainer', 'loadMoreTimelineBtn',
        'assigneeAddContainer', 'tagAddContainer', 'linkAddBtn', 'dependencyAddBtn', 'attachmentAddContainer', 'commentFormContainer',
        'uploadProgressContainer', 'uploadFileName', 'uploadPercentage', 'uploadProgressBar',
        'trashSection', 'trashContent', 'deletedAttachmentList', 'deletedLinkList', 'deletedCommentList',
        'confirmModal', 'confirmTitle', 'confirmMessage', 'restoreBtn',
        'errorModal', 'errorTitle', 'errorMessage'
    ];
    static values = {
        projectUuid: String,
        organUuid: String,
        apiUrl: { type: String, default: '/api' },
        taskId: String,
        currentUserUuid: String
    };

    connect() {
        this.isOpen = false;
        this.isTrashOpen = false;
        this.isUploading = false;
        this.pendingAction = null;
        this.members = [];
        this.projectTags = [];
        this.projectTasks = [];
        this.initialStatus = '';
        
        this.timelineOffset = 0;
        this.timelineLimit = 10;
        
        // Staged data for creation mode
        this.stagedAssignees = [];
        this.stagedTags = [];
        this.stagedLinks = [];
        this.stagedDependencies = [];
        
        this.loadInitialData();

        this.onProjectTagsChanged = () => this.loadProjectTags();
        window.addEventListener('project-tags-changed', this.onProjectTagsChanged);

        // Prevent page leave during upload
        this.beforeUnloadHandler = (e) => {
            if (this.isUploading) {
                e.preventDefault();
                e.returnValue = '';
            }
        };
        window.addEventListener('beforeunload', this.beforeUnloadHandler);
    }

    disconnect() {
        if (this.resizeObserver) {
            this.resizeObserver.disconnect();
        }
        window.removeEventListener('project-tags-changed', this.onProjectTagsChanged);
        window.removeEventListener('task-saved', this.onTaskSaved);
        window.removeEventListener('beforeunload', this.beforeUnloadHandler);
        if (this.sortables) {
            this.sortables.forEach(s => s.destroy());
        }
    }

    async loadInitialData() {
        try {
            const [membersRes, tagsRes] = await Promise.all([
                fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/members`, { credentials: 'include' }),
                fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/tags`, { credentials: 'include' })
            ]);

            if (membersRes.ok) this.members = await membersRes.json();
            if (tagsRes.ok) this.projectTags = await tagsRes.json();

            this.updateManagerSelect();
        } catch (e) {
            console.error("Failed to load initial modal data", e);
        }
    }

    async loadProjectTags() {
        try {
            const response = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/tags`, {
                credentials: 'include'
            });
            if (response.ok) {
                this.projectTags = await response.json();
                if (this.isOpen && !this.tagPickerTarget.classList.contains('hidden')) {
                    this.renderProjectTagList();
                }
            }
        } catch (e) {
            console.error("Failed to load project tags", e);
        }
    }

    updateManagerSelect() {
        this.managerSelectTarget.innerHTML = `<option value="">${trans('task.modal.empty.manager')}</option>`;
        this.members.forEach(m => {
            const option = document.createElement('option');
            option.value = m.user.uuid;
            option.textContent = `${m.user.firstName} ${m.user.lastName}`;
            this.managerSelectTarget.appendChild(option);
        });
    }

    openForCreate() {
        this.taskIdValue = "";
        this.isTaskTrashed = false;
        if (this.hasRestoreBtnTarget) this.restoreBtnTarget.classList.add('hidden');
        this.resetForm();
        this.initialStatus = 'TODO';
        this.toggleStatusMessage();
        this.modalTitleTarget.innerText = trans('task.modal.create_title');
        this.submitBtnTextTarget.innerText = trans('task.modal.buttons.create');
        
        // Reset staged data
        this.stagedAssignees = [];
        this.stagedTags = [];
        this.stagedLinks = [];
        this.stagedDependencies = [];
        
        this.subResourcesTarget.classList.remove('hidden');
        // Hide sections that require a taskId
        this.containerTarget.querySelectorAll('[data-section="attachments"], [data-section="comments"], [data-section="timeline"]').forEach(s => s.classList.add('hidden'));
        if (this.hasTrashSectionTarget) this.trashSectionTarget.classList.add('hidden');

        const stagedPerms = { taskOwnership: { isManager: true }, permissions: ['ALL'], isProjectAdmin: true };
        this.renderAssignees([], stagedPerms);
        this.renderTags([], stagedPerms);
        this.renderLinks([], stagedPerms);
        this.renderDependencies([], stagedPerms);
        
        this.taskMetaTarget.classList.add('hidden');
        this.deleteBtnTarget.classList.add('hidden');
        this.showForm();
        this.open();
    }

    async openForEdit(event) {
        const taskId = event.currentTarget.dataset.taskUuid || event.params.uuid;
        const isTrash = (event.currentTarget.dataset.isTrash === 'true') || (event.params.isTrash === true);
        if (!taskId) return;
        
        this.taskIdValue = taskId;
        this.isTaskTrashed = isTrash;
        this.modalTitleTarget.innerText = isTrash ? "Tâche supprimée" : "Modifier la tâche";
        this.submitBtnTextTarget.innerText = "Enregistrer les modifications";
        this.subResourcesTarget.classList.remove('hidden');
        this.containerTarget.querySelectorAll('[data-section="attachments"], [data-section="comments"], [data-section="timeline"]').forEach(s => s.classList.remove('hidden'));
        
        if (this.hasTrashSectionTarget) {
            this.trashSectionTarget.classList.remove('hidden');
            this.trashContentTarget.classList.add('hidden');
            this.isTrashOpen = false;
        }
        
        this.taskMetaTarget.classList.remove('hidden');
        this.deleteBtnTarget.classList.remove('hidden');
        if (this.hasRestoreBtnTarget) {
            this.restoreBtnTarget.classList.toggle('hidden', !isTrash);
        }
        
        this.open();
        this.showLoader();

        try {
            await this.refreshTaskData();
        } catch (e) {
            console.error("Failed to load task data", e);
        } finally {
            this.hideLoader();
        }
    }

    toggleStatusMessage() {
        if (!this.hasStatusMessageContainerTarget) return;
        const currentStatus = this.formTarget.status.value;
        const isChanged = currentStatus !== this.initialStatus;
        
        if (isChanged) {
            this.statusMessageContainerTarget.classList.remove('hidden');
        } else {
            this.statusMessageContainerTarget.classList.add('hidden');
            this.formTarget.statusMessage.value = '';
        }
    }

    async refreshTaskData() {
        const taskId = this.taskIdValue;
        const trashedParam = this.isTaskTrashed ? '?trashed=1' : '';
        this.timelineOffset = 0;
        this.timelineListTarget.innerHTML = '';

        const [taskRes, permRes, commentsRes, depsRes] = await Promise.all([
            fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${taskId}${trashedParam}`, { credentials: 'include' }),
            fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${taskId}/permissions`, { credentials: 'include' }),
            fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${taskId}/comments`, { credentials: 'include' }),
            fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${taskId}/dependencies`, { credentials: 'include' })
        ]);

        let perms = null;
        if (taskRes.ok && permRes.ok) {
            const task = await taskRes.json();
            perms = await permRes.json();
            this.fillForm(task, perms);
            this.renderAssignees(task.assignees, perms);
            this.renderTags(task.tags, perms);
            this.renderLinks(task.links, perms);
            this.renderDependencies(await depsRes.json(), perms);
            
            this.taskUuidTarget.innerText = task.uuid.slice(0, 8);
            this.taskCreatedAtTarget.innerText = new Date(task.createdAt).toLocaleDateString();

            if (this.isTrashOpen) await this.loadTrash(perms);
        }

        if (commentsRes.ok) this.renderComments(await commentsRes.json(), perms);
        await this.loadMoreTimeline();
        await this.renderAttachments(perms);
    }

    open() {
        if (this.isOpen) return;
        this.isOpen = true;
        this.containerTarget.classList.remove('hidden');
        document.body.style.overflow = 'hidden';
        
        setTimeout(() => {
            this.backdropTarget.classList.replace('opacity-0', 'opacity-100');
            this.panelTarget.classList.replace('translate-x-full', 'translate-x-0');
        }, 10);
    }

    close() {
        if (!this.isOpen) return;

        if (this.isUploading) {
            this.showConfirm(
                "Annuler le transfert ?", 
                "Un transfert est en cours. Si vous fermez ce modal, le transfert sera annulé. Voulez-vous continuer ?",
                () => {
                    this.isUploading = false; // Stop tracking to allow close
                    this.close();
                }
            );
            return;
        }

        this.isOpen = false;
        
        this.backdropTarget.classList.replace('opacity-100', 'opacity-0');
        this.panelTarget.classList.replace('translate-x-0', 'translate-x-full');
        document.body.style.overflow = '';
        
        setTimeout(() => {
            if (!this.isOpen) {
                this.containerTarget.classList.add('hidden');
            }
        }, 300);
    }

    resetForm() {
        this.formTarget.reset();
        this.fieldContainerTargets.forEach(c => {
            c.classList.remove('hidden');
            const input = c.querySelector('input, textarea, select');
            if (input) input.disabled = false;
        });
        if (this.hasStatusMessageContainerTarget) {
            this.statusMessageContainerTarget.classList.add('hidden');
        }
    }

    showLoader() {
        this.loaderTarget.classList.remove('hidden');
        this.formContentTarget.classList.add('hidden');
    }

    hideLoader() {
        this.loaderTarget.classList.add('hidden');
        this.formContentTarget.classList.remove('hidden');
    }

    showForm() {
        this.hideLoader();
        this.formContentTarget.classList.remove('hidden');
    }

    fillForm(task, perms) {
        this.currentPerms = perms;
        const form = this.formTarget;
        form.title.value = task.title || '';
        form.description.value = task.description || '';
        form.status.value = task.status || 'TODO';
        form.statusMessage.value = task.statusMessage || '';

        this.initialStatus = task.status || 'TODO';
        this.toggleStatusMessage();

        form.priority.value = task.priority || 1;

        form.estimatedHours.value = task.estimatedHours || '';

        const startParts = this.formatDateParts(task.startDate);
        form.startDate_date.value = startParts.date;
        form.startDate_time.value = startParts.time;

        const expiresParts = this.formatDateParts(task.expiresAt);
        form.expiresAt_date.value = expiresParts.date;
        form.expiresAt_time.value = expiresParts.time;

        form.managerUuid.value = task.manager ? task.manager.uuid : '';

        // Capture initial state for dirty checking
        this.initialState = {};
        const inputs = form.querySelectorAll('input, textarea, select');
        inputs.forEach(input => {
            if (input.name) {
                if (input.type === 'radio') {
                    if (input.checked) this.initialState[input.name] = input.value;
                } else if (input.type === 'number') {
                    this.initialState[input.name] = input.value === '' ? null : Number(input.value);
                } else {
                    this.initialState[input.name] = input.value;
                }
            }
        });

        // Permissions
        const editableFields = perms.editableFields || [];
        this.fieldContainerTargets.forEach(container => {
            const fieldName = container.dataset.field;
            let checkField = fieldName;
            if (fieldName === 'startDate') checkField = 'expiresAt';
            if (fieldName === 'managerUuid') checkField = 'manager';

            const isEditable = !this.isTaskTrashed && (editableFields.includes(checkField) || fieldName === 'status');
            const inputs = container.querySelectorAll('input, textarea, select');
            inputs.forEach(i => i.disabled = !isEditable);
        });

        const isTaskOwner = perms.taskOwnership.isManager || perms.taskOwnership.isCreator;
        const canDelete = this.isTaskTrashed 
            ? this.hasPerm('TASK_HARD_DELETE', isTaskOwner) 
            : this.hasPerm('TASK_DELETE', isTaskOwner);

        this.deleteBtnTarget.classList.toggle('hidden', !canDelete);
        this.deleteBtnTarget.title = this.isTaskTrashed ? "Supprimer définitivement" : "Supprimer la tâche";

        if (this.isTaskTrashed) {
            this.submitBtnTarget.classList.add('hidden');
        } else {
            this.submitBtnTarget.classList.remove('hidden');
        }

        // Action-based UI visibility
        const showAddBtns = !this.isTaskTrashed;

        const canAssign = this.hasPerm('TASK_ASSIGN_OTHERS', perms.taskOwnership.isManager) || this.hasPerm('TASK_ASSIGN_SELF');
        if (this.hasAssigneeAddContainerTarget) {
            this.assigneeAddContainerTarget.classList.toggle('hidden', !showAddBtns || !canAssign);
        }
        if (this.hasTagAddContainerTarget) {
            this.tagAddContainerTarget.classList.toggle('hidden', !showAddBtns || !this.hasPerm('TASK_TAG_MANAGE', perms.taskOwnership.isManager || perms.taskOwnership.isAssignee));
        }
        if (this.hasLinkAddBtnTarget) {
            this.linkAddBtnTarget.classList.toggle('hidden', !showAddBtns || !this.hasPerm('TASK_LINK_MANAGE', perms.taskOwnership.isManager || perms.taskOwnership.isAssignee));
        }
        if (this.hasDependencyAddBtnTarget) {
            this.dependencyAddBtnTarget.classList.toggle('hidden', !showAddBtns || !this.hasPerm('TASK_DEPENDENCY_MANAGE', perms.taskOwnership.isManager || perms.taskOwnership.isAssignee));
        }
        if (this.hasAttachmentAddContainerTarget) {
            this.attachmentAddContainerTarget.classList.toggle('hidden', !showAddBtns || !this.hasPerm('ATTACHMENT_ADD', perms.taskOwnership.isManager || perms.taskOwnership.isAssignee));
        }
        if (this.hasCommentFormContainerTarget) {
            this.commentFormContainerTarget.classList.toggle('hidden', !showAddBtns || !this.hasPerm('COMMENT_CREATE', perms.taskOwnership.isManager || perms.taskOwnership.isAssignee));
        }
    }

    hasPerm(permBaseName, isOwner = false) {
        if (!this.currentPerms) return false;
        const p = this.currentPerms.permissions || [];

        if (this.currentPerms.isProjectAdmin || p.includes('ALL')) return true;

        // 1. Exact match
        if (p.includes(permBaseName)) return true;

        // 2. ALL variant
        if (p.includes(`${permBaseName}_ALL`)) return true;

        // 3. OWN variant
        if (isOwner && p.includes(`${permBaseName}_OWN`)) return true;

        return false;
    }
    formatDateParts(dateString) {
        if (!dateString) return { date: '', time: '' };
        const d = new Date(dateString);
        if (isNaN(d.getTime())) return { date: '', time: '' };
        
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        
        return { 
            date: `${year}-${month}-${day}`, 
            time: `${hours}:${minutes}` 
        };
    }

    // --- ASSIGNEES ---
    renderAssignees(assignees, perms) {
        this.assigneeListTarget.innerHTML = '';
        const canManage = this.hasPerm('TASK_ASSIGN_OTHERS', perms.taskOwnership?.isManager) || this.hasPerm('TASK_ASSIGN_SELF');

        assignees.forEach(a => {
            const div = document.createElement('div');
            div.className = 'flex items-center gap-2 px-3 py-1.5 bg-gray-100 rounded-full text-xs font-bold text-gray-700';
            div.innerHTML = `
                <span>${a.firstName} ${a.lastName}</span>
                ${canManage ? `<button type="button" data-action="click->task-modal#removeAssignee" data-user-uuid="${a.uuid}" class="text-gray-400 hover:text-rose-500 transition-colors"><svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M6 18L18 6M6 6l12 12"></path></svg></button>` : ''}
            `;
            this.assigneeListTarget.appendChild(div);
        });
    }

    toggleUserPicker() {
        this.userPickerTarget.classList.toggle('hidden');
        if (!this.userPickerTarget.classList.contains('hidden')) {
            this.renderUserList();
        }
    }

    renderUserList() {
        this.userListTarget.innerHTML = '';
        this.members.forEach(m => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'w-full text-left px-3 py-2 text-xs font-bold text-gray-600 hover:bg-gray-50 rounded-lg transition-colors flex items-center justify-between group';
            btn.dataset.action = 'click->task-modal#addAssignee';
            btn.dataset.userUuid = m.user.uuid;
            btn.innerHTML = `
                <span>${m.user.firstName} ${m.user.lastName}</span>
                <span class="opacity-0 group-hover:opacity-100 text-[var(--highlight-color)]">+</span>
            `;
            this.userListTarget.appendChild(btn);
        });
    }

    async addAssignee(event) {
        const userUuid = event.currentTarget.dataset.userUuid;
        if (!this.taskIdValue) {
            const member = this.members.find(m => m.user.uuid === userUuid);
            if (member && !this.stagedAssignees.find(a => a.uuid === userUuid)) {
                this.stagedAssignees.push(member.user);
                this.renderAssignees(this.stagedAssignees, { taskOwnership: { isManager: true } }); // Full access for staged
            }
            this.userPickerTarget.classList.add('hidden');
            return;
        }

        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/assignees`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userUuid }),
                credentials: 'include'
            });
            if (res.ok) {
                this.userPickerTarget.classList.add('hidden');
                await this.refreshTaskData();
            }
        } catch (e) { console.error(e); }
    }

    async removeAssignee(event) {
        const userUuid = event.currentTarget.dataset.userUuid;
        if (!this.taskIdValue) {
            this.stagedAssignees = this.stagedAssignees.filter(a => a.uuid !== userUuid);
            this.renderAssignees(this.stagedAssignees, { taskOwnership: { isManager: true } });
            return;
        }

        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/assignees/${userUuid}`, {
                method: 'DELETE',
                credentials: 'include'
            });
            if (res.ok) await this.refreshTaskData();
        } catch (e) { console.error(e); }
    }

    // --- TAGS ---
    renderTags(tags, perms) {
        this.taskTagListTarget.innerHTML = '';
        const canManage = this.hasPerm('TASK_TAG_MANAGE', perms.taskOwnership?.isManager || perms.taskOwnership?.isAssignee);

        tags.forEach(t => {
            const div = document.createElement('div');
            div.className = 'flex items-center gap-2 px-3 py-1 bg-white border border-gray-100 rounded-lg text-[10px] font-black uppercase tracking-widest';
            div.style.color = t.color;
            div.innerHTML = `
                <span class="w-1.5 h-1.5 rounded-full" style="background-color: ${t.color}"></span>
                <span>${t.name}</span>
                ${canManage ? `<button type="button" data-action="click->task-modal#removeTag" data-tag-uuid="${t.uuid}" class="text-gray-300 transition-colors cursor-pointer" onmouseover="this.style.color='var(--highlight-color)'" onmouseout="this.style.color='#d1d5db'"><svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M6 18L18 6M6 6l12 12"></path></svg></button>` : ''}
            `;
            this.taskTagListTarget.appendChild(div);
        });
    }

    toggleTagPicker() {
        this.tagPickerTarget.classList.toggle('hidden');
        if (!this.tagPickerTarget.classList.contains('hidden')) {
            this.renderProjectTagList();
        }
    }

    renderProjectTagList() {
        this.projectTagListTarget.innerHTML = '';
        if (this.projectTags.length === 0) {
            this.projectTagListTarget.innerHTML = `<p class="text-[10px] text-gray-400 italic p-4 text-center">${trans('task.modal.empty.tags')}</p>`;
            return;
        }
        this.projectTags.forEach(t => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'w-full text-left px-3 py-2 text-[10px] font-black uppercase tracking-widest text-gray-500 hover:bg-gray-50 rounded-lg transition-colors flex items-center gap-2';
            btn.dataset.action = 'click->task-modal#addTag';
            btn.dataset.tagUuid = t.uuid;
            btn.innerHTML = `
                <span class="w-2 h-2 rounded-full" style="background-color: ${t.color}"></span>
                <span>${t.name}</span>
            `;
            this.projectTagListTarget.appendChild(btn);
        });
    }

    async addTag(event) {
        const tagUuid = event.currentTarget.dataset.tagUuid;
        if (!this.taskIdValue) {
            const tag = this.projectTags.find(t => t.uuid === tagUuid);
            if (tag && !this.stagedTags.find(t => t.uuid === tagUuid)) {
                this.stagedTags.push(tag);
                this.renderTags(this.stagedTags, { taskOwnership: { isManager: true } });
            }
            this.tagPickerTarget.classList.add('hidden');
            return;
        }

        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/tags`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ tagUuid }),
                credentials: 'include'
            });
            if (res.ok) {
                this.tagPickerTarget.classList.add('hidden');
                await this.refreshTaskData();
                window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
            }
        } catch (e) { console.error(e); }
    }

    async removeTag(event) {
        const tagUuid = event.currentTarget.dataset.tagUuid;
        if (!this.taskIdValue) {
            this.stagedTags = this.stagedTags.filter(t => t.uuid !== tagUuid);
            this.renderTags(this.stagedTags, { taskOwnership: { isManager: true } });
            return;
        }

        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/tags/${tagUuid}`, {
                method: 'DELETE',
                credentials: 'include'
            });
            if (res.ok) {
                await this.refreshTaskData();
                window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
            }
        } catch (e) { console.error(e); }
    }

    // --- LINKS ---
    renderLinks(links, perms) {
        this.linkListTarget.innerHTML = '';
        const canManage = this.hasPerm('TASK_LINK_MANAGE', perms.taskOwnership?.isManager || perms.taskOwnership?.isAssignee);

        links.forEach(l => {
            const div = document.createElement('div');
            div.className = 'flex items-center justify-between p-4 bg-white border border-gray-100 rounded-2xl group';
            div.innerHTML = `
                <div class="flex flex-col">
                    <a href="${l.url}" target="_blank" class="text-xs font-bold text-gray-900 hover:text-[var(--highlight-color)] transition-colors flex items-center gap-2">
                        ${l.description || l.url}
                        <svg class="w-3 h-3 opacity-0 group-hover:opacity-100 transition-opacity" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"></path></svg>
                    </a>
                    <span class="text-[10px] text-gray-400 font-medium truncate max-w-[200px]">${l.url}</span>
                </div>
                ${canManage ? `<button type="button" data-action="click->task-modal#removeLink" data-link-uuid="${l.uuid || l.url}" class="p-2 text-gray-300 hover:text-rose-500 transition-colors opacity-0 group-hover:opacity-100"><svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg></button>` : ''}
            `;
            this.linkListTarget.appendChild(div);
        });
    }

    showLinkForm() { this.linkFormTarget.classList.remove('hidden'); }
    hideLinkForm() { this.linkFormTarget.classList.add('hidden'); }

    async addLink() {
        const url = this.linkUrlTarget.value;
        const description = this.linkDescTarget.value;
        if (!url) return;

        if (!this.taskIdValue) {
            this.stagedLinks.push({ url, description });
            this.renderLinks(this.stagedLinks, { taskOwnership: { isManager: true } });
            this.linkUrlTarget.value = '';
            this.linkDescTarget.value = '';
            this.hideLinkForm();
            return;
        }

        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/links`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ url, description }),
                credentials: 'include'
            });
            if (res.ok) {
                this.linkUrlTarget.value = '';
                this.linkDescTarget.value = '';
                this.hideLinkForm();
                await this.refreshTaskData();
                window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
            }
        } catch (e) { console.error(e); }
    }

    async removeLink(event) {
        const linkUuid = event.currentTarget.dataset.linkUuid;
        if (!this.taskIdValue) {
            this.stagedLinks = this.stagedLinks.filter(l => (l.uuid || l.url) !== linkUuid);
            this.renderLinks(this.stagedLinks, { taskOwnership: { isManager: true } });
            return;
        }

        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/links/${linkUuid}`, {
                method: 'DELETE',
                credentials: 'include'
            });
            if (res.ok) {
                await this.refreshTaskData();
                window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
            }
        } catch (e) { console.error(e); }
    }

    // --- COMMENTS ---
    renderComments(comments, perms = null) {
        this.commentListTarget.innerHTML = '';
        comments.forEach(c => {
            const isResourceOwner = (c.user.uuid === this.currentUserUuidValue);
            const canDelete = this.hasPerm('COMMENT_DELETE', isResourceOwner);
            
            const div = document.createElement('div');
            div.className = 'flex gap-4 group/comment';
            div.innerHTML = `
                <div class="w-8 h-8 rounded-xl bg-gray-100 flex items-center justify-center text-[10px] font-black uppercase text-gray-500 shrink-0">
                    ${c.user.firstName[0]}${c.user.lastName[0]}
                </div>
                <div class="flex-grow space-y-1">
                    <div class="flex items-center justify-between">
                        <div class="flex items-center gap-2">
                            <span class="text-xs font-black text-gray-900">${c.user.firstName} ${c.user.lastName}</span>
                            <span class="text-[10px] font-bold text-gray-400">${new Date(c.createdAt).toLocaleString()}</span>
                        </div>
                        ${canDelete ? `
                            <button data-action="click->task-modal#removeComment" data-comment-uuid="${c.uuid}" class="opacity-0 group-hover/comment:opacity-100 p-1.5 text-gray-400 hover:text-rose-500 transition-all cursor-pointer">
                                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                            </button>
                        ` : ''}
                    </div>
                    <div class="p-4 bg-white border border-gray-100 rounded-2xl text-xs text-gray-600 leading-relaxed shadow-sm">
                        ${c.content}
                    </div>
                </div>
            `;
            this.commentListTarget.appendChild(div);
        });
    }

    async addComment() {
        const content = this.commentContentTarget.value;
        if (!content) return;

        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/comments`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content }),
                credentials: 'include'
            });
            if (res.ok) {
                this.commentContentTarget.value = '';
                await this.refreshTaskData();
                window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
            }
        } catch (e) { console.error(e); }
    }

    // --- TIMELINE ---
    async loadMoreTimeline() {
        const taskId = this.taskIdValue;
        if (!taskId) return;

        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${taskId}/timeline?offset=${this.timelineOffset}&limit=${this.timelineLimit}`, { credentials: 'include' });
            if (res.ok) {
                const items = await res.json();
                this.renderTimeline(items);
                this.timelineOffset += this.timelineLimit;
                
                if (this.hasLoadMoreTimelineBtnTarget) {
                    this.loadMoreTimelineBtnTarget.classList.toggle('hidden', items.length < this.timelineLimit);
                }
            }
        } catch (e) { console.error(e); }
    }

    renderTimeline(timeline) {
        timeline.forEach(t => {
            const div = document.createElement('div');
            div.className = 'relative space-y-1 group/item';
            
            let detail = t.detail;
            let hasDetails = false;
            let detailsHtml = '';

            const userName = t.first_name ? `${t.first_name} ${t.last_name}` : 'Système';

            if (t.type === 'HISTORY') {
                let actionKey = `task.history.actions.${t.action_type}`;
                let fieldLabel = t.field_name ? trans(`task.modal.fields.${t.field_name}`) : '';
                
                if (t.action_type === 'UPDATE') {
                    if (t.field_name === 'status') actionKey = 'task.history.actions.STATUS_CHANGE';
                    if (t.field_name === 'priority') actionKey = 'task.history.actions.PRIORITY_CHANGE';
                    
                    hasDetails = true;
                    const formatVal = (val) => {
                        if (!val || val === 'null') return '<i>vide</i>';
                        if (t.field_name?.toLowerCase().includes('date') || t.field_name === 'expiresAt') {
                            try { return new Date(val).toLocaleString(); } catch(e) { return val; }
                        }
                        if (t.field_name === 'status') {
                            return trans(`task.modal.status_labels.${val}`);
                        }
                        return val;
                    };
                    
                    detailsHtml = `
                        <div class="mt-2 p-4 bg-gray-50 rounded-2xl border border-gray-100 text-[10px] font-medium text-gray-500 flex flex-col gap-3 overflow-hidden hidden group-data-[expanded=true]/item:flex transition-all">
                            <div class="flex items-center gap-3">
                                <span class="w-16 px-1.5 py-0.5 bg-rose-50 text-rose-500 rounded-md font-black uppercase tracking-tighter text-center shrink-0">Ancien</span>
                                <span class="truncate bg-white px-2 py-1 rounded-lg border border-gray-100 flex-grow">${formatVal(t.old_value)}</span>
                            </div>
                            <div class="flex items-center gap-3">
                                <span class="w-16 px-1.5 py-0.5 bg-green-50 text-green-500 rounded-md font-black uppercase tracking-tighter text-center shrink-0">Nouveau</span>
                                <span class="truncate font-bold text-gray-700 bg-white px-2 py-1 rounded-lg border border-gray-100 flex-grow">${formatVal(t.new_value)}</span>
                            </div>
                        </div>
                    `;
                }

                const actionLabel = trans(actionKey);
                
                if (t.action_type === 'ASSIGNEE_ADD' || t.action_type === 'ASSIGNEE_REMOVE') {
                    const targetName = t.target_first_name ? `${t.target_first_name} ${t.target_last_name}` : (detail || 'un utilisateur');
                    detail = `${actionLabel} ${targetName}`;
                } else if (t.action_type === 'RESTORE') {
                    let itemType = "la tâche";
                    if (t.field_name === 'link') itemType = "le lien";
                    if (t.field_name === 'attachment') itemType = "la pièce jointe";
                    detail = `${actionLabel} ${itemType}`;
                } else if (t.action_type === 'CREATE') {
                    detail = actionLabel;
                } else {
                    detail = `${actionLabel} ${fieldLabel.toLowerCase()}`.trim();
                }
            }

            const typeLabel = trans(`task.history.types.${t.type}`);

            div.innerHTML = `
                <div class="absolute -left-[25px] top-1.5 w-4 h-4 rounded-full bg-white border-2 border-gray-100 group-hover/item:border-[var(--highlight-color)] transition-colors"></div>
                <div class="flex items-center gap-2">
                    <span class="text-[10px] font-black uppercase tracking-widest text-gray-400 group-hover/item:text-[var(--highlight-color)] transition-colors">${typeLabel}</span>
                    <span class="text-[10px] font-bold text-gray-300">•</span>
                    <span class="text-[10px] font-bold text-gray-400">${new Date(t.created_at || t.createdAt).toLocaleString()}</span>
                </div>
                <div class="${hasDetails ? 'cursor-pointer' : ''}" data-action="${hasDetails ? 'click->task-modal#toggleHistoryDetails' : ''}">
                    <p class="text-xs font-bold text-gray-600 flex items-center gap-2">
                        <span class="text-black">${userName}</span>
                        <span>${detail}</span>
                        ${hasDetails ? '<svg class="w-3 h-3 text-gray-300 group-data-[expanded=true]/item:rotate-180 group-hover/item:text-[var(--highlight-color)] transition-all" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M19 9l-7 7-7-7"></path></svg>' : ''}
                    </p>
                    ${detailsHtml}
                </div>
            `;
            this.timelineListTarget.appendChild(div);
        });
    }

    toggleHistoryDetails(event) {
        const item = event.currentTarget.closest('.group\\/item');
        const isExpanded = item.dataset.expanded === 'true';
        item.dataset.expanded = !isExpanded;
    }

    // --- DEPENDENCIES ---
    async renderDependencies(dependencies, perms) {
        this.dependencyListTarget.innerHTML = '';
        const canManage = this.hasPerm('TASK_DEPENDENCY_MANAGE', perms.taskOwnership?.isManager || perms.taskOwnership?.isAssignee);

        dependencies.forEach(d => {
            const div = document.createElement('div');
            div.className = 'flex items-center justify-between p-3 bg-white border border-gray-100 rounded-xl group';
            div.innerHTML = `
                <div class="flex items-center gap-3">
                    <div class="w-2 h-2 rounded-full ${d.status === 'DONE' ? 'bg-green-400' : 'bg-amber-400'}"></div>
                    <span class="text-xs font-bold text-gray-700">${d.title}</span>
                </div>
                ${canManage ? `<button type="button" data-action="click->task-modal#removeDependency" data-target-uuid="${d.dependsOnTaskUuid || d.uuid}" class="text-gray-300 hover:text-rose-500 transition-colors opacity-0 group-hover:opacity-100"><svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg></button>` : ''}
            `;
            this.dependencyListTarget.appendChild(div);
        });
    }

    toggleDependencyPicker() {
        this.dependencyPickerTarget.classList.toggle('hidden');
        if (!this.dependencyPickerTarget.classList.contains('hidden')) {
            this.loadProjectTasks();
        }
    }

    async loadProjectTasks() {
        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks`, { credentials: 'include' });
            if (res.ok) {
                const tasks = await res.json();
                this.projectTasks = tasks.filter(t => t.uuid !== this.taskIdValue);
                this.renderProjectTaskList();
            }
        } catch (e) { console.error(e); }
    }

    renderProjectTaskList() {
        this.projectTaskListTarget.innerHTML = '';
        this.projectTasks.forEach(t => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'w-full text-left px-3 py-2 text-xs font-bold text-gray-600 hover:bg-gray-50 rounded-lg transition-colors flex items-center justify-between group';
            btn.dataset.action = 'click->task-modal#addDependency';
            btn.dataset.targetUuid = t.uuid;
            btn.innerHTML = `
                <span>${t.title}</span>
                <span class="text-[10px] text-gray-400 font-medium">${t.status}</span>
            `;
            this.projectTaskListTarget.appendChild(btn);
        });
    }

    async addDependency(event) {
        const dependsOnTaskUuid = event.currentTarget.dataset.targetUuid;
        if (!this.taskIdValue) {
            const task = this.projectTasks.find(t => t.uuid === dependsOnTaskUuid);
            if (task && !this.stagedDependencies.find(d => d.uuid === dependsOnTaskUuid)) {
                this.stagedDependencies.push(task);
                this.renderDependencies(this.stagedDependencies.map(d => ({ dependsOnTaskUuid: d.uuid, title: d.title, status: d.status })), { taskOwnership: { isManager: true } });
            }
            this.dependencyPickerTarget.classList.add('hidden');
            return;
        }

        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/dependencies`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ dependsOnTaskUuid }),
                credentials: 'include'
            });
            if (res.ok) {
                this.dependencyPickerTarget.classList.add('hidden');
                await this.refreshTaskData();
                window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
            }
        } catch (e) { console.error(e); }
    }

    async removeDependency(event) {
        const targetUuid = event.currentTarget.dataset.targetUuid;
        if (!this.taskIdValue) {
            this.stagedDependencies = this.stagedDependencies.filter(d => d.uuid !== targetUuid);
            this.renderDependencies(this.stagedDependencies.map(d => ({ dependsOnTaskUuid: d.uuid, title: d.title, status: d.status })), { taskOwnership: { isManager: true } });
            return;
        }

        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/dependencies/${targetUuid}`, {
                method: 'DELETE',
                credentials: 'include'
            });
            if (res.ok) {
                await this.refreshTaskData();
                window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
            }
        } catch (e) { console.error(e); }
    }

    filterUsers(event) {
        const query = event.target.value.toLowerCase();
        const items = this.userListTarget.querySelectorAll('button');
        items.forEach(item => {
            const text = item.innerText.toLowerCase();
            item.classList.toggle('hidden', !text.includes(query));
        });
    }

    filterTasks(event) {
        const query = event.target.value.toLowerCase();
        const items = this.projectTaskListTarget.querySelectorAll('button');
        items.forEach(item => {
            const text = item.innerText.toLowerCase();
            item.classList.toggle('hidden', !text.includes(query));
        });
    }

    async uploadFile(event) {
        const file = event.target.files[0];
        if (!file) return;

        this.isUploading = true;
        try {
            // 1. Initialize upload (Send metadata only)
            const initRes = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/attachments/init-upload`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    fileName: file.name,
                    fileSize: file.size,
                    fileType: file.type
                }),
                credentials: 'include'
            });

            if (!initRes.ok) {
                const error = await initRes.json();
                const title = initRes.status === 413 ? "Fichier trop lourd" : (initRes.status === 415 ? "Format non supporté" : "Erreur d'upload");
                
                // Use translation keys from server if possible, with parameters
                let message = "Impossible d'initialiser le transfert.";
                if (error.message && error.message.startsWith('error.')) {
                    message = trans(`task.modal.${error.message}`, { limit: error.limit });
                } else {
                    message = error.message || message;
                }

                this.showError(title, message);
                return;
            }

            const initData = await initRes.json();

            // 2. Perform actual upload based on instructions
            if (initData.action === 'upload_to_drive') {
                await this.uploadToGoogleDrive(file, initData);
            } else {
                // Perform local upload
                const formData = new FormData();
                formData.append('file', file);
                
                const uploadRes = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/attachments`, {
                    method: 'POST',
                    body: formData,
                    credentials: 'include'
                });

                if (uploadRes.ok) {
                    await this.refreshTaskData();
                } else {
                    if (uploadRes.status === 413) {
                        this.showError("Fichier trop lourd", "Ce fichier est trop volumineux pour être envoyé sur nos serveurs.");
                    } else {
                        const contentType = uploadRes.headers.get("content-type");
                        if (contentType && contentType.indexOf("application/json") !== -1) {
                            const error = await uploadRes.json();
                            this.showError("Erreur d'envoi", error.message || "Erreur lors de l'envoi du fichier.");
                        } else {
                            this.showError("Erreur serveur", "Une erreur serveur est survenue (500).");
                        }
                    }
                }
            }
        } catch (e) { console.error(e); }
        finally {
            this.isUploading = false;
        }
    }

    async uploadToGoogleDrive(file, driveData) {
        this.showUploadProgress(file.name);
        this.isUploading = true;
        
        try {
            const metadata = {
                name: file.name,
                parents: [driveData.folderId]
            };

            const form = new FormData();
            form.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
            form.append('file', file);

            // We use XMLHttpRequest instead of fetch to get upload progress
            const xhr = new XMLHttpRequest();
            
            xhr.upload.onprogress = (event) => {
                if (event.lengthComputable) {
                    const percent = Math.round((event.loaded / event.total) * 100);
                    this.updateUploadProgress(percent);
                }
            };

            const promise = new Promise((resolve, reject) => {
                xhr.onload = () => {
                    if (xhr.status >= 200 && xhr.status < 300) {
                        resolve(JSON.parse(xhr.responseText));
                    } else {
                        reject(new Error(`Upload failed with status ${xhr.status}`));
                    }
                };
                xhr.onerror = () => reject(new Error("Network error during upload"));
            });

            xhr.open('POST', 'https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id');
            xhr.setRequestHeader('Authorization', `Bearer ${driveData.accessToken}`);
            xhr.send(form);

            const result = await promise;

            // 2. Confirm to our API
            const confirmRes = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/attachments/drive-confirm`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    driveId: result.id,
                    fileName: file.name,
                    fileSize: file.size,
                    fileType: file.type
                }),
                credentials: 'include'
            });

            if (confirmRes.ok) {
                await this.refreshTaskData();
            } else {
                this.showError("Erreur Google Drive", "Erreur lors de la confirmation du fichier auprès de l'API.");
            }
        } catch (e) {
            console.error("Google Drive Upload Error", e);
            this.showError("Échec de l'envoi", "Échec de l'envoi vers Google Drive.");
        } finally {
            this.isUploading = false;
            this.hideUploadProgress();
        }
    }

    showUploadProgress(fileName) {
        if (!this.hasUploadProgressContainerTarget) return;
        this.uploadFileNameTarget.innerText = fileName;
        this.updateUploadProgress(0);
        this.uploadProgressContainerTarget.classList.remove('hidden');
    }

    updateUploadProgress(percent) {
        if (!this.hasUploadProgressBarTarget) return;
        this.uploadProgressBarTarget.style.width = `${percent}%`;
        this.uploadPercentageTarget.innerText = `${percent}%`;
    }

    hideUploadProgress() {
        if (!this.hasUploadProgressContainerTarget) return;
        this.uploadProgressContainerTarget.classList.add('hidden');
    }

    // --- CONFIRMATION MODAL ---
    showConfirm(title, message, action) {
        this.confirmTitleTarget.innerText = title;
        this.confirmMessageTarget.innerText = message;
        this.pendingAction = action;
        this.confirmModalTarget.classList.remove('hidden');
    }

    executeConfirm() {
        if (this.pendingAction) this.pendingAction();
        this.cancelConfirm();
    }

    cancelConfirm() {
        this.confirmModalTarget.classList.add('hidden');
        this.pendingAction = null;
    }

    showError(title, message) {
        this.errorTitleTarget.innerText = title;
        this.errorMessageTarget.innerText = message;
        this.errorModalTarget.classList.remove('hidden');
    }

    hideError() {
        this.errorModalTarget.classList.add('hidden');
    }

    // --- TRASH & RESTORATION ---
    async toggleTrash() {
        this.isTrashOpen = !this.isTrashOpen;
        this.trashContentTarget.classList.toggle('hidden', !this.isTrashOpen);
        if (this.isTrashOpen) await this.loadTrash(this.currentPerms);
    }

    async loadTrash(perms = null) {
        const taskId = this.taskIdValue;
        const [attRes, linkRes, commentRes] = await Promise.all([
            fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${taskId}/attachments/trash`, { credentials: 'include' }),
            fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${taskId}/links/trash`, { credentials: 'include' }),
            fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${taskId}/comments/trash`, { credentials: 'include' })
        ]);

        if (attRes.ok) this.renderDeletedAttachments(await attRes.json(), perms);
        if (linkRes.ok) this.renderDeletedLinks(await linkRes.json(), perms);
        if (commentRes.ok) this.renderDeletedComments(await commentRes.json(), perms);
    }

    renderDeletedComments(comments, perms = null) {
        this.deletedCommentListTarget.innerHTML = '';
        if (comments.length === 0) {
            this.deletedCommentListTarget.innerHTML = `<p class="text-[10px] text-gray-300 italic p-4 text-center">${trans('task.modal.empty.comments')}</p>`;
            return;
        }

        comments.forEach(c => {
            const isResourceOwner = (c.user.uuid === this.currentUserUuidValue);
            const canRestore = this.hasPerm('COMMENT_DELETE', isResourceOwner);
            const canHardDelete = this.hasPerm('COMMENT_HARD_DELETE', isResourceOwner);

            const div = document.createElement('div');
            div.className = 'flex flex-col gap-3 p-4 bg-white border border-gray-100 rounded-2xl group transition-all';
            div.innerHTML = `
                <div class="flex items-start justify-between">
                    <div class="flex items-center gap-2">
                        <div class="w-6 h-6 rounded-lg bg-gray-50 flex items-center justify-center text-[8px] font-black uppercase text-gray-400">
                            ${c.user.firstName[0]}${c.user.lastName[0]}
                        </div>
                        <div class="flex flex-col">
                            <span class="text-[10px] font-bold text-gray-900">${c.user.firstName} ${c.user.lastName}</span>
                            <span class="text-[9px] font-medium text-gray-400">Supprimé le ${new Date(c.deletedAt).toLocaleString()}</span>
                        </div>
                    </div>
                    <div class="flex gap-2">
                        ${canRestore ? `
                            <button data-action="click->task-modal#restoreComment" data-uuid="${c.uuid}"
                                    class="p-2 text-[var(--highlight-color)] hover:bg-[var(--highlight-color)]/10 rounded-xl transition-all cursor-pointer" title="Restaurer">
                                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M3 10h10a8 8 0 018 8v2M3 10l5 5m-5-5l5-5"/></svg>
                            </button>
                        ` : ''}
                        ${canHardDelete ? `
                            <button data-action="click->task-modal#removeCommentPermanent" data-uuid="${c.uuid}"
                                    class="p-2 text-gray-300 hover:text-rose-500 hover:bg-rose-50 rounded-xl transition-all cursor-pointer" title="Supprimer définitivement">
                                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                            </button>
                        ` : ''}
                    </div>
                </div>
                <p class="text-[11px] text-gray-500 italic line-clamp-2 pl-8">"${c.content}"</p>
            `;
            this.deletedCommentListTarget.appendChild(div);
        });
    }

    async restoreComment(event) {
        const uuid = event.currentTarget.dataset.uuid;
        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/comments/${uuid}/restore`, {
                method: 'POST',
                credentials: 'include'
            });
            if (res.ok) {
                await this.refreshTaskData();
                window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
            }
        } catch (e) { console.error(e); }
    }

    async removeComment(event) {
        const uuid = event.currentTarget.dataset.commentUuid;
        this.showConfirm(
            "Supprimer le commentaire ?", 
            "Ce commentaire sera déplacé vers la corbeille de la tâche.",
            async () => {
                try {
                    const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/comments/${uuid}`, {
                        method: 'DELETE',
                        credentials: 'include'
                    });
                    if (res.ok) {
                        await this.refreshTaskData();
                        window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
                    }
                } catch (e) { console.error(e); }
            }
        );
    }

    async removeCommentPermanent(event) {
        const uuid = event.currentTarget.dataset.uuid;
        this.showConfirm(
            "Suppression définitive ?", 
            "Cette action est irréversible. Le commentaire sera définitivement supprimé.",
            async () => {
                try {
                    const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/comments/${uuid}?permanent=true`, {
                        method: 'DELETE',
                        credentials: 'include'
                    });
                    if (res.ok) {
                        await this.refreshTaskData();
                        window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
                    }
                } catch (e) { console.error(e); }
            }
        );
    }

    renderDeletedAttachments(attachments, perms = null) {
        this.deletedAttachmentListTarget.innerHTML = '';
        if (attachments.length === 0) {
            this.deletedAttachmentListTarget.innerHTML = `<p class="text-[10px] text-gray-300 italic p-4 text-center col-span-2">${trans('task.modal.empty.attachments')}</p>`;
            return;
        }

        attachments.forEach(a => {
            const isResourceOwner = (a.uploadedBy.uuid === this.currentUserUuidValue);
            const canRestore = this.hasPerm('ATTACHMENT_DELETE', isResourceOwner);
            const canHardDelete = this.hasPerm('ATTACHMENT_HARD_DELETE', isResourceOwner);

            const div = document.createElement('div');
            div.className = 'flex items-center justify-between p-3 bg-gray-50 border border-gray-100 rounded-2xl group transition-all';
            div.innerHTML = `
                <div class="flex items-center gap-3 min-w-0">
                    <div class="w-10 h-10 bg-white rounded-xl flex items-center justify-center text-gray-400 shrink-0 border border-gray-100">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"/></svg>
                    </div>
                    <div class="flex flex-col min-w-0">
                        <span class="text-xs font-bold text-gray-900 truncate max-w-[150px]">${a.fileName}</span>
                        <span class="text-[9px] text-gray-400 font-medium">Supprimé le ${new Date(a.deletedAt).toLocaleDateString()}</span>
                    </div>
                </div>
                <div class="flex gap-2">
                    ${canRestore ? `
                        <button type="button" data-action="click->task-modal#restoreAttachment" data-uuid="${a.uuid}"
                                class="px-3 py-1.5 bg-white border border-gray-200 text-[var(--highlight-color)] text-[9px] font-black uppercase tracking-widest rounded-lg hover:border-[var(--highlight-color)] transition-all cursor-pointer">
                            Restaurer
                        </button>
                    ` : ''}
                    ${canHardDelete ? `
                        <button type="button" data-action="click->task-modal#removeAttachmentPermanent" data-uuid="${a.uuid}"
                                class="p-2 bg-rose-50 text-rose-500 rounded-lg hover:bg-rose-100 transition-all cursor-pointer">
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" /></svg>
                        </button>
                    ` : ''}
                </div>
            `;
            this.deletedAttachmentListTarget.appendChild(div);
        });
    }

    renderDeletedLinks(links, perms = null) {
        this.deletedLinkListTarget.innerHTML = '';
        if (links.length === 0) {
            this.deletedLinkListTarget.innerHTML = `<p class="text-[10px] text-gray-300 italic p-4 text-center">${trans('task.modal.empty.links')}</p>`;
            return;
        }

        links.forEach(l => {
            const canRestore = this.hasPerm('TASK_LINK_MANAGE', this.currentPerms?.taskOwnership?.isManager || this.currentPerms?.taskOwnership?.isAssignee);
            const canHardDelete = this.hasPerm('TASK_LINK_HARD_DELETE', this.currentPerms?.taskOwnership?.isManager);

            const div = document.createElement('div');
            div.className = 'p-3 bg-gray-50 border border-gray-100 rounded-xl flex items-center justify-between group';
            div.innerHTML = `
                <div class="flex flex-col min-w-0">
                    <span class="text-xs font-bold text-gray-900 truncate max-w-[200px]">${l.description || l.url}</span>
                    <span class="text-[9px] text-gray-400 font-medium">${l.url}</span>
                </div>
                <div class="flex gap-2">
                    ${canRestore ? `
                        <button type="button" data-action="click->task-modal#restoreLink" data-uuid="${l.uuid}"
                                class="px-3 py-1.5 bg-white border border-gray-200 text-[var(--highlight-color)] text-[9px] font-black uppercase tracking-widest rounded-lg hover:border-[var(--highlight-color)] transition-all cursor-pointer">
                            Restaurer
                        </button>
                    ` : ''}
                    ${canHardDelete ? `
                        <button type="button" data-action="click->task-modal#removeLinkPermanent" data-uuid="${l.uuid}"
                                class="p-2 bg-rose-50 text-rose-500 rounded-lg hover:bg-rose-100 transition-all cursor-pointer">
                            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" /></svg>
                        </button>
                    ` : ''}
                </div>
            `;
            this.deletedLinkListTarget.appendChild(div);
        });
    }

    async restoreAttachment(event) {
        const uuid = event.currentTarget.dataset.uuid;
        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/attachments/${uuid}/restore`, {
                method: 'POST',
                credentials: 'include'
            });
            if (res.ok) {
                await this.refreshTaskData();
                window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
            }
        } catch (e) { console.error(e); }
    }

    async removeAttachmentPermanent(event) {
        const uuid = event.currentTarget.dataset.uuid;
        this.showConfirm(
            "Suppression définitive ?", 
            "Cette action est irréversible. Le fichier sera définitivement supprimé.",
            async () => {
                try {
                    const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/attachments/${uuid}?permanent=true`, {
                        method: 'DELETE',
                        credentials: 'include'
                    });
                    if (res.ok) {
                        await this.refreshTaskData();
                        window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
                    }
                } catch (e) { console.error(e); }
            }
        );
    }

    async restoreLink(event) {
        const uuid = event.currentTarget.dataset.uuid;
        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/links/${uuid}/restore`, {
                method: 'POST',
                credentials: 'include'
            });
            if (res.ok) {
                await this.refreshTaskData();
                window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
            }
        } catch (e) { console.error(e); }
    }

    async removeLinkPermanent(event) {
        const uuid = event.currentTarget.dataset.uuid;
        this.showConfirm(
            "Suppression définitive ?", 
            "Cette action est irréversible. Le lien sera définitivement supprimé.",
            async () => {
                try {
                    const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/links/${uuid}?permanent=true`, {
                        method: 'DELETE',
                        credentials: 'include'
                    });
                    if (res.ok) {
                        await this.refreshTaskData();
                        window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
                    }
                } catch (e) { console.error(e); }
            }
        );
    }

    async removeAttachment(event) {
        const attachmentUuid = event.currentTarget.dataset.attachmentUuid;
        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/attachments/${attachmentUuid}`, {
                method: 'DELETE',
                credentials: 'include'
            });
            if (res.ok) {
                await this.refreshTaskData();
                window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
            }
        } catch (e) { console.error(e); }
    }

    async renderAttachments(perms = null) {
        if (!this.taskIdValue) return;
        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/attachments`, { credentials: 'include' });
            if (res.ok) {
                const attachments = await res.json();
                this.attachmentListTarget.innerHTML = '';
                
                const formatSize = (bytes) => {
                    if (!bytes) return '0 B';
                    const k = 1024;
                    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
                    const i = Math.floor(Math.log(bytes) / Math.log(k));
                    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
                };

                attachments.forEach(a => {
                    const isResourceOwner = (a.uploadedBy.uuid === this.currentUserUuidValue);
                    const canDelete = this.hasPerm('ATTACHMENT_DELETE', isResourceOwner);

                    const sizeNum = parseInt(a.fileSize);
                    const sizeInMB = sizeNum / (1024 * 1024);
                    const isVeryLarge = sizeInMB >= 500;
                    const isLarge = sizeInMB >= 100;
                    
                    const downloadUrl = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/attachments/${a.uuid}/download`;
                    
                    const div = document.createElement('div');
                    div.className = `p-4 bg-white border border-gray-100 rounded-2xl flex items-center justify-between group hover:border-[var(--highlight-color)]/30 transition-all ${isLarge ? 'md:col-span-2 py-6' : ''}`;
                    
                    div.innerHTML = `
                        <div class="flex items-center gap-3 min-w-0">
                            <div class="p-2 bg-gray-50 rounded-lg text-gray-400 group-hover:text-[var(--highlight-color)] transition-colors ${isVeryLarge ? 'bg-rose-50 text-rose-500' : ''}">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path></svg>
                            </div>
                            <div class="flex flex-col min-w-0">
                                <div class="flex items-center gap-2">
                                    <a href="${downloadUrl}" target="_blank" class="text-xs font-bold text-gray-900 truncate max-w-[250px] hover:text-[var(--highlight-color)] transition-colors">${a.fileName}</a>
                                    ${isVeryLarge ? '<span class="px-1.5 py-0.5 bg-rose-50 text-rose-500 text-[8px] font-black uppercase rounded">Lourd</span>' : ''}
                                </div>
                                <div class="flex items-center gap-2">
                                    <span class="text-[9px] text-gray-400 font-medium">${formatSize(sizeNum)}</span>
                                    <span class="text-[9px] text-gray-300">•</span>
                                    <span class="text-[9px] text-gray-400 font-medium">${new Date(a.createdAt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })}</span>
                                </div>
                            </div>
                        </div>
                        ${canDelete ? `
                            <button type="button" data-action="click->task-modal#removeAttachment" data-attachment-uuid="${a.uuid}" class="p-2 text-gray-300 hover:text-rose-500 transition-colors opacity-0 group-hover:opacity-100">
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                            </button>
                        ` : ''}
                    `;
                    this.attachmentListTarget.appendChild(div);
                });
            }
        } catch (e) { console.error(e); }
    }

    // --- MAIN SAVE ---
    async save(event) {
        event.preventDefault();
        const isEdit = this.taskIdValue !== "";
        
        const data = {};
        const inputs = this.formTarget.querySelectorAll('input:not(:disabled), textarea:not(:disabled), select:not(:disabled)');
        
        inputs.forEach(input => {
            if (input.name) {
                let currentVal;
                if (input.type === 'radio') {
                    if (input.checked) currentVal = input.value;
                    else return;
                } else if (input.type === 'number') {
                    currentVal = input.value === '' ? null : Number(input.value);
                } else {
                    currentVal = input.value;
                }

                if (isEdit) {
                    // Only send if changed
                    if (currentVal !== this.initialState[input.name]) {
                        data[input.name] = currentVal;
                    }
                } else {
                    data[input.name] = currentVal;
                }
            }
        });

        // Combine date and time fields
        const combine = (field) => {
            const dateVal = this.formTarget[`${field}_date`].value;
            const timeVal = this.formTarget[`${field}_time`].value || (field === 'startDate' ? '09:00' : '18:00');
            return dateVal ? `${dateVal}T${timeVal}` : null;
        };

        if (isEdit) {
            if (data.startDate_date !== undefined || data.startDate_time !== undefined) {
                data.startDate = combine('startDate');
            }
            if (data.expiresAt_date !== undefined || data.expiresAt_time !== undefined) {
                data.expiresAt = combine('expiresAt');
            }
            delete data.startDate_date; delete data.startDate_time;
            delete data.expiresAt_date; delete data.expiresAt_time;
        } else {
            data.startDate = combine('startDate');
            data.expiresAt = combine('expiresAt');
            delete data.startDate_date; delete data.startDate_time;
            delete data.expiresAt_date; delete data.expiresAt_time;
        }

        // If nothing changed in edit mode, just close and return
        if (isEdit && Object.keys(data).length === 0) {
            this.close();
            return;
        }

        if (!isEdit) {
            data.assigneeUuids = this.stagedAssignees.map(a => a.uuid);
            data.tagUuids = this.stagedTags.map(t => t.uuid);
            data.dependencyUuids = this.stagedDependencies.map(d => d.uuid);
            data.linkData = this.stagedLinks;
        }

        const url = isEdit 
            ? `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}`
            : `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks`;
        
        const method = isEdit ? 'PATCH' : 'POST';

        this.submitBtnTarget.disabled = true;
        this.submitBtnTarget.classList.add('opacity-50', 'cursor-not-allowed');

        try {
            const response = await fetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data),
                credentials: 'include'
            });

            if (response.ok) {
                const result = await response.json();
                if (!isEdit) {
                    this.taskIdValue = result.uuid;
                    this.modalTitleTarget.innerText = trans('task.modal.edit_title');
                    this.submitBtnTextTarget.innerText = trans('task.modal.buttons.save');
                    this.subResourcesTarget.classList.remove('hidden');
                    this.containerTarget.querySelectorAll('[data-section="attachments"], [data-section="comments"], [data-section="timeline"]').forEach(s => s.classList.remove('hidden'));
                    this.taskMetaTarget.classList.remove('hidden');
                    this.deleteBtnTarget.classList.remove('hidden');
                    await this.refreshTaskData();
                } else {
                    this.close();
                }
                window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
            } else {
                const error = await response.json();
                let message = error.message || trans('task.modal.error.save_generic');
                if (response.status === 403) message = trans('task.modal.error.access_denied');
                this.showError(trans('task.modal.error.save_title'), message);
            }
        } catch (e) {
            console.error("Save failed", e);
        } finally {
            this.submitBtnTarget.disabled = false;
            this.submitBtnTarget.classList.remove('opacity-50', 'cursor-not-allowed');
        }
    }

    async deleteTask() {
        const isPermanent = this.isTaskTrashed === true;
        const title = isPermanent ? trans('task.modal.confirm.delete_permanent_title') : trans('task.modal.confirm.delete_title');
        const message = isPermanent 
            ? trans('task.modal.confirm.delete_permanent_message')
            : trans('task.modal.confirm.delete_message');

        this.showConfirm(
            title,
            message,
            async () => {
                try {
                    const url = `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}${isPermanent ? '?permanent=true' : ''}`;
                    const res = await fetch(url, {
                        method: 'DELETE',
                        credentials: 'include'
                    });
                    if (res.ok) {
                        this.close();
                        window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
                        // If we were in trash view, we might need a specific event or just rely on task-saved if the trash view listens to it
                    }
                } catch (e) { console.error(e); }
            }
        );
    }

    async restoreTask() {
        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${this.organUuidValue}/tasks/${this.taskIdValue}/restore`, {
                method: 'POST',
                credentials: 'include'
            });
            if (res.ok) {
                this.close();
                window.dispatchEvent(new CustomEvent('task-saved', { detail: { organUuid: this.organUuidValue } }));
            }
        } catch (e) { console.error(e); }
    }
}
