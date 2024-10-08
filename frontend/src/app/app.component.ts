import {Component, OnInit} from '@angular/core';
import {RouteConfigLoadEnd, RouteConfigLoadStart, Router, RouterOutlet} from '@angular/router';
import {CommonModule} from '@angular/common';
import {distinct, filter, map} from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  constructor(private router: Router) {
  }

  ngOnInit(): void {
    this.router.events
      .pipe(
        filter(e => e instanceof RouteConfigLoadStart || e instanceof RouteConfigLoadEnd),
        map(e => e instanceof RouteConfigLoadStart),
        distinct(),
      )
      .subscribe(isLoading => {
        let loadingOverlay = document.getElementById('loading-overlay');
        if (loadingOverlay == null) {
          console.warn('Loading overlay is not found');
          return;
        }

        let isHidden = loadingOverlay.style.display === 'none';

        if (isLoading && isHidden) {
          loadingOverlay.style.opacity = '1';
          setTimeout(() => loadingOverlay.style.display = 'flex', 700);
        } else if (!isLoading && !isHidden) {
          loadingOverlay.style.opacity = '0';
          setTimeout(() => loadingOverlay.style.display = 'none', 700);
        }
      });
  }
}
