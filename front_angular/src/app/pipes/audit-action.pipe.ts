import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslationService } from '../services/common/translation.service';
import { ProjectAuditLogItem } from '../models/project.model';

@Pipe({
  name: 'auditAction',
  standalone: true
})
export class AuditActionPipe implements PipeTransform {
  private translationService = inject(TranslationService);

  transform(value: ProjectAuditLogItem | null | undefined): string {
    if (!value) return '';
    return this.translationService.translateAuditAction(value);
  }
}
