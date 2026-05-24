import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-organ-members',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './organ-members.html'
})
export class OrganMembersComponent {
  @Input({ required: true }) addedMembers: any[] = [];
  @Input({ required: true }) projectMembers: any[] = [];
  @Input({ required: true }) roles: any[] = [];
  @Input({ required: true }) highlightColor: string = '#FF7EB6';
  @Input({ required: true }) canManage: boolean = false;

  @Output() membersChanged = new EventEmitter<any[]>();

  selectedMemberUuid = '';

  addMember() {
    if (!this.canManage) return;
    const userUuid = this.selectedMemberUuid;
    if (!userUuid) return;
    if (this.addedMembers.some(m => m.userUuid === userUuid)) return;

    const projMember = this.projectMembers.find(m => m.user.uuid === userUuid);
    if (!projMember) return;

    const updated = [
      ...this.addedMembers,
      {
        userUuid: userUuid,
        name: projMember.user.firstName + ' ' + projMember.user.lastName,
        email: projMember.user.email,
        roles: []
      }
    ];

    this.membersChanged.emit(updated);
    this.selectedMemberUuid = '';
  }

  removeMember(userUuid: string) {
    if (!this.canManage) return;
    const updated = this.addedMembers.filter(m => m.userUuid !== userUuid);
    this.membersChanged.emit(updated);
  }

  toggleMemberRole(userUuid: string, roleId: string) {
    if (!this.canManage) return;
    const updated = this.addedMembers.map(m => {
      if (m.userUuid === userUuid) {
        const roles = [...m.roles];
        const idx = roles.indexOf(roleId);
        if (idx !== -1) {
          roles.splice(idx, 1);
        } else {
          roles.push(roleId);
        }
        return { ...m, roles };
      }
      return m;
    });
    this.membersChanged.emit(updated);
  }

  isMemberRoleActive(userUuid: string, roleId: string): boolean {
    const member = this.addedMembers.find(m => m.userUuid === userUuid);
    if (!member) return false;
    return member.roles.includes(roleId);
  }
}
