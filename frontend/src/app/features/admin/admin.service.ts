import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {StompService} from '../../service/stomp.service';
import {AsyncAction} from '../../commons/dto';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  readonly #http = inject(HttpClient);
  readonly #stomp = inject(StompService);

  goToNextStage() {
    this.#http.put('/api/game/go-to-next-stage', null)
      .subscribe();
  }

  completeStage() {
    this.#http.put('/api/game/complete-stage', null)
      .subscribe();
  }

  adminAct(action: AsyncAction) {
    this.#stomp.send('/app/game/admin-act', action);
  }
}
