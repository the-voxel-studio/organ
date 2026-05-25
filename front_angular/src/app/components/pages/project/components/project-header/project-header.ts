import { Component, Input, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-project-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <header class="flex flex-col sm:flex-row sm:items-center justify-between gap-6 mb-12">
      <div class="flex items-center gap-6">
        <div class="w-16 h-16 bg-white rounded-2xl shadow-sm border-2 flex items-center justify-center overflow-hidden"
             [style.border-color]="project.color || '#FF7EB6'">
          @if (project.iconData) {
            @if (project.iconType === 'EMOJI') {
              <span class="text-3xl">{{ project.iconData }}</span>
            } @else if (project.iconType === 'BLOB' || project.iconType === 'IMAGE') {
              <img [src]="project.iconData" class="w-full h-full object-cover">
            } @else if (project.iconType === 'SVG' || project.iconType === 'ICON') {
              <div class="w-8 h-8 flex items-center justify-center" [innerHTML]="safeSvg(project.iconData)"></div>
            }
          } @else {
            <div class="w-full h-full" [style.background-color]="project.color || '#FF7EB6'"></div>
          }
        </div>
        <div>
          <h1 class="text-3xl font-black text-black mb-1">{{ project.title }}</h1>
          <div class="flex items-center gap-3">
            @if (project.status === 'ACTIVE') {
              <span class="px-3 py-1 text-[10px] font-black uppercase tracking-widest rounded-lg"
                    [style.background-color]="(project.color || '#FF7EB6') + '1a'"
                    [style.color]="project.color || '#FF7EB6'">
                Actif
              </span>
            } @else if (project.status === 'ARCHIVED') {
              <span class="px-3 py-1 bg-slate-100 text-slate-600 text-[10px] font-black uppercase tracking-widest rounded-lg border border-slate-200">
                Archivé
              </span>
            } @else {
              <span class="px-3 py-1 bg-amber-50 text-amber-600 text-[10px] font-black uppercase tracking-widest rounded-lg border border-amber-100">
                Inactif
              </span>
            }
            <span class="text-xs text-gray-400 font-medium italic">
              Votre rôle : {{ project.role }}
            </span>
          </div>
          @if (admin) {
            <div class="mt-2 text-xs text-gray-400 font-bold italic">
              Admin : {{ admin.firstName }} {{ admin.lastName }} 
              <span class="mx-1">•</span>
              <a href="mailto:{{ admin.email }}" class="hover:text-black transition-colors">{{ admin.email }}</a>
            </div>
          }
        </div>
      </div>
      <div class="flex items-center gap-3">
        @if (canManage) {
          <a [routerLink]="['/project', project.uuid, 'trash']" class="p-3 bg-white border border-gray-200 text-gray-400 hover:text-red-500 hover:border-red-100 hover:bg-red-50 rounded-2xl transition-all active:scale-95 shadow-sm group cursor-pointer" title="Corbeille">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-5 h-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
            </svg>
          </a>
          <a [routerLink]="['/project', project.uuid, 'analytics']" class="p-3 bg-white border border-gray-200 text-gray-400 hover:text-bubblegum hover:border-bubblegum/20 hover:bg-bubblegum/5 rounded-2xl transition-all active:scale-95 shadow-sm group cursor-pointer" title="Statistiques et Audit">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-5 h-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125z" />
            </svg>
          </a>
        }
        @if (project.role === 'ADMIN') {
          <a [routerLink]="['/project', project.uuid, 'settings']" class="px-6 py-3 bg-black text-white font-bold rounded-2xl transition-all hover:bg-black/90 active:scale-95 shadow-lg flex items-center gap-2 cursor-pointer">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-4 h-4">
              <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 6h9.75M10.5 6a1.5 1.5 0 1 1-3 0m3 0a1.5 1.5 0 1 0-3 0M3.75 6H7.5m3 12h9.75m-9.75 0a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m-3.75 0H7.5m9-6h3.75m-3.75 0a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m-9.75 0h9.75"/>
            </svg>
            Paramètres
          </a>
        }
      </div>
    </header>

    <!-- Custom Alert for Not Implemented Placeholders -->
    @if (showNotImplementedAlert()) {
      <div class="fixed inset-0 z-[120] flex items-center justify-center p-4 animate-in fade-in duration-200">
        <div class="fixed inset-0 bg-black/60 backdrop-blur-sm" (click)="closeNotImplementedAlert()"></div>
        <div class="relative bg-white rounded-[2.5rem] shadow-2xl max-w-sm w-full p-8 space-y-6 animate-in zoom-in-95 duration-200">
          <div class="w-16 h-16 bg-blue-50 rounded-2xl flex items-center justify-center text-blue-500 mx-auto">
            <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z"></path></svg>
          </div>
          
          <div class="text-center space-y-2">
            <h3 class="text-xl font-bold text-gray-900">Fonctionnalité en cours</h3>
            <p class="text-sm text-gray-500 leading-relaxed">
              La fonctionnalité <strong>{{ notImplementedFeatureName }}</strong> n'est pas encore disponible dans cette version d'évaluation front-end Angular. Elle sera transposée prochainement !
            </p>
          </div>

          <div class="pt-2">
            <button type="button" (click)="closeNotImplementedAlert()" 
                    class="w-full py-4 bg-black text-white font-black uppercase tracking-widest text-[10px] rounded-2xl hover:bg-gray-900 transition-all shadow-lg active:scale-95 cursor-pointer">
              Compris
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class ProjectHeaderComponent {
  @Input({ required: true }) project!: any;
  @Input({ required: true }) admin!: any;
  @Input({ required: true }) canManage!: boolean;

  private sanitizer = inject(DomSanitizer);

  showNotImplementedAlert = signal(false);
  notImplementedFeatureName = '';

  safeSvg(svgContent: string | null | undefined): SafeHtml {
    if (!svgContent) return '';
    return this.sanitizer.bypassSecurityTrustHtml(svgContent);
  }

  openNotImplementedAlert(featureName: string) {
    this.notImplementedFeatureName = featureName;
    this.showNotImplementedAlert.set(true);
  }

  closeNotImplementedAlert() {
    this.showNotImplementedAlert.set(false);
  }
}
