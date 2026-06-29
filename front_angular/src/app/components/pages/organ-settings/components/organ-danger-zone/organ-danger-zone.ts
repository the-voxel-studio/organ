import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { OrganService } from '../../../../../services/api/organ.service';

@Component({
  selector: 'app-organ-danger-zone',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './organ-danger-zone.html'
})
export class OrganDangerZoneComponent {
  @Input({ required: true }) projectUuid!: string;
  @Input({ required: true }) organUuid!: string;
  @Input({ required: true }) organTitle: string = '';
  @Input({ required: true }) canDelete: boolean = false;
  @Input({ required: true }) highlightColor: string = '#FF7EB6';

  @Output() loadingStateChanged = new EventEmitter<{ loading: boolean, text: string }>();
  @Output() errorOccurred = new EventEmitter<string>();

  private organService = inject(OrganService);
  private router = inject(Router);

  showDeleteModal = false;
  isSubmitting = signal(false);

  openDeleteModal() {
    if (!this.canDelete) return;
    this.showDeleteModal = true;
  }

  closeDeleteModal() {
    this.showDeleteModal = false;
  }

  async confirmDelete() {
    if (!this.canDelete) return;
    
    this.isSubmitting.set(true);
    this.loadingStateChanged.emit({ loading: true, text: "Suppression de l'organ..." });

    try {
      await firstValueFrom(this.organService.deleteOrgan(this.projectUuid, this.organUuid, false));
      this.isSubmitting.set(false);
      this.loadingStateChanged.emit({ loading: false, text: "" });
      this.showDeleteModal = false;
      this.router.navigate(['/project', this.projectUuid]);
    } catch (err: any) {
      console.error('Failed to delete organ', err);
      const msg = err?.error?.message || 'Erreur lors de la suppression de l\'organ.';
      this.errorOccurred.emit(msg);
      this.isSubmitting.set(false);
      this.loadingStateChanged.emit({ loading: false, text: "" });
      this.showDeleteModal = false;
    }
  }
}
