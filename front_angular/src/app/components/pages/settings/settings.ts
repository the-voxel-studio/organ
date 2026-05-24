import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

// Sub-components
import { SettingsProfileComponent } from './components/settings-profile/settings-profile';
import { SettingsPasswordComponent } from './components/settings-password/settings-password';
import { SettingsGoogleLinkComponent } from './components/settings-google-link/settings-google-link';
import { SettingsConnectionsComponent } from './components/settings-connections/settings-connections';
import { SettingsDangerZoneComponent } from './components/settings-danger-zone/settings-danger-zone';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    SettingsProfileComponent,
    SettingsPasswordComponent,
    SettingsGoogleLinkComponent,
    SettingsConnectionsComponent,
    SettingsDangerZoneComponent
  ],
  templateUrl: './settings.html'
})
export class SettingsComponent {}
