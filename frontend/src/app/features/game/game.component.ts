import {ChangeDetectionStrategy, Component, computed, inject, input, OnInit} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {GameService} from './game.service';
import {AuthenticationService} from '../../service/authentication.service';
import {filter, take} from 'rxjs';
import {RegistrationStageComponent} from '../../components/stages/registration-stage/registration-stage.component';
import {AsyncAction} from '../../commons/dto';
import {ScoresComponent} from '../../components/scores/scores.component';
import {GuessStageComponent} from '../../components/stages/guess-stage/guess-stage.component';
import {ContestStageComponent} from '../../components/stages/contest-stage/contest-stage.component';
import {TournamentStageComponent} from '../../components/stages/tournament-stage/tournament-stage.component';

@Component({
  selector: 'brstg-game',
  standalone: true,
  imports: [
    RegistrationStageComponent,
    ScoresComponent,
    GuessStageComponent,
    ContestStageComponent,
    TournamentStageComponent,
  ],
  templateUrl: './game.component.html',
  styleUrl: './game.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameComponent implements OnInit {
  //Should be passed as path parameter
  protected readonly token = input('token');
  readonly #gameService = inject(GameService);
  protected game = toSignal(this.#gameService.game$);
  readonly #authenticationService = inject(AuthenticationService);
  protected principal = toSignal(this.#authenticationService.principal$);
  protected isJoined = computed(() => this.game() && this.game()!.players.some(p => p.userId == this.#authenticationService.principal?.id));

  ngOnInit() {
    this.#authenticationService.exchangeAuthToken(this.token());
    this.#authenticationService.jwt$
      .pipe(
        filter(jwt => jwt != null),
        take(1),
      ).subscribe(_ => this.#gameService.connect());

    this.#gameService.game$.subscribe(console.log);
  }

  join() {
    this.#gameService.join();
  }

  handleStageAct(action: AsyncAction) {
    console.log(action);
    this.#gameService.act(action);
  }
}
