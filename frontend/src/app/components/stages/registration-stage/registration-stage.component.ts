import {ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal} from '@angular/core';
import {Principal, StageDescriptor} from '../../../commons/dto';
import {RegistrationAction, RegistrationStageContext} from './dto';
import {environment} from '../../../environment/environment';
import {FormBuilder, FormControl, ReactiveFormsModule} from '@angular/forms';
import {debounceTime, filter} from 'rxjs';
import {StageHeadingComponent} from '../stage-heading/stage-heading.component';

type TraitOptionDescriptor = {
  trait: string;
  category: string;
}

type TraitDisplayDescriptor = {
  category: string;
  traits: TraitOptionDescriptor[];
  fc: FormControl<TraitOptionDescriptor | null>;
};

@Component({
  selector: 'brstg-registration-stage',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    StageHeadingComponent,
  ],
  templateUrl: './registration-stage.component.html',
  styleUrl: './registration-stage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegistrationStageComponent {
  stage = input.required<StageDescriptor>();
  player = input.required<Principal>();
  act = output<RegistrationAction>();
  protected ctx = computed(() => this.stage().ctx as RegistrationStageContext);
  protected avatarsAvailabilityMap = computed(() => {
    const ctx = this.ctx();
    const user = this.player();

    const result = new Map<string, 'YOU' | 'ANOTHER'>;

    if (user == null) {
      return result;
    }

    for (let playerId in ctx.chosenAvatars) {
      result.set(ctx.chosenAvatars[playerId], Number.parseInt(playerId) == user.id ? 'YOU' : 'ANOTHER');
    }

    return result;
  });
  protected traitsDisplay = signal<TraitDisplayDescriptor[]>([]);
  protected isPlayerReady = computed(() => {
    const ctx = this.ctx();
    const principal = this.player();

    return ctx.playersReadiness[principal.id] ?? false;
  });
  protected readonly environment = environment;
  readonly #fb = inject(FormBuilder);
  protected nicknameFc = this.#fb.control<string | null>(null);
  protected traitsFc = this.#fb.array<TraitOptionDescriptor | null>([]);

  constructor() {
    effect(() => {
      const id = this.player().id;

      if (id == null) {
        return;
      }

      const chosenNickname = this.ctx()?.chosenNicknames?.[id];
      if (chosenNickname == null) {
        return;
      }

      this.nicknameFc.setValue(chosenNickname, {emitEvent: false});
    });

    effect(() => {
      const availableTraits = this.ctx().availableTraits;
      const availableCategories = Object.keys(availableTraits);

      const chosenTraits = this.ctx().chosenTraits?.[this.player().id] ?? {};

      this.traitsDisplay.update(prev => prev.filter(td => {
        const found = availableCategories.includes(td.category);
        if (!found) {
          const fcIdx = this.traitsFc.controls.indexOf(td.fc);
          this.traitsFc.removeAt(fcIdx, {emitEvent: false});
        }
        return found;
      }));

      availableCategories.forEach(category => {
        const chosenTrait = chosenTraits?.[category];
        const value: TraitOptionDescriptor | null = chosenTrait != null ? {trait: chosenTrait, category} : null;

        const existent = this.traitsDisplay().find(td => td.category === category);

        if (existent == null) {
          this.traitsDisplay.update(prev => {
            const control = this.#fb.control(value);
            this.traitsFc.push(control, {emitEvent: false});

            return [
              ...prev,
              {
                traits: availableTraits[category].map(trait => ({trait, category})),
                fc: control,
                category: category,
              },
            ];
          });
        } else {
          if (existent.fc.value?.trait !== value?.trait) {
            existent.fc.setValue(value, {emitEvent: false});
          }
        }

      });
    }, {allowSignalWrites: true});

    effect(() => {
      if (this.isPlayerReady()) {
        this.traitsFc.controls.forEach(control => control.disable({emitEvent: false}));
        this.nicknameFc.disable({emitEvent: false});
      }
    });

    this.nicknameFc.valueChanges
      .pipe(
        filter(n => n != null),
        debounceTime(500),
      )
      .subscribe(nickname => this.handleNicknameChanged(nickname));

    this.traitsFc.valueChanges.subscribe(tr => {
      this.handleTraitsSelected(tr);
    });
  }

  traitsComparator: (o1: TraitOptionDescriptor, o2: TraitOptionDescriptor) => boolean = (o1, o2) => o1?.trait === o2?.trait;

  handleAvatarClick(avatar: string) {
    this.act.emit({
      type: 'CHOOSE_AVATAR',
      avatar: avatar,
    });
  }

  handleReadyClick() {
    this.act.emit({
      type: "READY"
    });
  }

  private handleNicknameChanged(nickname: string) {
    this.act.emit({
      type: 'CHOOSE_NICKNAME',
      nickname,
    });
  }

  private handleTraitsSelected(traits: (TraitOptionDescriptor | null)[]) {
    console.log(traits);
    let payload: { [category: string]: string } = {};

    traits.filter(t => t != null).forEach(t => payload[t.category] = t.trait);

    this.act.emit({
      type: 'CHOOSE_TRAITS',
      traits: payload,
    });
  }
}
