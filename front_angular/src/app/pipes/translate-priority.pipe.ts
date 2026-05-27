import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslationService } from '../services/common/translation.service';

@Pipe({
  name: 'translatePriority',
  standalone: true
})
export class TranslatePriorityPipe implements PipeTransform {
  private translationService = inject(TranslationService);

  transform(value: string | null | undefined): string {
    return this.translationService.translatePriority(value);
  }
}
