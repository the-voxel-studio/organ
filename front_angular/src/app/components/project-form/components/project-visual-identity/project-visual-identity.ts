import { Component, Input, Output, EventEmitter, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-project-visual-identity',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './project-visual-identity.html'
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
    const segmenter = new Intl.Segmenter(undefined, { granularity: 'grapheme' });
    const segments = Array.from(segmenter.segment(val));
    const firstGrapheme = segments[0]?.segment;

    const emojiRegex = /\p{Extended_Pictographic}/u;
    if (firstGrapheme && emojiRegex.test(firstGrapheme)) {
      this.emojiValue = firstGrapheme;
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
