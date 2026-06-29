import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, switchMap } from 'rxjs';
import { ChatbotActionExecutorService } from './chatbot-action-executor.service';
import { ToastService } from '../common/toast.service';

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface ChatbotResponse {
  message: string;
  actions: Array<{
    type: 'navigate' | 'create_project' | 'create_organ' | 'create_role' | 'create_task' | 'update_task' | 'copy_role' | 'paste_role';
    id?: string;
    path?: string;
    projectUuid?: string;
    organUuid?: string;
    roleUuid?: string;
    taskUuid?: string;
    payload?: any;
  }> | null;
}

@Injectable({
  providedIn: 'root'
})
export class ChatbotService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private executor = inject(ChatbotActionExecutorService);
  private toastService = inject(ToastService);

  private conversationHistory: ChatMessage[] = [];

  // État de la modale de tâche (utilisé par le chatbot pour adapter sa position)
  isTaskModalOpen = signal(false);

  // Toggle permettant d'activer/désactiver l'exécution automatique des actions par l'IA
  actionsEnabled = signal(true);

  constructor() {
    this.resetChat();
  }

  // Facade getters/setters pour le staging et les triggers délégués à l'executeur
  get stagedProjectData() { return this.executor.stagedProjectData; }
  set stagedProjectData(val) { this.executor.stagedProjectData = val; }

  get stagedOrganData() { return this.executor.stagedOrganData; }
  set stagedOrganData(val) { this.executor.stagedOrganData = val; }

  get stagedTaskData() { return this.executor.stagedTaskData; }
  set stagedTaskData(val) { this.executor.stagedTaskData = val; }

  get stagedTaskEditUuid() { return this.executor.stagedTaskEditUuid; }
  set stagedTaskEditUuid(val) { this.executor.stagedTaskEditUuid = val; }

  get taskCreateTrigger$() { return this.executor.taskCreateTrigger$; }
  get taskEditTrigger$() { return this.executor.taskEditTrigger$; }

  resetChat(): void {
    this.conversationHistory = [];
  }

  getHistory(): ChatMessage[] {
    return this.conversationHistory;
  }

  addSystemResponse(content: string): void {
    this.conversationHistory.push({ role: 'assistant', content });
  }

  sendMessage(userMessage: string): Observable<ChatbotResponse> {
    this.conversationHistory.push({ role: 'user', content: userMessage });

    // Extraire la commande slash
    let command = 'ask';
    let cleanMessage = userMessage;

    if (userMessage.startsWith('/create')) {
      command = 'create';
      cleanMessage = userMessage.substring(7).trim();
    } else if (userMessage.startsWith('/edit')) {
      command = 'edit';
      cleanMessage = userMessage.substring(5).trim();
    } else if (userMessage.startsWith('/role')) {
      command = 'role';
      cleanMessage = userMessage.substring(5).trim();
    } else if (userMessage.startsWith('/ask')) {
      command = 'ask';
      cleanMessage = userMessage.substring(4).trim();
    }

    const payload = {
      history: this.conversationHistory,
      context: {
        currentRoute: this.router.url,
        command: command,
        cleanMessage: cleanMessage,
        actionsEnabled: this.actionsEnabled()
      }
    };

    return this.http.post<ChatbotResponse>('/api/chatbot/message', payload).pipe(
      switchMap(async response => {
        this.conversationHistory.push({ role: 'assistant', content: response.message });

        if (response.actions && response.actions.length > 0) {
          if (this.actionsEnabled()) {
            try {
              await this.executor.executeActions(response.actions);
            } catch (err) {
              console.error('Failed to execute chatbot actions', err);
            }
          } else {
            this.toastService.info('Actions ignorées', "L'exécution automatique des actions est désactivée.");
          }
        }

        return response;
      })
    );
  }
}
