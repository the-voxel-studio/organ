import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslationService } from '../services/common/translation.service';
import { ProjectActivityFeedItem } from '../models/project.model';

@Pipe({
  name: 'activityText',
  standalone: true
})
export class ActivityTextPipe implements PipeTransform {
  private translationService = inject(TranslationService);

  transform(value: ProjectActivityFeedItem | null | undefined): string {
    if (!value) return '';
    return this.translationService.translateActivityText(value);
  }
}
