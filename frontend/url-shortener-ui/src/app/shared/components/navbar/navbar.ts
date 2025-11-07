import { Component } from '@angular/core';
  import { CommonModule } from '@angular/common';
  import { MatToolbarModule } from '@angular/material/toolbar';
  import { MatButtonModule } from '@angular/material/button';
  import { MatIconModule } from '@angular/material/icon';
  import { MatMenuModule } from '@angular/material/menu';
  import { AuthService } from '../../../core/services/auth.service';
import { RouterLink } from '@angular/router';

  @Component({
    selector: 'app-navbar',
    standalone: true,
    imports: [
      CommonModule,
      MatToolbarModule,
      MatButtonModule,
      MatIconModule,
      MatMenuModule,
      RouterLink
    ],
    templateUrl: './navbar.html',
    styleUrls: ['./navbar.scss']
  })

  export class NavBar {
    constructor(public authService : AuthService) {}

    onLogout():void {
        this.authService.logout();
    }
  } 