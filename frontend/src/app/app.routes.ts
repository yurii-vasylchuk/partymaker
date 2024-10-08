import {Routes} from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/index/index.component').then(mod => mod.IndexComponent),
  },
  {
    path: 'game',
    loadComponent: () => import('./features/game/game.component').then(mod => mod.GameComponent),
  },
  {
    path: 'admin',
    loadComponent: () => import('./features/admin/admin.component').then(mod => mod.AdminComponent),
  },
  {
    path: 'test',
    loadComponent: () => import('./features/test/test.component').then(mod => mod.TestComponent),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
