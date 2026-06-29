import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ChatbotMarkdownService {
  /**
   * Retire les marqueurs markdown d'un texte
   */
  stripMarkdown(text: string): string {
    if (!text || typeof text !== 'string') return text;
    return text
      // En-têtes : # Header
      .replace(/^#{1,6}\s+(.*)$/gim, '$1')
      // Gras/Italique : **text**, *text*, __text__, _text_
      .replace(/(\*\*|__)(.*?)\1/g, '$2')
      .replace(/(?<=^|\s)(\*|_)(?=\S)(.*?)(?<=\S)\1(?=$|\s|[.,;:!?])/g, '$2')
      // Barré : ~~text~~
      .replace(/~~(.*?)~~/g, '$1')
      // Code en ligne : `code`
      .replace(/`(.*?)`/g, '$1')
      // Blocs de code : ```js ... ```
      .replace(/```[a-z]*\n([\s\S]*?)\n```/g, '$1')
      // Liens : [text](url)
      .replace(/\[(.*?)\]\((.*?)\)/g, '$1')
      // Images : ![alt](url)
      .replace(/!\[(.*?)\]\((.*?)\)/g, '$1')
      // Citations : > quote
      .replace(/^\s*>\s+(.*)$/gim, '$1')
      // Listes à puces : - item, * item, 1. item
      .replace(/^\s*[-*+]\s+(.*)$/gim, '$1')
      .replace(/^\s*\d+\.\s+(.*)$/gim, '$1')
      // Multi-sauts de ligne
      .replace(/\n{2,}/g, '\n\n')
      .trim();
  }

  /**
   * Nettoie uniquement les champs textuels utilisateur (title, description, name) en retirant le markdown
   */
  cleanPayloadMarkdown(obj: any): any {
    if (obj === null || obj === undefined) return obj;
    
    if (Array.isArray(obj)) {
      return obj.map(item => this.cleanPayloadMarkdown(item));
    }
    
    if (typeof obj === 'object') {
      const cleaned: any = {};
      for (const key of Object.keys(obj)) {
        if (key === 'title' || key === 'description' || key === 'name') {
          if (typeof obj[key] === 'string') {
            cleaned[key] = this.stripMarkdown(obj[key]);
          } else {
            cleaned[key] = obj[key];
          }
        } else {
          cleaned[key] = this.cleanPayloadMarkdown(obj[key]);
        }
      }
      return cleaned;
    }
    
    return obj;
  }
}
