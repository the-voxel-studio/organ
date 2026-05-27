import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslationService } from '../services/common/translation.service';

@Pipe({
  name: 'translateStatus',
  standalone: true
})
export class TranslateStatusPipe implements PipeTransform {
  private translationService = inject(TranslationService);

  transform(value: string | null | undefined): string {
    return this.translationService.translateStatus(value);
  }
}
