import { Injectable, inject, signal } from '@angular/core';
import { TaskService } from '../../../../../../services/api/task.service';
import { TaskCommentService } from '../../../../../../services/api/task-comment.service';
import { TaskAttachmentService } from '../../../../../../services/api/task-attachment.service';
import { TaskLinkService } from '../../../../../../services/api/task-link.service';
import { TaskDependencyService } from '../../../../../../services/api/task-dependency.service';
import { OrganService } from '../../../../../../services/api/organ.service';
import { TagService } from '../../../../../../services/api/tag.service';

import { TaskResponse, TaskStatus, TaskTagSummary, TaskLinkSummary, TaskPermissionsResponse, TaskTimelineItem, CreateTaskRequest, UpdateTaskRequest } from '../../../../../../models/task.model';
import { TaskCommentResponse } from '../../../../../../models/task-comment.model';
import { TaskAttachmentResponse } from '../../../../../../models/task-attachment.model';
import { OrganMember } from '../../../../../../models/organ.model';
import { TagResponse } from '../../../../../../models/tag.model';

@Injectable()
export class TaskModalStore {
  private taskService = inject(TaskService);
  private commentService = inject(TaskCommentService);
  private attachmentService = inject(TaskAttachmentService);
  private linkService = inject(TaskLinkService);
  private dependencyService = inject(TaskDependencyService);
  private organService = inject(OrganService);
  private tagService = inject(TagService);

  // Signals
  taskId = signal<string | null>(null);
  projectUuid = signal<string>('');
  organUuid = signal<string>('');
  isTaskTrashed = signal(false);
  isTrashOpen = signal(false);
  
  isOpen = signal(false);
  backdropVisible = signal(false);
  panelVisible = signal(false);
  isLoading = signal(false);
  isUploading = signal(false);

  taskData = signal<TaskResponse | null>(null);
  currentPerms = signal<TaskPermissionsResponse | null>(null);
  editableFields = signal<string[]>([]);

  // Sub-resource lists
  members = signal<OrganMember[]>([]);
  projectTags = signal<TagResponse[]>([]);
  organTasks = signal<TaskResponse[]>([]); // for dependencies

  assignees = signal<any[]>([]);
  tags = signal<TaskTagSummary[]>([]);
  links = signal<TaskLinkSummary[]>([]);
  dependencies = signal<any[]>([]);
  attachments = signal<TaskAttachmentResponse[]>([]);
  comments = signal<TaskCommentResponse[]>([]);
  timeline = signal<TaskTimelineItem[]>([]);

  // Trash data
  deletedAttachments = signal<TaskAttachmentResponse[]>([]);
  deletedLinks = signal<TaskLinkSummary[]>([]);
  deletedComments = signal<TaskCommentResponse[]>([]);

  // Pagination
  timelineOffset = 0;
  timelineLimit = 10;
  hasMoreTimeline = signal(false);

  // Staged creation lists
  stagedAssignees = signal<OrganMember[]>([]);
  stagedTags = signal<TagResponse[]>([]);
  stagedLinks = signal<Array<{ url: string; description?: string }>>([]);
  stagedDependencies = signal<TaskResponse[]>([]);

  reset() {
    this.taskData.set(null);
    this.attachments.set([]);
    this.comments.set([]);
    this.timeline.set([]);
    this.dependencies.set([]);
    this.currentPerms.set(null);
    this.editableFields.set([]);
    this.timelineOffset = 0;
    this.hasMoreTimeline.set(false);

    this.deletedAttachments.set([]);
    this.deletedLinks.set([]);
    this.deletedComments.set([]);

    this.stagedAssignees.set([]);
    this.stagedTags.set([]);
    this.stagedLinks.set([]);
    this.stagedDependencies.set([]);
  }

  loadInitialStaticData(projectUuid: string, organUuid: string) {
    this.projectUuid.set(projectUuid);
    this.organUuid.set(organUuid);

    this.organService.getOrganMembers(projectUuid, organUuid).subscribe({
      next: (members) => this.members.set(members),
      error: (err: any) => console.error('Failed to load organ members', err)
    });

    this.tagService.getTags(projectUuid).subscribe({
      next: (tags) => this.projectTags.set(tags),
      error: (err: any) => console.error('Failed to load project tags', err)
    });
  }

  loadOrganTasks() {
    this.taskService.getTasks(this.projectUuid(), this.organUuid()).subscribe({
      next: (tasks) => {
        this.organTasks.set(tasks.filter(t => t.uuid !== this.taskId()));
      },
      error: (err: any) => console.error('Failed to load tasks for dependencies', err)
    });
  }

  openForCreate(stagedPermissions: TaskPermissionsResponse) {
    this.taskId.set(null);
    this.isTaskTrashed.set(false);
    this.isTrashOpen.set(false);
    this.reset();
    
    this.currentPerms.set(stagedPermissions);
    this.editableFields.set(stagedPermissions.editableFields || []);

    this.isOpen.set(true);
    this.backdropVisible.set(true);
    this.panelVisible.set(true);
  }

  openForEdit(taskId: string, isTrash = false) {
    this.taskId.set(taskId);
    this.isTaskTrashed.set(isTrash);
    this.isTrashOpen.set(false);
    this.reset();

    this.isOpen.set(true);
    this.backdropVisible.set(true);
    this.panelVisible.set(true);
    this.isLoading.set(true);

    this.loadOrganTasks();
  }

  closeModalFlow(callback: () => void) {
    this.panelVisible.set(false);
    this.backdropVisible.set(false);
    setTimeout(() => {
      this.isOpen.set(false);
      callback();
    }, 300);
  }

  loadComments() {
    this.commentService.getComments(this.projectUuid(), this.organUuid(), this.taskId()!).subscribe({
      next: (comments) => this.comments.set(comments),
      error: (err: any) => console.error('Failed to load comments', err)
    });
  }

  loadAttachments() {
    this.attachmentService.getAttachments(this.projectUuid(), this.organUuid(), this.taskId()!).subscribe({
      next: (attachments) => this.attachments.set(attachments),
      error: (err: any) => console.error('Failed to load attachments', err)
    });
  }

  loadDependencies() {
    this.dependencyService.getDependencies(this.projectUuid(), this.organUuid(), this.taskId()!).subscribe({
      next: (deps) => this.dependencies.set(deps.map(d => ({
        uuid: d.dependsOnTaskUuid,
        title: d.title
      }))),
      error: (err: any) => console.error('Failed to load dependencies', err)
    });
  }

  loadTimeline(append = false) {
    if (!append) {
      this.timelineOffset = 0;
      this.timeline.set([]);
    }
    this.taskService.getTaskTimeline(this.projectUuid(), this.organUuid(), this.taskId()!, this.timelineOffset, this.timelineLimit).subscribe({
      next: (items) => {
        const actionItems = items.filter(item => item.actionType !== 'CONSULTATION');
        if (append) {
          this.timeline.set([...this.timeline(), ...actionItems]);
        } else {
          this.timeline.set(actionItems);
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

    this.commentService.getTrashedComments(this.projectUuid(), this.organUuid(), tId).subscribe({
      next: (comments) => this.deletedComments.set(comments),
      error: (err: any) => console.error('Failed to load trashed comments', err)
    });

    this.attachmentService.getTrashedAttachments(this.projectUuid(), this.organUuid(), tId).subscribe({
      next: (attachments) => this.deletedAttachments.set(attachments),
      error: (err: any) => console.error('Failed to load trashed attachments', err)
    });

    this.linkService.getTrashedLinks(this.projectUuid(), this.organUuid(), tId).subscribe({
      next: (links) => this.deletedLinks.set(links),
      error: (err: any) => console.error('Failed to load trashed links', err)
    });
  }

  loadTaskPermissions() {
    this.taskService.getTaskPermissions(this.projectUuid(), this.organUuid(), this.taskId()!).subscribe({
      next: (perms) => {
        this.currentPerms.set(perms);
        this.editableFields.set(perms.editableFields || []);
      },
      error: (err: any) => console.error('Failed to load task permissions', err)
    });
  }

  refreshTaskData(onSuccess?: (task: TaskResponse) => void, onError?: (err: any) => void) {
    const tId = this.taskId();
    if (!tId) return;

    this.taskService.getTask(this.projectUuid(), this.organUuid(), tId, this.isTaskTrashed()).subscribe({
      next: (task) => {
        this.taskData.set(task);
        this.assignees.set(task.assignees);
        this.tags.set(task.tags);
        this.links.set(task.links);

        this.loadTaskPermissions();
        this.loadComments();
        this.loadAttachments();
        this.loadTimeline();
        this.loadDependencies();

        if (this.isTrashOpen()) {
          this.loadTrashData();
        }

        this.isLoading.set(false);
        if (onSuccess) onSuccess(task);
      },
      error: (err: any) => {
        this.isLoading.set(false);
        if (onError) onError(err);
      }
    });
  }

  addAssignee(member: OrganMember, onRefresh?: (task: TaskResponse) => void) {
    if (!this.taskId()) {
      this.stagedAssignees.update(arr => [...arr, member]);
      this.assignees.set(this.stagedAssignees());
      return;
    }

    this.taskService.addAssignee(this.projectUuid(), this.organUuid(), this.taskId()!, { userUuid: member.uuid }).subscribe({
      next: () => this.refreshTaskData(onRefresh),
      error: (err: any) => console.error('Failed to add assignee', err)
    });
  }

  removeAssignee(memberUuid: string, onRefresh?: (task: TaskResponse) => void) {
    if (!this.taskId()) {
      this.stagedAssignees.update(arr => arr.filter(a => a.uuid !== memberUuid));
      this.assignees.set(this.stagedAssignees());
      return;
    }

    this.taskService.removeAssignee(this.projectUuid(), this.organUuid(), this.taskId()!, memberUuid).subscribe({
      next: () => this.refreshTaskData(onRefresh),
      error: (err: any) => console.error('Failed to remove assignee', err)
    });
  }

  addTag(tag: TagResponse, onRefresh?: (task: TaskResponse) => void, onSaved?: () => void) {
    if (!this.taskId()) {
      this.stagedTags.update(arr => [...arr, tag]);
      this.tags.set(this.stagedTags().map(t => ({ uuid: t.uuid, name: t.name, color: t.color })));
      return;
    }

    this.tagService.addTaskTag(this.projectUuid(), this.organUuid(), this.taskId()!, { tagUuid: tag.uuid }).subscribe({
      next: () => {
        this.refreshTaskData(onRefresh);
        if (onSaved) onSaved();
      },
      error: (err: any) => console.error('Failed to add tag to task', err)
    });
  }

  removeTag(tagUuid: string, onRefresh?: (task: TaskResponse) => void, onSaved?: () => void) {
    if (!this.taskId()) {
      this.stagedTags.update(arr => arr.filter(t => t.uuid !== tagUuid));
      this.tags.set(this.stagedTags().map(t => ({ uuid: t.uuid, name: t.name, color: t.color })));
      return;
    }

    this.tagService.removeTaskTag(this.projectUuid(), this.organUuid(), this.taskId()!, tagUuid).subscribe({
      next: () => {
        this.refreshTaskData(onRefresh);
        if (onSaved) onSaved();
      },
      error: (err: any) => console.error('Failed to remove tag from task', err)
    });
  }

  addLink(payload: { url: string; description?: string }, onRefresh?: (task: TaskResponse) => void, onSaved?: () => void) {
    const url = payload.url.trim();
    if (!url) return;

    if (!this.taskId()) {
      this.stagedLinks.update(arr => [...arr, { url, description: payload.description || undefined }]);
      this.links.set(this.stagedLinks().map((l, i) => ({
        uuid: `staged-${i}`,
        url: l.url,
        description: l.description || null
      })));
      return;
    }

    this.linkService.createLink(this.projectUuid(), this.organUuid(), this.taskId()!, {
      url,
      description: payload.description || undefined
    }).subscribe({
      next: () => {
        this.refreshTaskData(onRefresh);
        if (onSaved) onSaved();
      },
      error: (err: any) => console.error('Failed to add task link', err)
    });
  }

  removeLink(linkUuid: string, onRefresh?: (task: TaskResponse) => void, onSaved?: () => void) {
    if (!this.taskId()) {
      this.stagedLinks.update(arr => arr.filter((_, i) => `staged-${i}` !== linkUuid));
      this.links.set(this.stagedLinks().map((l, i) => ({
        uuid: `staged-${i}`,
        url: l.url,
        description: l.description || null
      })));
      return;
    }

    this.linkService.deleteLink(this.projectUuid(), this.organUuid(), this.taskId()!, linkUuid, false).subscribe({
      next: () => {
        this.refreshTaskData(onRefresh);
        if (onSaved) onSaved();
      },
      error: (err: any) => console.error('Failed to delete task link', err)
    });
  }

  addDependency(task: TaskResponse, onRefresh?: (task: TaskResponse) => void) {
    if (!this.taskId()) {
      this.stagedDependencies.update(arr => [...arr, task]);
      this.dependencies.set(this.stagedDependencies().map(d => ({
        uuid: d.uuid,
        title: d.title
      })));
      return;
    }

    this.dependencyService.addDependency(this.projectUuid(), this.organUuid(), this.taskId()!, { dependsOnTaskUuid: task.uuid }).subscribe({
      next: () => this.refreshTaskData(onRefresh),
      error: (err: any) => console.error('Failed to add dependency', err)
    });
  }

  removeDependency(depUuid: string, onRefresh?: (task: TaskResponse) => void) {
    if (!this.taskId()) {
      this.stagedDependencies.update(arr => arr.filter(d => d.uuid !== depUuid));
      this.dependencies.set(this.stagedDependencies().map(d => ({
        uuid: d.uuid,
        title: d.title
      })));
      return;
    }

    this.dependencyService.removeDependency(this.projectUuid(), this.organUuid(), this.taskId()!, depUuid).subscribe({
      next: () => this.refreshTaskData(onRefresh),
      error: (err: any) => console.error('Failed to remove dependency', err)
    });
  }

  addComment(content: string, onRefresh?: (task: TaskResponse) => void, onSaved?: () => void) {
    if (!content.trim()) return;

    this.commentService.createComment(this.projectUuid(), this.organUuid(), this.taskId()!, { content }).subscribe({
      next: () => {
        this.refreshTaskData(onRefresh);
        if (onSaved) onSaved();
      },
      error: (err: any) => console.error('Failed to add comment', err)
    });
  }

  deleteComment(commentUuid: string, onRefresh?: (task: TaskResponse) => void) {
    this.commentService.deleteComment(this.projectUuid(), this.organUuid(), this.taskId()!, commentUuid, false).subscribe({
      next: () => this.refreshTaskData(onRefresh),
      error: (err: any) => console.error('Failed to delete comment', err)
    });
  }

  restoreComment(commentUuid: string, onRefresh?: (task: TaskResponse) => void) {
    this.commentService.restoreComment(this.projectUuid(), this.organUuid(), this.taskId()!, commentUuid).subscribe({
      next: () => this.refreshTaskData(onRefresh),
      error: (err: any) => console.error('Failed to restore comment', err)
    });
  }

  restoreAttachment(attachmentUuid: string, onRefresh?: (task: TaskResponse) => void) {
    this.attachmentService.restoreAttachment(this.projectUuid(), this.organUuid(), this.taskId()!, attachmentUuid).subscribe({
      next: () => this.refreshTaskData(onRefresh),
      error: (err: any) => console.error('Failed to restore attachment', err)
    });
  }

  hardDeleteComment(commentUuid: string, onRefresh?: (task: TaskResponse) => void) {
    this.commentService.deleteComment(this.projectUuid(), this.organUuid(), this.taskId()!, commentUuid, true).subscribe({
      next: () => this.refreshTaskData(onRefresh),
      error: (err: any) => console.error('Failed to hard delete comment', err)
    });
  }

  hardDeleteAttachment(attachmentUuid: string, onRefresh?: (task: TaskResponse) => void) {
    this.attachmentService.deleteAttachment(this.projectUuid(), this.organUuid(), this.taskId()!, attachmentUuid, true).subscribe({
      next: () => this.refreshTaskData(onRefresh),
      error: (err: any) => console.error('Failed to hard delete attachment', err)
    });
  }

  deleteTask(isHard: boolean, onSaved: () => void, onError: (err: any) => void) {
    this.taskService.deleteTask(this.projectUuid(), this.organUuid(), this.taskId()!, isHard).subscribe({
      next: () => onSaved(),
      error: (err: any) => onError(err)
    });
  }

  restoreTask(onSaved: () => void, onError: (err: any) => void) {
    this.taskService.restoreTask(this.projectUuid(), this.organUuid(), this.taskId()!).subscribe({
      next: () => onSaved(),
      error: (err: any) => onError(err)
    });
  }

  saveTask(req: CreateTaskRequest | UpdateTaskRequest, isEdit: boolean, onSaved: () => void, onError: (err: any) => void) {
    if (isEdit) {
      this.taskService.updateTask(this.projectUuid(), this.organUuid(), this.taskId()!, req as UpdateTaskRequest).subscribe({
        next: () => onSaved(),
        error: (err: any) => onError(err)
      });
    } else {
      this.taskService.createTask(this.projectUuid(), this.organUuid(), req as CreateTaskRequest).subscribe({
        next: () => onSaved(),
        error: (err: any) => onError(err)
      });
    }
  }
}
