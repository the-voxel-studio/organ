import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectActivityFeedItem } from '../../../../../models/project.model';
import { ActivityTextPipe } from '../../../../../pipes/activity-text.pipe';
import { FormatDatePipe } from '../../../../../pipes/format-date.pipe';

@Component({
  selector: 'app-project-activity-feed',
  standalone: true,
  imports: [CommonModule, ActivityTextPipe, FormatDatePipe],
  templateUrl: './project-activity-feed.html'
})
export class ProjectActivityFeedComponent {
  @Input({ required: true }) activities!: ProjectActivityFeedItem[];
  @Input({ required: true }) projectColor!: string;

  getUserInitials(activity: ProjectActivityFeedItem): string {
    const first = activity.user_first_name ? activity.user_first_name.charAt(0).toUpperCase() : '';
    const last = activity.user_last_name ? activity.user_last_name.charAt(0).toUpperCase() : '';
    return first + last || 'U';
  }

  getActivityTypeLabel(type: 'COMMENT' | 'ATTACHMENT' | 'HISTORY'): string {
    const labels = {
      COMMENT: 'Commentaire',
      ATTACHMENT: 'Fichier',
      HISTORY: 'Activité'
    };
    return labels[type] || 'Activité';
  }
}

