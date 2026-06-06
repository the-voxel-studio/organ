import { Component, signal, OnInit, inject, computed } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { gsap } from 'gsap';
import { MorphSVGPlugin } from 'gsap/MorphSVGPlugin';
import { ToastService } from './services/common/toast.service';
import { AuthService } from './services/api/auth.service';
import { ChatbotComponent } from './components/chatbot/chatbot';
import { ToastComponent } from './components/toast/toast';


// Enregistrement du plugin (désormais public)
gsap.registerPlugin(MorphSVGPlugin);

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ChatbotComponent, ToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly title = signal('front-angular');
  protected toastService = inject(ToastService);
  protected authService = inject(AuthService);
  private router = inject(Router);
  private currentUrl = signal('');

  protected showChatbot = computed(() => {
    if (!this.authService.isAuthenticated()) {
      return false;
    }
    const publicRoutes = ['/', '/get-the-app', '/legal-notice', '/privacy-policy', '/terms', '/login', '/register'];
    const currentPath = this.currentUrl().split('#')[0].split('?')[0];
    return !publicRoutes.includes(currentPath);
  });

  ngOnInit() {
    console.log('GSAP MorphSVGPlugin chargé avec succès !');
    
    this.currentUrl.set(this.router.url);
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.currentUrl.set(this.router.url);
    });
  }
}
