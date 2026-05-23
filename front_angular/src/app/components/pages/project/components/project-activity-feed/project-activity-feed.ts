import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectActivityFeedItem } from '../../../../../models/project.model';

@Component({
  selector: 'app-project-activity-feed',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="h-full">
      <h2 class="text-xl font-bold text-black mb-8 flex items-center gap-3">
        <div class="p-2 bg-green-50 rounded-xl text-green-600">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z"/>
          </svg>
        </div>
        Dernières activités
      </h2>
      <div class="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
        <div class="divide-y divide-gray-50">
          @for (activity of activities; track activity.created_at) {
            <div class="p-6 flex items-start gap-4 hover:bg-gray-50 transition-colors">
              <div class="w-10 h-10 rounded-xl bg-white border border-gray-100 flex items-center justify-center flex-shrink-0 text-bubblegum font-black text-sm uppercase shadow-sm">
                {{ getUserInitials(activity) }}
              </div>
              <div class="flex-grow min-w-0">
                <p class="text-sm text-gray-600 leading-snug">
                  <span class="font-bold text-gray-900">{{ activity.user_first_name }} {{ activity.user_last_name }}</span>
                  
                  {{ getActivityText(activity) }}
                  
                  @if (activity.field_name && activity.type === 'HISTORY' && activity.action_type === 'UPDATE') {
                    {{ translateField(activity.field_name) | lowercase }}
                  }

                  sur <span class="font-bold text-gray-900">{{ activity.task_title }}</span>
                  <span class="text-[10px] font-black uppercase tracking-widest text-gray-400 block mt-1">dans {{ activity.organ_title }}</span>
                </p>
                <div class="flex items-center gap-2 mt-2">
                  <span class="text-[10px] font-black uppercase tracking-widest text-gray-400">{{ formatTime(activity.created_at) }}</span>
                  <span class="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-md"
                        [style.background-color]="projectColor + '1a'" 
                        [style.color]="projectColor">
                    {{ getActivityTypeLabel(activity.type) }}
                  </span>
                </div>
              </div>
            </div>
          } @empty {
            <div class="p-12 text-center text-gray-400 italic font-medium">
              Aucune activité récente sur les tâches.
            </div>
          }
        </div>
      </div>
    </section>
  `
})
export class ProjectActivityFeedComponent {
  @Input({ required: true }) activities!: ProjectActivityFeedItem[];
  @Input({ required: true }) projectColor!: string;

  formatTime(dateString: string | null | undefined): string {
    if (!dateString) return '';
    try {
      const date = new Date(dateString);
      return date.toLocaleTimeString('fr-FR', {
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return '';
    }
  }

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

  getActivityText(activity: ProjectActivityFeedItem): string {
    if (activity.type === 'COMMENT') {
      return 'a ajouté un commentaire';
    } else if (activity.type === 'ATTACHMENT') {
      return 'a ajouté une pièce jointe';
    } else if (activity.type === 'HISTORY') {
      const action = activity.action_type;
      const field = activity.field_name;
      if (action === 'CREATE') {
        return 'a créé la tâche';
      } else if (action === 'UPDATE') {
        if (field === 'status') {
          return 'a changé le statut';
        } else if (field === 'priority') {
          return 'a changé la priorité';
        }
        return 'a modifié ' + (this.translateField(field) || '');
      } else if (action === 'STATUS_CHANGE') {
        return 'a changé le statut';
      } else if (action === 'PRIORITY_CHANGE') {
        return 'a changé la priorité';
      } else if (action === 'COMMENT_ADD' || action === 'COMMENT_CREATE') {
        return 'a ajouté un commentaire';
      } else if (action === 'ATTACHMENT_ADD') {
        return 'a ajouté une pièce jointe';
      } else if (action === 'LINK_ADD') {
        return 'a ajouté un lien';
      } else if (action === 'ASSIGNEE_ADD') {
        return 'a assigné la tâche';
      } else if (action === 'ASSIGNEE_REMOVE') {
        return 'a retiré l\'assignation';
      } else if (action === 'RESTORE') {
        return 'a restauré';
      }
    }
    return '';
  }

  translateField(field: string | null | undefined): string {
    if (!field) return '';
    const fields: { [key: string]: string } = {
      title: 'le titre',
      description: 'la description',
      status: 'le statut',
      status_message: 'le message de statut',
      statusMessage: 'le message de statut',
      priority: 'la priorité',
      estimated_hours: 'le temps estimé',
      estimatedHours: 'le temps estimé',
      start_date: 'la date de début',
      startDate: 'la date de début',
      expires_at: 'l\'échéance',
      expiresAt: 'l\'échéance'
    };
    return fields[field] || field;
  }
}
