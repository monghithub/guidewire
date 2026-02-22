import { Injectable, signal, computed } from '@angular/core';
import { ApiCallRecord } from '../models';

@Injectable({ providedIn: 'root' })
export class ApiLogService {
  private readonly _calls = signal<ApiCallRecord[]>([]);
  readonly calls = this._calls.asReadonly();
  readonly lastCall = computed(() => {
    const all = this._calls();
    return all.length > 0 ? all[all.length - 1] : null;
  });

  log(record: ApiCallRecord): void {
    this._calls.update(calls => [...calls.slice(-99), record]);
  }

  clear(): void {
    this._calls.set([]);
  }
}
