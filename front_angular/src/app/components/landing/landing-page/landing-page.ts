import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LandingHeroComponent } from '../landing-hero/landing-hero';
import { LandingFeaturesComponent } from '../landing-features/landing-features';
import { LandingCollaborationComponent } from '../landing-collaboration/landing-collaboration';
import { LandingUseCasesComponent } from '../landing-use-cases/landing-use-cases';
import { LandingAboutComponent } from '../landing-about/landing-about';
import { LandingSocialProofComponent } from '../landing-social-proof/landing-social-proof';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    CommonModule,
    LandingHeroComponent,
    LandingFeaturesComponent,
    LandingCollaborationComponent,
    LandingUseCasesComponent,
    LandingAboutComponent,
    LandingSocialProofComponent
  ],
  templateUrl: './landing-page.html'
})
export class LandingPageComponent {}
