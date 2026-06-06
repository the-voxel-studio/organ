import { Component, inject, ElementRef, ViewChild, Input, Output, EventEmitter, OnChanges, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ChatbotService, ChatMessage } from '../../../../services/chatbot/chatbot.service';

@Component({
  selector: 'app-chatbot-panel',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './chatbot-panel.html',
  styleUrl: './chatbot-panel.css',
  encapsulation: ViewEncapsulation.None
})
export class ChatbotPanelComponent implements OnChanges {
  private chatbotService = inject(ChatbotService);
  private sanitizer = inject(DomSanitizer);

  @Input() isOpen = false;
  @Output() toggle = new EventEmitter<void>();

  @ViewChild('chatHistoryContainer') private chatHistoryContainer!: ElementRef;
  @ViewChild('chatInput') private chatInput!: ElementRef;

  inputValue = '';
  isLoading = false;
  activeCommand = 'ask';
  showHelp = false;
  private renderedHtmlCache = new Map<string, SafeHtml>();

  get messages(): ChatMessage[] {
    return this.chatbotService.getHistory();
  }

  get actionsEnabled(): boolean {
    return this.chatbotService.actionsEnabled();
  }

  set actionsEnabled(val: boolean) {
    this.chatbotService.actionsEnabled.set(val);
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['isOpen'] && changes['isOpen'].currentValue === true) {
      setTimeout(() => {
        this.scrollToBottom();
        this.focusInput();
      }, 50);
    }
  }

  toggleChat() {
    this.toggle.emit();
  }

  toggleHelp() {
    this.showHelp = !this.showHelp;
  }

  insertCommand(cmd: string) {
    const cleanCmd = cmd.replace('/', '').trim();
    if (cleanCmd === 'create' || cleanCmd === 'edit' || cleanCmd === 'role' || cleanCmd === 'ask') {
      this.activeCommand = cleanCmd;
    }
    setTimeout(() => {
      this.focusInput();
    }, 30);
  }

  onInputChange() {
    const text = this.inputValue;
    if (text.startsWith('/create ')) {
      this.activeCommand = 'create';
      this.inputValue = text.substring(8);
    } else if (text.startsWith('/edit ')) {
      this.activeCommand = 'edit';
      this.inputValue = text.substring(6);
    } else if (text.startsWith('/role ')) {
      this.activeCommand = 'role';
      this.inputValue = text.substring(6);
    } else if (text.startsWith('/ask ')) {
      this.activeCommand = 'ask';
      this.inputValue = text.substring(5);
    }
  }

  onCommandChange() {
    this.focusInput();
  }

  resetChat() {
    this.chatbotService.resetChat();
    this.renderedHtmlCache.clear();
    this.showHelp = false;
    this.activeCommand = 'ask';
  }

  sendMessage() {
    const text = this.inputValue.trim();
    if (!text || this.isLoading) {
      return;
    }

    const prefix = this.activeCommand !== 'ask' ? `/${this.activeCommand} ` : '';
    const formattedText = prefix + text;

    this.inputValue = '';
    this.activeCommand = 'ask';
    this.showHelp = false;
    this.isLoading = true;

    setTimeout(() => this.scrollToBottom(), 30);

    this.chatbotService.sendMessage(formattedText).subscribe({
      next: () => {
        this.isLoading = false;
        setTimeout(() => {
          this.scrollToBottom();
          this.focusInput();
        }, 50);
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur chatbot:', err);
        
        let errorMsg = "Désolé, je rencontre une difficulté temporaire pour contacter mon serveur de réflexion.";
        if (err.error && typeof err.error === 'object' && err.error.message) {
          errorMsg = err.error.message;
        } else if (err.status === 502 || err.status === 504) {
          errorMsg = "Impossible de contacter l'assistant virtuel (Erreur de réseau ou serveur injoignable).";
        } else if (err.status === 401) {
          errorMsg = "Session expirée. Veuillez vous reconnecter pour pouvoir échanger avec l'assistant.";
        }

        this.chatbotService.addSystemResponse(errorMsg);
        setTimeout(() => {
          this.scrollToBottom();
          this.focusInput();
        }, 50);
      }
    });
  }

  private scrollToBottom(): void {
    try {
      if (this.chatHistoryContainer) {
        const element = this.chatHistoryContainer.nativeElement;
        element.scrollTop = element.scrollHeight;
      }
    } catch (err) {
      // Silent catch
    }
  }

  private focusInput(): void {
    try {
      if (this.chatInput) {
        this.chatInput.nativeElement.focus();
      }
    } catch (err) {
      // Silent catch
    }
  }

  renderMarkdown(text: string): SafeHtml {
    if (!text) return '';
    if (this.renderedHtmlCache.has(text)) {
      return this.renderedHtmlCache.get(text)!;
    }

    let escaped = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');

    escaped = escaped.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    escaped = escaped.replace(/(?<=^|\s)\*(?=\S)([^*]+?)(?<=\S)\*(?=$|\s|[.,;:!?])/g, '<em>$1</em>');
    escaped = escaped.replace(/(?<=^|\s)_(?=\S)([^_]+?)(?<=\S)_(?=$|\s|[.,;:!?])/g, '<em>$1</em>');
    escaped = escaped.replace(/`(.*?)`/g, '<code class="px-1.5 py-0.5 bg-black/35 border border-white/10 rounded font-mono text-[11px] text-[#FF7DD4]">$1</code>');

    const lines = escaped.split('\n');
    let inList = false;
    const processedLines = lines.map(line => {
      const trimmed = line.trim();
      if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
        const content = trimmed.substring(2);
        if (!inList) {
          inList = true;
          return '<ul class="list-disc pl-5 my-1.5 flex flex-col gap-1"><li>' + content + '</li>';
        }
        return '<li>' + content + '</li>';
      } else {
        if (inList) {
          inList = false;
          return '</ul>' + line;
        }
        return line;
      }
    });
    
    if (inList) {
      processedLines.push('</ul>');
    }
    
    escaped = processedLines.join('\n');
    escaped = escaped.replace(/\n/g, '<br>');

    const safeHtml = this.sanitizer.bypassSecurityTrustHtml(escaped);
    this.renderedHtmlCache.set(text, safeHtml);
    return safeHtml;
  }
}
