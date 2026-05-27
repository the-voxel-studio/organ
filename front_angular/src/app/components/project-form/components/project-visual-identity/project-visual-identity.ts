import { Component, Input, Output, EventEmitter, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-project-visual-identity',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="grid grid-cols-1 md:grid-cols-2 gap-10 animate-in fade-in duration-200">
      <!-- COULEUR -->
      <div class="space-y-4">
        <label class="block text-sm font-semibold text-gray-700">Couleur du projet</label>
        <div class="flex flex-wrap gap-3 items-center">
          @for (color of colors; track color) {
            <label class="relative cursor-pointer group">
              <input type="radio" name="project_color" [value]="color" class="sr-only peer"
                     [checked]="selectedColor === color"
                     (change)="selectPresetColor(color)">
              <div class="w-10 h-10 rounded-full border-2 border-transparent peer-checked:border-black transition-all group-hover:scale-110" [style.background-color]="color"></div>
            </label>
          }
          
          <label class="relative cursor-pointer group">
            <input type="radio" name="project_color" value="custom" class="sr-only peer"
                   [checked]="isCustomColor"
                   (change)="selectCustomColorMode()">
            <input type="color" [value]="customColor" (input)="onCustomColorInput($event)" class="sr-only" #customColorPicker>
            <div (click)="customColorPicker.click()"
                 class="w-10 h-10 rounded-full bg-gray-100 border-2 border-dashed border-gray-300 peer-checked:border-black peer-checked:border-solid flex items-center justify-center text-gray-500 transition-all group-hover:scale-110"
                 [style.background-color]="isCustomColor ? selectedColor : ''">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" [ngClass]="isCustomColor ? 'hidden' : ''"><path d="M5 12h14"/><path d="M12 5v14"/></svg>
            </div>
          </label>
        </div>
      </div>

      <!-- ICÔNE -->
      <div class="space-y-4">
        <label class="block text-sm font-semibold text-gray-700">Icône du projet</label>
        
        <div class="flex flex-wrap gap-2 p-1 bg-gray-50 rounded-xl w-fit">
          <button type="button" (click)="switchIconMode('BLOB')" [ngClass]="iconMode() === 'BLOB' ? 'bg-white shadow-sm text-gray-900' : 'text-gray-500 hover:text-gray-900'" class="px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer">Image</button>
          <button type="button" (click)="switchIconMode('EMOJI')" [ngClass]="iconMode() === 'EMOJI' ? 'bg-white shadow-sm text-gray-900' : 'text-gray-500 hover:text-gray-900'" class="px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer">Emoji</button>
          <button type="button" (click)="switchIconMode('SVG')" [ngClass]="iconMode() === 'SVG' ? 'bg-white shadow-sm text-gray-900' : 'text-gray-500 hover:text-gray-900'" class="px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer">SVG</button>
        </div>

        <!-- Image Upload (BLOB) -->
        @if (iconMode() === 'BLOB') {
          <div class="animate-in fade-in duration-150">
            <label class="flex items-center justify-center w-32 h-32 bg-gray-50 border-2 border-dashed border-gray-200 rounded-2xl cursor-pointer hover:border-bubblegum transition-all group overflow-hidden">
              <input type="file" class="sr-only" accept="image/*" (change)="handleImageUpload($event)">
              
              @if (imagePreview) {
                <img [src]="imagePreview" class="w-full h-full object-cover">
              } @else {
                <div class="text-center">
                  <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto h-8 w-8 text-gray-400 group-hover:text-bubblegum" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                  </svg>
                  <span class="mt-2 block text-xs font-semibold text-gray-400 group-hover:text-bubblegum">Upload</span>
                </div>
              }
            </label>
          </div>
        }

        <!-- Emoji Input -->
        @if (iconMode() === 'EMOJI') {
          <div class="animate-in fade-in duration-150">
            <div class="flex items-center gap-4">
              <input type="text" [(ngModel)]="emojiValue" name="emojiValue" (input)="onEmojiInput()"
                     class="w-16 h-16 text-3xl text-center bg-gray-50 border-2 border-dashed border-gray-200 focus:border-bubblegum focus:bg-white rounded-2xl outline-none transition-all"
                     placeholder="🚀" maxlength="2">
            </div>
          </div>
        }

        <!-- Custom SVG -->
        @if (iconMode() === 'SVG') {
          <div class="space-y-3 animate-in fade-in duration-150">
            <textarea [(ngModel)]="svgValue" name="svgValue" (input)="onSvgInput()" rows="3"
                      class="block w-full px-4 py-3 bg-gray-50 border-2 border-dashed border-gray-200 focus:border-bubblegum focus:bg-white rounded-2xl text-xs font-mono transition-all resize-none"
                      placeholder="<svg ...>...</svg>"></textarea>
          </div>
        }
      </div>
    </div>
  `
})
export class ProjectVisualIdentityComponent implements OnInit {
  @Input() initialColor: string = '#FF7EB6';
  @Input() initialIconType: string = 'BLOB';
  @Input() initialIconData: string | null = null;

  @Output() colorChanged = new EventEmitter<string>();
  @Output() iconChanged = new EventEmitter<{ type: string, data: string | null }>();

  colors = ['#FF7EB6', '#4ADE80', '#60A5FA', '#FBBF24', '#A78BFA'];

  // États des couleurs
  selectedColor = '#FF7EB6';
  customColor = '#000000';
  isCustomColor = false;

  // États des icônes
  iconMode = signal('BLOB');
  imagePreview: string | null = null;
  emojiValue = '';
  svgValue = '';

  ngOnInit() {
    this.selectedColor = this.initialColor;
    if (!this.colors.includes(this.selectedColor)) {
      this.isCustomColor = true;
      this.customColor = this.selectedColor;
    }

    this.iconMode.set(this.initialIconType || 'BLOB');
    const data = this.initialIconData;
    if (this.iconMode() === 'BLOB') {
      this.imagePreview = data;
    } else if (this.iconMode() === 'EMOJI') {
      this.emojiValue = data || '';
    } else if (this.iconMode() === 'SVG') {
      this.svgValue = data || '';
    }
  }

  selectPresetColor(color: string) {
    this.selectedColor = color;
    this.isCustomColor = false;
    this.colorChanged.emit(this.selectedColor);
  }

  selectCustomColorMode() {
    this.isCustomColor = true;
    this.selectedColor = this.customColor;
    this.colorChanged.emit(this.selectedColor);
  }

  onCustomColorInput(event: Event) {
    const input = event.target as HTMLInputElement;
    this.customColor = input.value;
    if (this.isCustomColor) {
      this.selectedColor = this.customColor;
      this.colorChanged.emit(this.selectedColor);
    }
  }

  switchIconMode(mode: string) {
    this.iconMode.set(mode);
    this.emitIconChange();
  }

  handleImageUpload(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        this.imagePreview = e.target?.result as string;
        this.emitIconChange();
      };
      reader.readAsDataURL(file);
    }
  }

  onEmojiInput() {
    const val = this.emojiValue.trim();
    if (!val) {
      this.emitIconChange();
      return;
    }
    const emojiRegex = /\p{Extended_Pictographic}/u;
    if (emojiRegex.test(val)) {
      this.emojiValue = Array.from(val)[0];
    } else {
      this.emojiValue = '';
    }
    this.emitIconChange();
  }

  onSvgInput() {
    this.emitIconChange();
  }

  emitIconChange() {
    let data: string | null = null;
    const mode = this.iconMode();
    if (mode === 'BLOB') {
      data = this.imagePreview;
    } else if (mode === 'EMOJI') {
      data = this.emojiValue || null;
    } else if (mode === 'SVG') {
      data = this.svgValue || null;
    }
    this.iconChanged.emit({ type: mode, data });
  }
}
