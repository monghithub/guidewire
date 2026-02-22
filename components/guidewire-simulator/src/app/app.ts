import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from './layout/sidebar/sidebar.component';
import { HeaderComponent } from './layout/header/header.component';
import { ToastComponent } from './shared/components/toast/toast.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, HeaderComponent, ToastComponent],
  template: `
    <div class="app-layout">
      <app-sidebar />
      <div class="main-area">
        <app-header />
        <main class="content">
          <router-outlet />
        </main>
      </div>
    </div>
    <app-toast />
  `,
  styles: [`
    .app-layout {
      display: flex;
      height: 100vh;
      overflow: hidden;
    }
    .main-area {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
    .content {
      flex: 1;
      overflow-y: auto;
      background: #f0f2f5;
    }
  `],
})
export class AppComponent {}
