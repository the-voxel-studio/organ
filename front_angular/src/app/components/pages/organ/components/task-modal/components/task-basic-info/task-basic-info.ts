import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { TaskResponse, TaskStatus, TaskPermissionsResponse, TaskTagSummary } from '../../../../../../../models/task.model';
import { OrganMember } from '../../../../../../../models/organ.model';
import { TagResponse } from '../../../../../../../models/tag.model';

@Component({
  selector: 'app-task-basic-info',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './task-basic-info.html'
})
export class TaskBasicInfoComponent {
  @Input({ required: true }) taskForm!: FormGroup;

  // Inputs standards
  @Input() taskId: string | null = null;
  @Input() highlightColor: string = '#FF7DD4';
  @Input() isTaskTrashed: boolean = false;
  @Input() initialStatus: TaskStatus = 'TODO';
  
  // Lists
  @Input() members: OrganMember[] = [];
  @Input() projectTags: TagResponse[] = [];
  @Input() organTasks: TaskResponse[] = []; // tasks in organ for dependency selection
  @Input() assignees: any[] = [];
  @Input() tags: TaskTagSummary[] = [];
  @Input() dependencies: any[] = [];

  // Listes temporaires (pour le mode création)
  @Input() stagedAssignees: OrganMember[] = [];
  @Input() stagedTags: TagResponse[] = [];
  @Input() stagedDependencies: TaskResponse[] = [];

  // État des permissions
  @Input() currentPerms: TaskPermissionsResponse | null = null;
  @Input() editableFields: string[] = [];

  // Outputs pour modifications des sous-ressources
  @Output() assigneeAdded = new EventEmitter<OrganMember>();
  @Output() assigneeRemoved = new EventEmitter<string>();
  @Output() tagAdded = new EventEmitter<TagResponse>();
  @Output() tagRemoved = new EventEmitter<string>();
  @Output() dependencyAdded = new EventEmitter<TaskResponse>();
  @Output() dependencyRemoved = new EventEmitter<string>();

  // État des pickers
  showUserPicker = signal(false);
  showTagPicker = signal(false);
  showDependencyPicker = signal(false);

  userSearchQuery = '';
  taskSearchQuery = '';

  // Helpers et actions
  toggleUserPicker() {
    this.showUserPicker.update(v => !v);
    if (this.showUserPicker()) {
      this.showTagPicker.set(false);
      this.showDependencyPicker.set(false);
    }
  }

  toggleTagPicker() {
    this.showTagPicker.update(v => !v);
    if (this.showTagPicker()) {
      this.showUserPicker.set(false);
      this.showDependencyPicker.set(false);
    }
  }

  toggleDependencyPicker() {
    this.showDependencyPicker.update(v => !v);
    if (this.showDependencyPicker()) {
      this.showUserPicker.set(false);
      this.showTagPicker.set(false);
    }
  }

  addAssignee(m: OrganMember) {
    this.assigneeAdded.emit(m);
    this.showUserPicker.set(false);
    this.userSearchQuery = '';
  }

  removeAssignee(uuid: string) {
    this.assigneeRemoved.emit(uuid);
  }

  addTag(t: TagResponse) {
    this.tagAdded.emit(t);
    this.showTagPicker.set(false);
  }

  removeTag(uuid: string) {
    this.tagRemoved.emit(uuid);
  }

  addDependency(t: TaskResponse) {
    this.dependencyAdded.emit(t);
    this.showDependencyPicker.set(false);
    this.taskSearchQuery = '';
  }

  removeDependency(uuid: string) {
    this.dependencyRemoved.emit(uuid);
  }

  getFilteredMembers(): OrganMember[] {
    const list = this.members || [];
    const currentAssigneeUuids = this.assignees.map(a => a.uuid);
    return list.filter(m => 
      !currentAssigneeUuids.includes(m.uuid) &&
      (`${m.firstName} ${m.lastName}`).toLowerCase().includes(this.userSearchQuery.toLowerCase())
    );
  }

  getFilteredTags(): TagResponse[] {
    const list = this.projectTags || [];
    const currentTagUuids = this.tags.map(t => t.uuid);
    return list.filter(t => !currentTagUuids.includes(t.uuid));
  }

  getFilteredTasks(): TaskResponse[] {
    const list = this.organTasks || [];
    const currentDepUuids = this.dependencies.map(d => d.uuid);
    return list.filter(t => 
      !currentDepUuids.includes(t.uuid) &&
      t.title.toLowerCase().includes(this.taskSearchQuery.toLowerCase())
    );
  }

  validatePriority() {
    const control = this.taskForm.get('priority');
    if (!control) return;
    let val = control.value;
    if (val === null || val === undefined || val === '') return;
    if (val < 1) {
      control.setValue(1);
    } else if (val > 10) {
      control.setValue(10);
    }
  }

  isStatusMessageVisible(): boolean {
    return this.taskForm.get('status')?.value !== this.initialStatus;
  }

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
    if (this.isTaskTrashed) return false;
    let check = fieldName;
    if (fieldName === 'startDate' || fieldName === 'expiresAt') check = 'expiresAt';
    if (fieldName === 'managerUuid') check = 'manager';
    return this.editableFields.includes(check);
  }

  isTaskOwner(): boolean {
    return !!(this.currentPerms?.taskOwnership?.isManager || this.currentPerms?.taskOwnership?.isCreator);
  }
}
