import {ChangeDetectionStrategy, Component, input} from '@angular/core';
import {GamePlayer} from '../../commons/dto';
import {fullName} from '../../commons/functions';

@Component({
  selector: 'brstg-players-list',
  standalone: true,
  imports: [],
  templateUrl: './players-list.component.html',
  styleUrl: './players-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlayersListComponent {
  players = input.required<GamePlayer[]>();
  title = input<string>('Гравці')
  protected readonly fullName = fullName;
}
