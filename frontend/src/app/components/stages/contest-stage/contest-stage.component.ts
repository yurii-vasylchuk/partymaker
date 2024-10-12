import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  OnInit,
  output,
  signal
} from '@angular/core';
import {GamePlayer, Principal, StageDescriptor} from '../../../commons/dto';
import {ContestActions, ContestStageContext, ContestTask, ContestUserTaskLink, PlacementsSetAction} from './dto';
import {FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {StageHeadingComponent} from '../stage-heading/stage-heading.component';
import {fullName} from '../../../commons/functions';
import {PlayersListComponent} from '../../players-list/players-list.component';

type DisplayTask = ContestTask & ContestUserTaskLink;
type PlacesForm = FormGroup<{
  places: FormArray<FormControl<number | null>>;
}>;

@Component({
  selector: 'brstg-contest-stage',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    StageHeadingComponent,
    PlayersListComponent,
  ],
  templateUrl: './contest-stage.component.html',
  styleUrl: './contest-stage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContestStageComponent implements OnInit {
  stage = input.required<StageDescriptor>();
  principal = input.required<Principal>();
  act = output<ContestActions>();
  protected ctx = computed<ContestStageContext>(() => this.stage().ctx as ContestStageContext);
  protected currentTask = computed<DisplayTask>(() => {
    const ctx = this.ctx();

    return {
      ...ctx.availableTasks[ctx.tasksDistribution[ctx.currentTaskIdx].taskIdx],
      ...ctx.tasksDistribution[ctx.currentTaskIdx],
    };
  });
  protected currentTaskPlayers = computed<GamePlayer[]>(() => this.ctx().players.filter(p => (this.currentTask()?.usersIds ?? []).includes(p.userId)));
  protected isPlayerReady = computed(() => {
    const ctx = this.ctx();
    const playerId = this.principal().id;

    return ctx.playersReadiness[playerId];
  });
  protected loaded = signal(false);
  protected readonly fullName = fullName;
  readonly #fb = inject(FormBuilder);
  protected form: PlacesForm = this.#fb.group({
    places: this.#fb.array<number>([]),
  });

  constructor() {
    effect(() => {
      const ctx = this.ctx();
      if (ctx == null) {
        return;
      }

      if (this.form.controls.places.length == ctx.winningPlaces) {
        return;
      } else if (this.form.controls.places.length > ctx.winningPlaces) {
        for (let i = this.form.controls.places.length - 1; i >= (ctx.winningPlaces); i--) {
          this.form.controls.places.removeAt(i);
        }
      } else {
        for (let i = this.form.controls.places.length; i < ctx.winningPlaces; i++) {
          this.form.controls.places.push(
            this.#fb.control<number | null>(ctx.placements[this.principal().id][i + 1], Validators.required),
            {emitEvent: false},
          );
        }
      }

      this.loaded.set(true);
    }, {allowSignalWrites: true});

    effect(() => {
      const isPlayerReady = this.isPlayerReady();

      if (isPlayerReady && !this.form.disabled) {
        this.form.disable({emitEvent: false});
      } else if (!isPlayerReady && this.form.disabled) {
        this.form.enable({emitEvent: false});
      }
    });

    this.form.valueChanges.subscribe(form => this.onFormEdited(form.places));
  }

  ngOnInit() {
    this.form.setValue({
      places: []
    }, {emitEvent: false});
  }

  protected range(to: number): number[] {
    let res: number[] = [];
    for (let i = 0; i < to; i++) {
      res.push(i);
    }
    return res;
  }

  protected handleReadyClicked() {
    this.act.emit({
      type: 'READY',
    });
  }

  private onFormEdited(places?: (number | null)[]) {
    let payload: PlacementsSetAction['placements'] = {};

    if (places == null) {
      console.warn('Places form value is null');
      return;
    }

    for (let i = 0; i < places.length; i++) {
      payload[i + 1] = places[i];
    }

    this.act.emit({
      type: 'PLACEMENTS_SET',
      placements: payload,
    });
  }
}
