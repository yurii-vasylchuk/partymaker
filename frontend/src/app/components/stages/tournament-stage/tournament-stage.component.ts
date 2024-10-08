import {ChangeDetectionStrategy, Component, computed, input, output} from '@angular/core';
import {GamePlayer, Principal, StageDescriptor} from '../../../commons/dto';
import {ContestActions} from '../contest-stage/dto';
import {CompetitorTask, TournamentContext} from './dto';
import {StageHeadingComponent} from '../stage-heading/stage-heading.component';
import {environment} from '../../../environment/environment';
import {PlayersListComponent} from '../../players-list/players-list.component';

@Component({
  selector: 'brstg-tournament-stage',
  standalone: true,
  imports: [
    StageHeadingComponent,
    PlayersListComponent,
  ],
  templateUrl: './tournament-stage.component.html',
  styleUrl: './tournament-stage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TournamentStageComponent {
  stage = input.required<StageDescriptor>();
  principal = input.required<Principal>();
  act = output<ContestActions>();
  protected readonly environment = environment;
  protected ctx = computed(() => this.stage().ctx as TournamentContext);
  protected currentMatch = computed(() => {
    const ctx = this.ctx();
    return ctx.matches[ctx.currentMatchIdx];
  });
  protected playerTask = computed<CompetitorTask | null>(() => {
    const currentMatch = this.currentMatch();
    const principal = this.principal();

    if (!currentMatch.competitors.includes(principal.id)) {
      return null;
    }

    if (currentMatch.type === 'SAME_FOR_ALL') {
      return currentMatch.competitorsTasks[0];
    }

    return currentMatch.competitorsTasks[currentMatch.competitors.indexOf(principal.id)];
  });

  playerById(id: number): GamePlayer | undefined {
    return this.ctx().players.find(p => p.userId == id);
  }

  playersByIds(competitors: number[]): GamePlayer[] {
    if (competitors == null) {
      return [];
    }

    return competitors.map(id => this.playerById(id)).filter(v => v != null);
  }
}
