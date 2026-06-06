import { Component, signal, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
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

  ngOnInit() {
    console.log('GSAP MorphSVGPlugin chargé avec succès !');
  }
}
