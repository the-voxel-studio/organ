import { Component, signal, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { gsap } from 'gsap';
import { MorphSVGPlugin } from 'gsap/MorphSVGPlugin';

// Enregistrement du plugin (désormais public)
gsap.registerPlugin(MorphSVGPlugin);

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly title = signal('front-angular');

  ngOnInit() {
    console.log('GSAP MorphSVGPlugin chargé avec succès !');
  }
}
