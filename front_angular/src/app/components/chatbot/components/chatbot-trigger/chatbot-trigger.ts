import { Component, Input, Output, EventEmitter, ViewEncapsulation } from '@angular/core';

@Component({
  selector: 'app-chatbot-trigger',
  standalone: true,
  templateUrl: './chatbot-trigger.html',
  styleUrl: './chatbot-trigger.css',
  encapsulation: ViewEncapsulation.None
})
export class ChatbotTriggerComponent {
  @Input() isOpen = false;
  @Output() toggle = new EventEmitter<void>();

  onToggle() {
    this.toggle.emit();
  }
}
