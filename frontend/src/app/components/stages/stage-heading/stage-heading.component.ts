import {ChangeDetectionStrategy, Component, input} from '@angular/core';

@Component({
  selector: 'brstg-stage-heading',
  standalone: true,
  imports: [],
  templateUrl: './stage-heading.component.html',
  styleUrl: './stage-heading.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StageHeadingComponent {
  name = input.required<string>();
  description = input.required<string>();

}
