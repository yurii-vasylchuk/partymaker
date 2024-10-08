import {ChangeDetectionStrategy, Component, computed, inject, input, OnInit} from '@angular/core';
import {filter, take} from 'rxjs';
import {AuthenticationService} from '../../service/authentication.service';
import {AdminService} from './admin.service';
import {GameService} from '../game/game.service';
import {toSignal} from '@angular/core/rxjs-interop';
import {ScoresComponent} from '../../components/scores/scores.component';
import {ContestStageContext} from '../../components/stages/contest-stage/dto';
import {ChoseWinnerAction, GamePlayer} from '../../commons/dto';
import {PlayersListComponent} from '../../components/players-list/players-list.component';
import {TournamentContext} from '../../components/stages/tournament-stage/dto';
import {environment} from '../../environment/environment';
import {FormBuilder, ReactiveFormsModule} from '@angular/forms';


@Component({
  selector: 'brstg-admin',
  standalone: true,
  imports: [
    ScoresComponent,
    PlayersListComponent,
    ReactiveFormsModule,
  ],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminComponent implements OnInit {
  protected readonly environment = environment;
  protected token = input.required<string>();
  protected contestCtx = computed<ContestStageContext | null>(() => this.game()?.currentStage.ctx as ContestStageContext ?? null);
  protected contestCurrentPlayers = computed<GamePlayer[]>(() => {
    const ctx = this.contestCtx();
    if (ctx == null) {
      return [];
    }

    return ctx.players
      .filter(p => ctx.tasksDistribution[ctx.currentTaskIdx].usersIds.includes(p.userId));
  });
  protected tournamentCurrentMatch = computed(() => {
    const currentStage = this.game()?.currentStage;
    if (currentStage == null || currentStage?.type != 'TOURNAMENT') {
      return null;
    }
    const ctx = currentStage.ctx as TournamentContext;
    return ctx.matches[ctx.currentMatchIdx];
  });
  readonly #gameService = inject(GameService);
  protected game = toSignal(this.#gameService.game$);
  readonly #authenticationService = inject(AuthenticationService);
  readonly #adminService = inject(AdminService);
  readonly #fb = inject(FormBuilder);
  protected tournamentWinnerSelectorFc = this.#fb.control<number | null>(null);

  ngOnInit(): void {
    this.#authenticationService.exchangeAuthToken(this.token());
    this.#authenticationService.jwt$
      .pipe(
        filter(jwt => jwt != null),
        take(1),
      ).subscribe(_ => this.#gameService.connect());
    this.tournamentWinnerSelectorFc.valueChanges.pipe(
      filter(value => value != null),
    ).subscribe(matchWinner => {
      this.#adminService.adminAct({
        type: 'CHOSE_WINNER',
        winnerId: matchWinner
      } as ChoseWinnerAction);
    });
  }

  playerById(id: number | undefined): GamePlayer | undefined {
    if (id == undefined) {
      return undefined;
    }
    return this.game()?.players.find(p => p.userId == id);
  }

  handleNextMatchClick() {
    this.tournamentWinnerSelectorFc.setValue(null, {emitEvent: false});
    this.#adminService.adminAct({
      type: 'NEXT_MATCH',
    });
  }

  protected handleStartGameClicked() {
    this.#adminService.goToNextStage();
  }

  protected handleCompleteStageClicked() {
    this.#adminService.completeStage();
  }

  protected handleNextTaskClicked() {
    this.#adminService.adminAct({
      type: 'NEXT_TASK',
    });
  }

  protected handlePrevTaskClicked() {
    this.#adminService.adminAct({
      type: 'PREV_TASK',
    });
  }
}
