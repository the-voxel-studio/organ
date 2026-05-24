import { Component, OnInit, OnDestroy, Input, Output, EventEmitter, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { TaskService } from '../../../../../services/task.service';
import { TaskCommentService } from '../../../../../services/task-comment.service';
import { TaskAttachmentService } from '../../../../../services/task-attachment.service';
import { TaskLinkService } from '../../../../../services/task-link.service';
import { TaskDependencyService } from '../../../../../services/task-dependency.service';
import { TagService } from '../../../../../services/tag.service';
import { AuthService } from '../../../../../services/auth.service';
import { OrganService } from '../../../../../services/organ.service';

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
  templateUrl: './task-modal.html'
})
export class TaskModalComponent implements OnInit, OnDestroy {
  // Inputs
  @Input() projectUuid!: string;
  @Input() organUuid!: string;
  @Input() highlightColor = '#FF7DD4';

  // Outputs
  @Output() taskSaved = new EventEmitter<void>();

  // Inject Services
  private taskService = inject(TaskService);
  private commentService = inject(TaskCommentService);
  private attachmentService = inject(TaskAttachmentService);
  private linkService = inject(TaskLinkService);
  private dependencyService = inject(TaskDependencyService);
  private organService = inject(OrganService);
  private tagService = inject(TagService);
  public authService = inject(AuthService);
  private fb = inject(FormBuilder);

  // Visibility states
  isOpen = signal(false);
  backdropVisible = signal(false);
  panelVisible = signal(false);
  isLoading = signal(false);
  isUploading = signal(false);
  isTrashOpen = signal(false);

  // Form states
  taskId = signal<string | null>(null);
  isTaskTrashed = signal(false);
  initialStatus: TaskStatus = 'TODO';
  
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

  // Getters/setters mapping back to form controllers
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

  // Dropdown list data
  members: OrganMember[] = [];
  projectTags: TagResponse[] = [];
  organTasks: TaskResponse[] = []; // for dependency picker

  // Permissions state
  currentPerms: TaskPermissionsResponse | null = null;
  editableFields: string[] = [];

  // Active sub-resources
  taskData: TaskResponse | null = null;
  assignees: any[] = [];
  tags: TaskTagSummary[] = [];
  links: TaskLinkSummary[] = [];
  dependencies: any[] = [];
  attachments: TaskAttachmentResponse[] = [];
  comments: TaskCommentResponse[] = [];
  timeline: TaskTimelineItem[] = [];

  // Trashed sub-resources
  deletedAttachments: TaskAttachmentResponse[] = [];
  deletedLinks: TaskLinkSummary[] = [];
  deletedComments: TaskCommentResponse[] = [];



  // Timeline pagination
  timelineOffset = 0;
  timelineLimit = 10;
  hasMoreTimeline = signal(false);

  // Staged data for creation mode
  stagedAssignees: OrganMember[] = [];
  stagedTags: TagResponse[] = [];
  stagedLinks: Array<{ url: string; description?: string }> = [];
  stagedDependencies: TaskResponse[] = [];

  // Confirmation Modal
  showConfirmModal = signal(false);
  confirmTitle = '';
  confirmMessage = '';
  confirmCallback: (() => void) | null = null;

  // Error Modal
  showErrorModal = signal(false);
  errorTitle = '';
  errorMessage = '';

  private tagsChangedSubscription: any;
  private beforeUnloadHandler = (e: BeforeUnloadEvent) => {
    if (this.isUploading()) {
      e.preventDefault();
    }
  };

  ngOnInit() {
    this.loadInitialData();
    window.addEventListener('beforeunload', this.beforeUnloadHandler);
  }

  ngOnDestroy() {
    window.removeEventListener('beforeunload', this.beforeUnloadHandler);
  }

  loadInitialData() {
    // Load members of organ and tags of project
    this.organService.getOrganMembers(this.projectUuid, this.organUuid).subscribe({
      next: (members: OrganMember[]) => {
        this.members = members;
      },
      error: (err: any) => console.error('Failed to load organ members', err)
    });

    this.tagService.getTags(this.projectUuid).subscribe({
      next: (tags: TagResponse[]) => {
        this.projectTags = tags;
      },
      error: (err: any) => console.error('Failed to load project tags', err)
    });
  }

  loadOrganTasks() {
    this.taskService.getTasks(this.projectUuid, this.organUuid).subscribe({
      next: (tasks: TaskResponse[]) => {
        // Exclude current task
        this.organTasks = tasks.filter(t => t.uuid !== this.taskId());
      },
      error: (err: any) => console.error('Failed to load tasks for dependencies', err)
    });
  }

  openForCreate() {
    this.taskId.set(null);
    this.isTaskTrashed.set(false);
    this.isTrashOpen.set(false);
    this.resetForm();
    this.initialStatus = 'TODO';
    
    // Staged creation lists
    this.stagedAssignees = [];
    this.stagedTags = [];
    this.stagedLinks = [];
    this.stagedDependencies = [];

    // Mock permissions for creation
    this.currentPerms = {
      permissions: ['ALL'],
      editableFields: ['title', 'description', 'priority', 'status', 'expiresAt', 'manager'],
      isProjectAdmin: true,
      taskOwnership: { isManager: true, isAssignee: true, isCreator: true }
    };
    this.editableFields = this.currentPerms.editableFields;

    this.assignees = [];
    this.tags = [];
    this.links = [];
    this.dependencies = [];
    
    this.isOpen.set(true);
    this.backdropVisible.set(true);
    this.panelVisible.set(true);
  }

  openForEdit(taskId: string, isTrash = false) {
    this.taskId.set(taskId);
    this.isTaskTrashed.set(isTrash);
    this.isTrashOpen.set(false);
    this.resetForm();

    this.isOpen.set(true);
    this.backdropVisible.set(true);
    this.panelVisible.set(true);
    this.isLoading.set(true);

    this.refreshTaskData();
    this.loadOrganTasks();
  }

  close() {
    if (this.isUploading()) {
      this.showConfirm(
        'Annuler le transfert ?',
        'Un transfert est en cours. Si vous fermez cette modale, le transfert sera annulé. Voulez-vous continuer ?',
        () => {
          this.isUploading.set(false);
          this.closeModalFlow();
        }
      );
      return;
    }
    this.closeModalFlow();
  }

  private closeModalFlow() {
    this.panelVisible.set(false);
    this.backdropVisible.set(false);
    setTimeout(() => {
      this.isOpen.set(false);
    }, 300);
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
    this.taskData = null;
    this.attachments = [];
    this.comments = [];
    this.timeline = [];
    this.dependencies = [];
    this.currentPerms = null;
    this.editableFields = [];
    this.timelineOffset = 0;
    this.hasMoreTimeline.set(false);
  }

  refreshTaskData() {
    const tId = this.taskId();
    if (!tId) return;

    this.taskService.getTask(this.projectUuid, this.organUuid, tId, this.isTaskTrashed()).subscribe({
      next: (task) => {
        this.taskData = task;
        this.title = task.title;
        this.description = task.description || '';
        this.status = task.status;
        this.initialStatus = task.status;
        this.priority = task.priority;
        this.statusMessage = task.statusMessage || '';
        this.managerUuid = task.manager?.uuid || '';
        this.estimatedHours = task.estimatedHours || '';

        // Dates formatting
        if (task.startDate) {
          const d = new Date(task.startDate);
          this.startDate_date = d.toISOString().substring(0, 10);
          this.startDate_time = d.toTimeString().substring(0, 5);
        }
        if (task.expiresAt) {
          const d = new Date(task.expiresAt);
          this.expiresAt_date = d.toISOString().substring(0, 10);
          this.expiresAt_time = d.toTimeString().substring(0, 5);
        }

        this.assignees = task.assignees;
        this.tags = task.tags;
        this.links = task.links;

        // Fetch remaining resources
        this.loadTaskPermissions();
        this.loadComments();
        this.loadAttachments();
        this.loadTimeline();
        this.loadDependencies();

        if (this.isTrashOpen()) {
          this.loadTrashData();
        }

        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load task details', err);
        this.showError('Erreur de chargement', 'Impossible de récupérer les détails de la tâche.');
        this.isLoading.set(false);
        this.close();
      }
    });
  }

  loadTaskPermissions() {
    this.taskService.getTaskPermissions(this.projectUuid, this.organUuid, this.taskId()!).subscribe({
      next: (perms) => {
        this.currentPerms = perms;
        this.editableFields = perms.editableFields || [];
      },
      error: (err) => console.error('Failed to load task permissions', err)
    });
  }

  loadComments() {
    this.commentService.getComments(this.projectUuid, this.organUuid, this.taskId()!).subscribe({
      next: (comments) => this.comments = comments,
      error: (err) => console.error('Failed to load comments', err)
    });
  }

  loadAttachments() {
    this.attachmentService.getAttachments(this.projectUuid, this.organUuid, this.taskId()!).subscribe({
      next: (attachments) => this.attachments = attachments,
      error: (err) => console.error('Failed to load attachments', err)
    });
  }

  loadDependencies() {
    this.dependencyService.getDependencies(this.projectUuid, this.organUuid, this.taskId()!).subscribe({
      next: (deps) => this.dependencies = deps.map(d => ({
        uuid: d.dependsOnTaskUuid,
        title: d.title
      })),
      error: (err) => console.error('Failed to load dependencies', err)
    });
  }

  loadTimeline(append = false) {
    if (!append) {
      this.timelineOffset = 0;
      this.timeline = [];
    }
    this.taskService.getTaskTimeline(this.projectUuid, this.organUuid, this.taskId()!, this.timelineOffset, this.timelineLimit).subscribe({
      next: (items: TaskTimelineItem[]) => {
        const actionItems = items.filter(item => item.actionType !== 'CONSULTATION');
        if (append) {
          this.timeline = [...this.timeline, ...actionItems];
        } else {
          this.timeline = actionItems;
        }
        this.timelineOffset += this.timelineLimit;
        this.hasMoreTimeline.set(items.length === this.timelineLimit);
      },
      error: (err: any) => console.error('Failed to load timeline', err)
    });
  }

  loadTrashData() {
    const tId = this.taskId();
    if (!tId) return;

    this.commentService.getTrashedComments(this.projectUuid, this.organUuid, tId).subscribe({
      next: (comments) => this.deletedComments = comments,
      error: (err) => console.error('Failed to load trashed comments', err)
    });

    this.attachmentService.getTrashedAttachments(this.projectUuid, this.organUuid, tId).subscribe({
      next: (attachments) => this.deletedAttachments = attachments,
      error: (err) => console.error('Failed to load trashed attachments', err)
    });

    this.linkService.getTrashedLinks(this.projectUuid, this.organUuid, tId).subscribe({
      next: (links) => this.deletedLinks = links,
      error: (err: any) => console.error('Failed to load trashed links', err)
    });
  }

  // Permission checkers
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
      // Edit mode
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

      this.taskService.updateTask(this.projectUuid, this.organUuid, this.taskId()!, req).subscribe({
        next: () => {
          this.taskSaved.emit();
          this.close();
        },
        error: (err) => {
          console.error('Failed to update task', err);
          const msg = err?.error?.message || 'Erreur lors de l\'enregistrement de la tâche.';
          this.showError('Erreur de sauvegarde', msg);
        }
      });
    } else {
      // Create mode
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

      this.taskService.createTask(this.projectUuid, this.organUuid, req).subscribe({
        next: () => {
          this.taskSaved.emit();
          this.close();
        },
        error: (err) => {
          console.error('Failed to create task', err);
          const msg = err?.error?.message || 'Erreur lors de la création de la tâche.';
          this.showError('Erreur de création', msg);
        }
      });
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
          this.taskService.deleteTask(this.projectUuid, this.organUuid, this.taskId()!, true).subscribe({
            next: () => {
              this.taskSaved.emit();
              this.close();
            },
            error: (err) => this.showError('Erreur', err?.error?.message || 'Erreur lors de la suppression définitive.')
          });
        }
      );
    } else {
      if (!canSoftDelete) return;
      this.showConfirm(
        'Supprimer la tâche ?',
        'La tâche sera déplacée vers la corbeille de l\'Organ.',
        () => {
          this.taskService.deleteTask(this.projectUuid, this.organUuid, this.taskId()!, false).subscribe({
            next: () => {
              this.taskSaved.emit();
              this.close();
            },
            error: (err) => this.showError('Erreur', err?.error?.message || 'Erreur lors de la suppression.')
          });
        }
      );
    }
  }

  restoreTask() {
    this.taskService.restoreTask(this.projectUuid, this.organUuid, this.taskId()!).subscribe({
      next: () => {
        this.taskSaved.emit();
        this.close();
      },
      error: (err) => this.showError('Erreur', err?.error?.message || 'Erreur lors de la restauration de la tâche.')
    });
  }

  // --- ASSIGNEES ---
  addAssignee(member: OrganMember) {
    if (!this.taskId()) {
      this.stagedAssignees.push(member);
      this.assignees = [...this.stagedAssignees];
      return;
    }

    this.taskService.addAssignee(this.projectUuid, this.organUuid, this.taskId()!, { userUuid: member.uuid }).subscribe({
      next: () => {
        this.refreshTaskData();
      },
      error: (err) => console.error('Failed to add assignee', err)
    });
  }

  removeAssignee(memberUuid: string) {
    if (!this.taskId()) {
      this.stagedAssignees = this.stagedAssignees.filter(a => a.uuid !== memberUuid);
      this.assignees = [...this.stagedAssignees];
      return;
    }

    this.taskService.removeAssignee(this.projectUuid, this.organUuid, this.taskId()!, memberUuid).subscribe({
      next: () => this.refreshTaskData(),
      error: (err) => console.error('Failed to remove assignee', err)
    });
  }

  // --- TAGS ---
  addTag(tag: TagResponse) {
    if (!this.taskId()) {
      this.stagedTags.push(tag);
      this.tags = [...this.stagedTags];
      return;
    }

    // Call service to attach tag
    this.tagService.addTaskTag(this.projectUuid, this.organUuid, this.taskId()!, { tagUuid: tag.uuid }).subscribe({
      next: () => {
        this.refreshTaskData();
        this.taskSaved.emit();
      },
      error: (err: any) => console.error('Failed to add tag to task', err)
    });
  }

  removeTag(tagUuid: string) {
    if (!this.taskId()) {
      this.stagedTags = this.stagedTags.filter(t => t.uuid !== tagUuid);
      this.tags = [...this.stagedTags];
      return;
    }

    this.tagService.removeTaskTag(this.projectUuid, this.organUuid, this.taskId()!, tagUuid).subscribe({
      next: () => {
        this.refreshTaskData();
        this.taskSaved.emit();
      },
      error: (err: any) => console.error('Failed to remove tag from task', err)
    });
  }

  // --- LINKS ---
  addLink(payload: { url: string; description?: string }) {
    const url = payload.url.trim();
    if (!url) return;

    if (!this.taskId()) {
      this.stagedLinks.push({ url, description: payload.description || undefined });
      // Staged display
      this.links = this.stagedLinks.map((l, i) => ({
        uuid: `staged-${i}`,
        url: l.url,
        description: l.description || null
      }));
      return;
    }

    this.linkService.createLink(this.projectUuid, this.organUuid, this.taskId()!, {
      url,
      description: payload.description || undefined
    }).subscribe({
      next: () => {
        this.refreshTaskData();
        this.taskSaved.emit();
      },
      error: (err) => console.error('Failed to add task link', err)
    });
  }

  removeLink(linkUuid: string) {
    if (!this.taskId()) {
      this.stagedLinks = this.stagedLinks.filter((_, i) => `staged-${i}` !== linkUuid);
      this.links = this.stagedLinks.map((l, i) => ({
        uuid: `staged-${i}`,
        url: l.url,
        description: l.description || null
      }));
      return;
    }

    this.linkService.deleteLink(this.projectUuid, this.organUuid, this.taskId()!, linkUuid, false).subscribe({
      next: () => {
        this.refreshTaskData();
        this.taskSaved.emit();
      },
      error: (err) => console.error('Failed to delete task link', err)
    });
  }

  // --- DEPENDENCIES ---
  addDependency(task: TaskResponse) {
    if (!this.taskId()) {
      this.stagedDependencies.push(task);
      this.dependencies = this.stagedDependencies.map(d => ({
        uuid: d.uuid,
        title: d.title
      }));
      return;
    }

    this.dependencyService.addDependency(this.projectUuid, this.organUuid, this.taskId()!, { dependsOnTaskUuid: task.uuid }).subscribe({
      next: () => {
        this.refreshTaskData();
      },
      error: (err) => console.error('Failed to add dependency', err)
    });
  }

  removeDependency(depUuid: string) {
    if (!this.taskId()) {
      this.stagedDependencies = this.stagedDependencies.filter(d => d.uuid !== depUuid);
      this.dependencies = this.stagedDependencies.map(d => ({
        uuid: d.uuid,
        title: d.title
      }));
      return;
    }

    this.dependencyService.removeDependency(this.projectUuid, this.organUuid, this.taskId()!, depUuid).subscribe({
      next: () => this.refreshTaskData(),
      error: (err) => console.error('Failed to remove dependency', err)
    });
  }

  // --- COMMENTS ---
  addComment(content: string) {
    if (!content.trim()) return;

    this.commentService.createComment(this.projectUuid, this.organUuid, this.taskId()!, { content }).subscribe({
      next: () => {
        this.refreshTaskData();
        this.taskSaved.emit();
      },
      error: (err) => console.error('Failed to add comment', err)
    });
  }

  deleteComment(commentUuid: string) {
    this.commentService.deleteComment(this.projectUuid, this.organUuid, this.taskId()!, commentUuid, false).subscribe({
      next: () => this.refreshTaskData(),
      error: (err) => console.error('Failed to delete comment', err)
    });
  }

  // --- TRASH TOGGLE & RESTORES ---
  toggleTrash() {
    this.isTrashOpen.update(v => !v);
    if (this.isTrashOpen()) {
      this.loadTrashData();
    }
  }

  restoreComment(commentUuid: string) {
    this.commentService.restoreComment(this.projectUuid, this.organUuid, this.taskId()!, commentUuid).subscribe({
      next: () => this.refreshTaskData(),
      error: (err) => console.error('Failed to restore comment', err)
    });
  }

  restoreAttachment(attachmentUuid: string) {
    this.attachmentService.restoreAttachment(this.projectUuid, this.organUuid, this.taskId()!, attachmentUuid).subscribe({
      next: () => this.refreshTaskData(),
      error: (err) => console.error('Failed to restore attachment', err)
    });
  }

  hardDeleteComment(commentUuid: string) {
    this.showConfirm(
      'Supprimer définitivement ?',
      'Ce commentaire sera supprimé sans retour possible.',
      () => {
        this.commentService.deleteComment(this.projectUuid, this.organUuid, this.taskId()!, commentUuid, true).subscribe({
          next: () => this.refreshTaskData(),
          error: (err) => console.error('Failed to hard delete comment', err)
        });
      }
    );
  }

  hardDeleteAttachment(attachmentUuid: string) {
    this.showConfirm(
      'Supprimer définitivement ?',
      'Cette pièce jointe sera définitivement supprimée du serveur.',
      () => {
        this.attachmentService.deleteAttachment(this.projectUuid, this.organUuid, this.taskId()!, attachmentUuid, true).subscribe({
          next: () => this.refreshTaskData(),
          error: (err) => console.error('Failed to hard delete attachment', err)
        });
      }
    );
  }

  // Confirmation helpers
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

  // Error helpers
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
