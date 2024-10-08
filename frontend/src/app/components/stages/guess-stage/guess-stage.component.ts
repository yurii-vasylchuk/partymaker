import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  OnInit,
  output,
  signal,
} from '@angular/core';
import {GamePlayer, Principal, StageDescriptor} from '../../../commons/dto';
import {GuessActions, GuessChangedAction, GuessStageContext} from './dto';
import {environment} from '../../../environment/environment';
import {KeyValuePipe} from '@angular/common';
import {FormBuilder, FormControl, ReactiveFormsModule} from '@angular/forms';
import {StageHeadingComponent} from '../stage-heading/stage-heading.component';

type PlayerMatch = {
  profileId: number;
  userId: number | null;
};

type DisplayOption = {
  control: FormControl<PlayerMatch | null>;
  profile: GamePlayer;
};

@Component({
  selector: 'brstg-guess-stage',
  standalone: true,
  imports: [
    KeyValuePipe,
    ReactiveFormsModule,
    StageHeadingComponent,
  ],
  templateUrl: './guess-stage.component.html',
  styleUrl: './guess-stage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GuessStageComponent implements OnInit {
  stage = input.required<StageDescriptor>();
  principal = input.required<Principal>();
  act = output<GuessActions>();
  protected ctx = computed(() => this.stage().ctx as GuessStageContext);
  protected isPlayerReady = computed<boolean>(() => this.ctx().playersReadiness[this.principal().id] ?? false);
  protected displayOptions = signal<DisplayOption[]>([]);
  protected readonly environment = environment;
  readonly #fb = inject(FormBuilder);
  protected guessesFc = this.#fb.array<PlayerMatch>([]);

  constructor() {
    effect(() => {
      const ctx = this.ctx();
      const currentUserId = this.principal().id;
      this.displayOptions.update(options => {
        const next: DisplayOption[] = [];

        for (const displayOption of options) {
          if (ctx.players.some(p => p.userId == displayOption.profile.userId)) {
            // DisplayOption is present as well as player
            // Try to update FC value, keep DisplayOption

            const guessedId = ctx.guesses?.[currentUserId]?.[displayOption.profile.userId] ?? null;

            displayOption.control.setValue({
              profileId: displayOption.profile.userId,
              userId: guessedId,
            }, {emitEvent: false});

            next.push(displayOption);
          } else {
            // Display option is present, but player isn't
            // Removing FC
            console.warn("This should never happen");

            this.guessesFc.removeAt(this.guessesFc.controls.indexOf(displayOption.control), {emitEvent: false});
          }
        }

        ctx.players.forEach(player => {
          if (!next.some(displayOption => player.userId === displayOption.profile.userId)) {
            // DisplayOption is not found for particular player
            const guessed = ctx.guesses?.[currentUserId]?.[player.userId] ?? null;
            const control = this.#fb.control<PlayerMatch>({profileId: player.userId, userId: guessed});
            this.guessesFc.push(control, {emitEvent: false});
            next.push({
              profile: player,
              control,
            });
          }
        });

        return next;
      });

    }, {allowSignalWrites: true});
    effect(() => {
      if (this.isPlayerReady() && this.guessesFc.enabled) {
        this.guessesFc.disable({emitEvent: false});
      } else if (!this.isPlayerReady() && this.guessesFc.disabled) {
        this.guessesFc.enable({emitEvent: false});
      }
    });
  }

  ngOnInit(): void {
    this.guessesFc.valueChanges.subscribe(guesses => {
      let payload: GuessChangedAction['guesses'] = {};
      guesses.forEach(formValue => {
        if (formValue != null && formValue.userId != null) {
          payload[formValue.profileId] = formValue.userId;
        }
      });

      this.act.emit({
        type: 'GUESSES_CHANGED',
        guesses: payload,
      });
    });
  }

  handleReadyClick() {
    this.act.emit({
      type: 'READY',
    });
  }

  protected displayOptionComparator = (pm1: PlayerMatch, pm2: PlayerMatch) => {
    return pm1.userId === pm2.userId && pm1.profileId === pm2.profileId;
  };
}
