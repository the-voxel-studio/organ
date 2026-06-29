import { Component, inject, ViewEncapsulation } from '@angular/core';
import { ChatbotTriggerComponent } from './components/chatbot-trigger/chatbot-trigger';
import { ChatbotPanelComponent } from './components/chatbot-panel/chatbot-panel';
import { ChatbotService } from '../../services/chatbot/chatbot.service';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [ChatbotTriggerComponent, ChatbotPanelComponent],
  templateUrl: './chatbot.html',
  styleUrl: './chatbot.css',
  encapsulation: ViewEncapsulation.None
})
export class ChatbotComponent {
  private chatbotService = inject(ChatbotService);
  isOpen = false;

  get isTaskModalOpen(): boolean {
    return this.chatbotService.isTaskModalOpen();
  }

  toggleChat() {
    this.isOpen = !this.isOpen;
  }
}
