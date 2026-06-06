import { Component, Input, Output, EventEmitter, signal, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-project-members-invite',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './project-members-invite.html'
})
export class ProjectMembersInviteComponent {
  @Input({ required: true }) invites!: any[];
  @Input({ required: true }) userEmail!: string;

  @Output() inviteAdded = new EventEmitter<string>();
  @Output() memberRemoved = new EventEmitter<number>();
  @Output() roleChanged = new EventEmitter<{ index: number, role: string }>();

  newEmail = '';
  showRoleExplanation = signal(false);
  activeMenuIndex: number | null = null;

  toggleRoleExplanation() {
    this.showRoleExplanation.update(v => !v);
  }

  addInvite() {
    const email = this.newEmail.trim();
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return;
    this.inviteAdded.emit(email);
    this.newEmail = '';
  }

  toggleRoleMenu(idx: number, event: Event) {
    event.stopPropagation();
    if (this.activeMenuIndex === idx) {
      this.activeMenuIndex = null;
    } else {
      this.activeMenuIndex = idx;
    }
  }

  updateRole(idx: number, role: string) {
    this.roleChanged.emit({ index: idx, role });
    this.activeMenuIndex = null;
  }

  removeInvite(idx: number) {
    this.memberRemoved.emit(idx);
    if (this.activeMenuIndex === idx) {
      this.activeMenuIndex = null;
    }
  }

  getRoleLabel(role: string): string {
    const labels: { [key: string]: string } = {
      ADMIN: 'Administrateur',
      MANAGER: 'Manager',
      MEMBER: 'Membre'
    };
    return labels[role] || role;
  }

  @HostListener('document:click')
  closeRoleMenus() {
    this.activeMenuIndex = null;
  }
}
