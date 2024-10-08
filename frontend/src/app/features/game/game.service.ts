import {inject, Injectable} from '@angular/core';
import {Observable, Subject} from 'rxjs';
import {StompService} from '../../service/stomp.service';
import {GameState} from './dto';
import {AsyncAction, GameStateChangedEvent} from '../../commons/dto';

@Injectable({
  providedIn: 'root',
})
export class GameService {
  readonly #stomp = inject(StompService);

  readonly #game = new Subject<GameState>();

  get game$(): Observable<GameState> {
    return this.#game.asObservable();
  }

  connect() {
    this.#stomp.subscribe('/topic/game').subscribe(gameEvent => {
      if (gameEvent.type === 'GAME_STATE_CHANGED') {
        this.#game.next(gameEvent as GameStateChangedEvent as GameState);
      }
    });
  }

  join() {
    this.#stomp.send('/app/game/join', null);
  }

  act(action: AsyncAction) {
    this.#stomp.send('/app/game/act', action);
  }
}
