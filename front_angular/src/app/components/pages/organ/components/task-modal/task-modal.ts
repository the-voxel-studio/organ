import { Component, OnInit, OnDestroy, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../../../../services/api/auth.service';

import { TaskResponse, TaskStatus, TaskTagSummary, TaskLinkSummary, TaskPermissionsResponse, TaskTimelineItem, UpdateTaskRequest, CreateTaskRequest } from '../../../../../models/task.model';
import { TaskCommentResponse } from '../../../../../models/task-comment.model';
import { TaskAttachmentResponse } from '../../../../../models/task-attachment.model';
import { OrganMember } from '../../../../../models/organ.model';
import { TagResponse } from '../../../../../models/tag.model';

import { TaskBasicInfoComponent } from './components/task-basic-info/task-basic-info';
import { TaskCommentsComponent } from './components/task-comments/task-comments';
import { TaskAttachmentsComponent } from './components/task-attachments/task-attachments';
import { TaskLinksComponent } from './components/task-links/task-links';
import { TaskTimelineComponent } from './components/task-timeline/task-timeline';
import { TaskTrashTabComponent } from './components/task-trash-tab/task-trash-tab';
import { TaskModalStore } from './services/task-modal.store';

@Component({
  selector: 'app-task-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TaskBasicInfoComponent,
    TaskCommentsComponent,
    TaskAttachmentsComponent,
    TaskLinksComponent,
    TaskTimelineComponent,
    TaskTrashTabComponent
  ],
  providers: [TaskModalStore],
  templateUrl: './task-modal.html'
})
export class TaskModalComponent implements OnInit, OnDestroy {
  // Entrées
  @Input() projectUuid!: string;
  @Input() organUuid!: string;
  @Input() highlightColor = '#FF7DD4';

  // Sorties
  @Output() taskSaved = new EventEmitter<void>();

  // Inject Services & Store
  public store = inject(TaskModalStore);
  public authService = inject(AuthService);
  private fb = inject(FormBuilder);

  // Form Group
  taskForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(4000)]],
    status: ['TODO' as TaskStatus],
    priority: [1, [Validators.min(1), Validators.max(10)]],
    statusMessage: [''],
    startDate_date: [''],
    startDate_time: [''],
    expiresAt_date: [''],
    expiresAt_time: [''],
    managerUuid: [''],
    estimatedHours: ['']
  });

  initialStatus: TaskStatus = 'TODO';

  // Modal de confirmation
  showConfirmModal = signal(false);
  confirmTitle = '';
  confirmMessage = '';
  confirmCallback: (() => void) | null = null;

  // Modal d'erreur
  showErrorModal = signal(false);
  errorTitle = '';
  errorMessage = '';

  private beforeUnloadHandler = (e: BeforeUnloadEvent) => {
    if (this.isUploading()) {
      e.preventDefault();
    }
  };

  // Redirections vers le Store (Maintien de l'API publique pour le template HTML)
  isOpen = this.store.isOpen;
  backdropVisible = this.store.backdropVisible;
  panelVisible = this.store.panelVisible;
  isLoading = this.store.isLoading;
  isUploading = this.store.isUploading;
  isTrashOpen = this.store.isTrashOpen;
  taskId = this.store.taskId;
  isTaskTrashed = this.store.isTaskTrashed;
  hasMoreTimeline = this.store.hasMoreTimeline;

  get taskData(): TaskResponse | null { return this.store.taskData(); }
  get currentPerms(): TaskPermissionsResponse | null { return this.store.currentPerms(); }
  get editableFields(): string[] { return this.store.editableFields(); }

  get members(): OrganMember[] { return this.store.members(); }
  get projectTags(): TagResponse[] { return this.store.projectTags(); }
  get organTasks(): TaskResponse[] { return this.store.organTasks(); }

  get assignees(): any[] { return this.store.assignees(); }
  get tags(): TaskTagSummary[] { return this.store.tags(); }
  get links(): TaskLinkSummary[] { return this.store.links(); }
  get dependencies(): any[] { return this.store.dependencies(); }
  get attachments(): TaskAttachmentResponse[] { return this.store.attachments(); }
  get comments(): TaskCommentResponse[] { return this.store.comments(); }
  get timeline(): TaskTimelineItem[] { return this.store.timeline(); }

  get deletedAttachments(): TaskAttachmentResponse[] { return this.store.deletedAttachments(); }
  get deletedLinks(): TaskLinkSummary[] { return this.store.deletedLinks(); }
  get deletedComments(): TaskCommentResponse[] { return this.store.deletedComments(); }

  get stagedAssignees(): OrganMember[] { return this.store.stagedAssignees(); }
  get stagedTags(): TagResponse[] { return this.store.stagedTags(); }
  get stagedLinks() { return this.store.stagedLinks(); }
  get stagedDependencies(): TaskResponse[] { return this.store.stagedDependencies(); }

  // Mappage getters/setters vers les contrôleurs du formulaire
  get title(): string { return this.taskForm.get('title')?.value || ''; }
  set title(val: string) { this.taskForm.get('title')?.setValue(val); }

  get description(): string { return this.taskForm.get('description')?.value || ''; }
  set description(val: string) { this.taskForm.get('description')?.setValue(val); }

  get status(): TaskStatus { return this.taskForm.get('status')?.value as TaskStatus || 'TODO'; }
  set status(val: TaskStatus) { this.taskForm.get('status')?.setValue(val); }

  get priority(): number { return this.taskForm.get('priority')?.value || 1; }
  set priority(val: number) { this.taskForm.get('priority')?.setValue(val); }

  get statusMessage(): string { return this.taskForm.get('statusMessage')?.value || ''; }
  set statusMessage(val: string) { this.taskForm.get('statusMessage')?.setValue(val); }

  get startDate_date(): string { return this.taskForm.get('startDate_date')?.value || ''; }
  set startDate_date(val: string) { this.taskForm.get('startDate_date')?.setValue(val); }

  get startDate_time(): string { return this.taskForm.get('startDate_time')?.value || ''; }
  set startDate_time(val: string) { this.taskForm.get('startDate_time')?.setValue(val); }

  get expiresAt_date(): string { return this.taskForm.get('expiresAt_date')?.value || ''; }
  set expiresAt_date(val: string) { this.taskForm.get('expiresAt_date')?.setValue(val); }

  get expiresAt_time(): string { return this.taskForm.get('expiresAt_time')?.value || ''; }
  set expiresAt_time(val: string) { this.taskForm.get('expiresAt_time')?.setValue(val); }

  get managerUuid(): string { return this.taskForm.get('managerUuid')?.value || ''; }
  set managerUuid(val: string) { this.taskForm.get('managerUuid')?.setValue(val); }

  get estimatedHours(): string { return this.taskForm.get('estimatedHours')?.value || ''; }
  set estimatedHours(val: string) { this.taskForm.get('estimatedHours')?.setValue(val); }

  ngOnInit() {
    this.store.loadInitialStaticData(this.projectUuid, this.organUuid);
    window.addEventListener('beforeunload', this.beforeUnloadHandler);
  }

  ngOnDestroy() {
    window.removeEventListener('beforeunload', this.beforeUnloadHandler);
  }

  openForCreate() {
    const stagedPerms = {
      permissions: ['ALL'],
      editableFields: ['title', 'description', 'priority', 'status', 'expiresAt', 'manager'],
      isProjectAdmin: true,
      taskOwnership: { isManager: true, isAssignee: true, isCreator: true }
    };
    this.resetForm();
    this.initialStatus = 'TODO';
    this.store.openForCreate(stagedPerms);
  }

  openForEdit(taskId: string, isTrash = false) {
    this.resetForm();
    this.store.openForEdit(taskId, isTrash);
    this.refreshTaskData();
  }

  close() {
    if (this.isUploading()) {
      this.showConfirm(
        'Annuler le transfert ?',
        'Un transfert est en cours. Si vous fermez cette modale, le transfert sera annulé. Voulez-vous continuer ?',
        () => {
          this.isUploading.set(false);
          this.store.closeModalFlow(() => {});
        }
      );
      return;
    }
    this.store.closeModalFlow(() => {});
  }

  resetForm() {
    this.taskForm.reset({
      title: '',
      description: '',
      status: 'TODO',
      priority: 1,
      statusMessage: '',
      startDate_date: '',
      startDate_time: '',
      expiresAt_date: '',
      expiresAt_time: '',
      managerUuid: '',
      estimatedHours: ''
    });
    this.store.reset();
  }

  refreshTaskData() {
    this.store.refreshTaskData(
      (task) => this.patchForm(task),
      (err) => {
        console.error('Failed to load task details', err);
        this.showError('Erreur de chargement', 'Impossible de récupérer les détails de la tâche.');
        this.close();
      }
    );
  }

  private patchForm(task: TaskResponse) {
    this.taskForm.patchValue({
      title: task.title,
      description: task.description || '',
      status: task.status,
      priority: task.priority,
      statusMessage: task.statusMessage || '',
      managerUuid: task.manager?.uuid || '',
      estimatedHours: task.estimatedHours || ''
    });
    this.initialStatus = task.status;

    // Dates formatting
    if (task.startDate) {
      const d = new Date(task.startDate);
      this.startDate_date = d.toISOString().substring(0, 10);
      this.startDate_time = d.toTimeString().substring(0, 5);
    } else {
      this.startDate_date = '';
      this.startDate_time = '';
    }
    if (task.expiresAt) {
      const d = new Date(task.expiresAt);
      this.expiresAt_date = d.toISOString().substring(0, 10);
      this.expiresAt_time = d.toTimeString().substring(0, 5);
    } else {
      this.expiresAt_date = '';
      this.expiresAt_time = '';
    }
  }

  // Vérifications des permissions
  hasPerm(permBaseName: string, isOwner = false): boolean {
    if (!this.currentPerms) return false;
    const p = this.currentPerms.permissions || [];
    if (this.currentPerms.isProjectAdmin || p.includes('ALL')) return true;
    if (p.includes(permBaseName)) return true;
    if (p.includes(`${permBaseName}_ALL`)) return true;
    if (isOwner && p.includes(`${permBaseName}_OWN`)) return true;
    return false;
  }

  canEditField(fieldName: string): boolean {
    if (this.isTaskTrashed()) return false;
    let check = fieldName;
    if (fieldName === 'startDate' || fieldName === 'expiresAt') check = 'expiresAt';
    if (fieldName === 'managerUuid') check = 'manager';
    return this.editableFields.includes(check);
  }

  isTaskOwner(): boolean {
    return !!(this.currentPerms?.taskOwnership?.isManager || this.currentPerms?.taskOwnership?.isCreator);
  }

  isStatusMessageVisible(): boolean {
    return this.status !== this.initialStatus;
  }

  // Task creation/edition submission
  saveTask() {
    if (this.taskForm.invalid) {
      this.showError('Champs requis', 'Veuillez saisir un titre valide pour la tâche.');
      return;
    }

    const startDateStr = this.startDate_date ? `${this.startDate_date}T${this.startDate_time || '00:00'}:00` : null;
    const expiresAtStr = this.expiresAt_date ? `${this.expiresAt_date}T${this.expiresAt_time || '00:00'}:00` : null;

    if (this.taskId()) {
      const req: UpdateTaskRequest = {
        title: this.title,
        description: this.description,
        priority: this.priority,
        status: this.status,
        statusMessage: this.isStatusMessageVisible() ? this.statusMessage : undefined,
        startDate: startDateStr,
        expiresAt: expiresAtStr,
        managerUuid: this.managerUuid || null,
        estimatedHours: this.estimatedHours || undefined
      };

      this.store.saveTask(
        req,
        true,
        () => {
          this.taskSaved.emit();
          this.close();
        },
        (err) => {
          console.error('Failed to update task', err);
          const msg = err?.error?.message || 'Erreur lors de l\'enregistrement de la tâche.';
          this.showError('Erreur de sauvegarde', msg);
        }
      );
    } else {
      const req: CreateTaskRequest = {
        title: this.title,
        description: this.description || undefined,
        priority: this.priority,
        status: this.status,
        statusMessage: this.statusMessage || undefined,
        startDate: startDateStr || undefined,
        expiresAt: expiresAtStr || undefined,
        managerUuid: this.managerUuid || undefined,
        estimatedHours: this.estimatedHours || undefined,
        assigneeUuids: this.stagedAssignees.map(a => a.uuid),
        tagUuids: this.stagedTags.map(t => t.uuid),
        dependencyUuids: this.stagedDependencies.map(d => d.uuid),
        linkData: this.stagedLinks
      };

      this.store.saveTask(
        req,
        false,
        () => {
          this.taskSaved.emit();
          this.close();
        },
        (err) => {
          console.error('Failed to create task', err);
          const msg = err?.error?.message || 'Erreur lors de la création de la tâche.';
          this.showError('Erreur de création', msg);
        }
      );
    }
  }

  deleteTask() {
    const isOwner = this.isTaskOwner();
    const canHardDelete = this.isTaskTrashed() ? this.hasPerm('TASK_HARD_DELETE', isOwner) : false;
    const canSoftDelete = !this.isTaskTrashed() && this.hasPerm('TASK_DELETE', isOwner);

    if (this.isTaskTrashed()) {
      if (!canHardDelete) return;
      this.showConfirm(
        'Supprimer définitivement ?',
        'Cette action est irréversible. La tâche et toutes ses données associées seront définitivement perdues.',
        () => {
          this.store.deleteTask(
            true,
            () => {
              this.taskSaved.emit();
              this.close();
            },
            (err) => this.showError('Erreur', err?.error?.message || 'Erreur lors de la suppression définitive.')
          );
        }
      );
    } else {
      if (!canSoftDelete) return;
      this.showConfirm(
        'Supprimer la tâche ?',
        'La tâche sera déplacée vers la corbeille de l\'Organ.',
        () => {
          this.store.deleteTask(
            false,
            () => {
              this.taskSaved.emit();
              this.close();
            },
            (err) => this.showError('Erreur', err?.error?.message || 'Erreur lors de la suppression.')
          );
        }
      );
    }
  }

  restoreTask() {
    this.store.restoreTask(
      () => {
        this.taskSaved.emit();
        this.close();
      },
      (err) => this.showError('Erreur', err?.error?.message || 'Erreur lors de la restauration de la tâche.')
    );
  }

  // --- ASSIGNEES ---
  addAssignee(member: OrganMember) {
    this.store.addAssignee(member, (t) => this.patchForm(t));
  }

  removeAssignee(memberUuid: string) {
    this.store.removeAssignee(memberUuid, (t) => this.patchForm(t));
  }

  // --- TAGS ---
  addTag(tag: TagResponse) {
    this.store.addTag(tag, (t) => this.patchForm(t), () => this.taskSaved.emit());
  }

  removeTag(tagUuid: string) {
    this.store.removeTag(tagUuid, (t) => this.patchForm(t), () => this.taskSaved.emit());
  }

  // --- LINKS ---
  addLink(payload: { url: string; description?: string }) {
    this.store.addLink(payload, (t) => this.patchForm(t), () => this.taskSaved.emit());
  }

  removeLink(linkUuid: string) {
    this.store.removeLink(linkUuid, (t) => this.patchForm(t), () => this.taskSaved.emit());
  }

  // --- DEPENDENCIES ---
  addDependency(task: TaskResponse) {
    this.store.addDependency(task, (t) => this.patchForm(t));
  }

  removeDependency(depUuid: string) {
    this.store.removeDependency(depUuid, (t) => this.patchForm(t));
  }

  // --- COMMENTAIRES ---
  addComment(content: string) {
    this.store.addComment(content, (t) => this.patchForm(t), () => this.taskSaved.emit());
  }

  deleteComment(commentUuid: string) {
    this.store.deleteComment(commentUuid, (t) => this.patchForm(t));
  }

  // --- CORBEILLE ET RESTAURATIONS ---
  toggleTrash() {
    this.store.isTrashOpen.update(v => !v);
    if (this.store.isTrashOpen()) {
      this.store.loadTrashData();
    }
  }

  restoreComment(commentUuid: string) {
    this.store.restoreComment(commentUuid, (t) => this.patchForm(t));
  }

  restoreAttachment(attachmentUuid: string) {
    this.store.restoreAttachment(attachmentUuid, (t) => this.patchForm(t));
  }

  loadTimeline(append = false) {
    this.store.loadTimeline(append);
  }


  hardDeleteComment(commentUuid: string) {
    this.showConfirm(
      'Supprimer définitivement ?',
      'Ce commentaire sera supprimé sans retour possible.',
      () => {
        this.store.hardDeleteComment(commentUuid, (t) => this.patchForm(t));
      }
    );
  }

  hardDeleteAttachment(attachmentUuid: string) {
    this.showConfirm(
      'Supprimer définitivement ?',
      'Cette pièce jointe sera définitivement supprimée du serveur.',
      () => {
        this.store.hardDeleteAttachment(attachmentUuid, (t) => this.patchForm(t));
      }
    );
  }

  // Helpers de confirmation
  showConfirm(title: string, message: string, callback: () => void) {
    this.confirmTitle = title;
    this.confirmMessage = message;
    this.confirmCallback = callback;
    this.showConfirmModal.set(true);
  }

  cancelConfirm() {
    this.showConfirmModal.set(false);
    this.confirmCallback = null;
  }

  executeConfirm() {
    if (this.confirmCallback) {
      this.confirmCallback();
    }
    this.showConfirmModal.set(false);
    this.confirmCallback = null;
  }

  // Helpers d'erreur
  showError(title: string, message: string) {
    this.errorTitle = title;
    this.errorMessage = message;
    this.showErrorModal.set(true);
  }

  hideError() {
    this.showErrorModal.set(false);
  }

  formatDateString(dateStr: string | null | undefined): string {
    if (!dateStr) return '-';
    try {
      const date = new Date(dateStr);
      return date.toLocaleString('fr-FR', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateStr;
    }
  }

  getPriorityLabel(priority: number): string {
    if (priority <= 3) return 'Faible';
    if (priority <= 7) return 'Moyenne';
    return 'Urgent';
  }
}
