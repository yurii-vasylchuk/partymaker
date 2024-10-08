import {ChangeDetectionStrategy, Component, computed, input} from '@angular/core';
import {GamePlayer} from '../../commons/dto';
import {GameState} from '../../features/game/dto';
import {environment} from '../../environment/environment';

type ScoresRecord = {
  player: GamePlayer;
  stageScore?: number;
  gameScore: number;
};

@Component({
  selector: 'brstg-scores',
  standalone: true,
  imports: [],
  templateUrl: './scores.component.html',
  styleUrl: './scores.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScoresComponent {
  stageName = input<string>();
  players = input.required<GamePlayer[]>();
  gameScores = input.required<GameState['scores']>();
  stageScores = input<GameState['lastStageScores']>();

  protected scores = computed<ScoresRecord[]>(() => {
    return (this.players().map(p => ({
      player: p,
      stageScore: this.stageScores()?.[p.userId] ?? undefined,
      gameScore: this.gameScores()[p.userId] ?? 0,
    })) ?? []).sort((s1, s2) => s2.gameScore - s1.gameScore);
  });
  protected readonly environment = environment;
}
