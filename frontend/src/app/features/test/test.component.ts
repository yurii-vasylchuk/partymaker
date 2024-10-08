import {ChangeDetectionStrategy, Component, input} from '@angular/core';

@Component({
  selector: 'brstg-test',
  standalone: true,
  imports: [],
  templateUrl: './test.component.html',
  styleUrl: './test.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TestComponent {

  protected token = input(null);

  constructor() {
  }
}
