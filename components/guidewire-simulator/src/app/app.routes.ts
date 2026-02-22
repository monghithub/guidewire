import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
  },
  {
    path: 'policies',
    loadComponent: () => import('./features/policies/policies.component').then(m => m.PoliciesComponent),
  },
  {
    path: 'claims',
    loadComponent: () => import('./features/claims/claims.component').then(m => m.ClaimsComponent),
  },
  {
    path: 'invoices',
    loadComponent: () => import('./features/invoices/invoices.component').then(m => m.InvoicesComponent),
  },
  {
    path: 'customers',
    loadComponent: () => import('./features/customers/customers.component').then(m => m.CustomersComponent),
  },
  {
    path: 'incidents',
    loadComponent: () => import('./features/incidents/incidents.component').then(m => m.IncidentsComponent),
  },
  {
    path: 'rules',
    loadComponent: () => import('./features/rules/rules.component').then(m => m.RulesComponent),
  },
  { path: '**', redirectTo: 'dashboard' },
];
