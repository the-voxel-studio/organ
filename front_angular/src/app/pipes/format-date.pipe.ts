import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslationService } from '../services/common/translation.service';

@Pipe({
  name: 'formatDate',
  standalone: true
})
export class FormatDatePipe implements PipeTransform {
  private translationService = inject(TranslationService);

  transform(value: string | null | undefined, type: 'standard' | 'audit' | 'time' = 'standard'): string {
    if (type === 'audit') {
      return this.translationService.formatAuditDate(value);
    }
    if (type === 'time') {
      return this.translationService.formatTime(value);
    }
    return this.translationService.formatDate(value);
  }
}
