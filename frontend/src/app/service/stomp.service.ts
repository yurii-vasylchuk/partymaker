import {Injectable} from '@angular/core';
import {map, Observable} from 'rxjs';
import {AuthenticationService} from './authentication.service';
import {RxStomp} from '@stomp/rx-stomp';
import {AsyncEvent} from '../commons/dto';
import {environment} from '../environment/environment';


@Injectable({
  providedIn: 'root',
})
export class StompService {
  private readonly client: RxStomp;

  constructor(private auth: AuthenticationService) {
    this.client = new RxStomp();
    this.client.configure({
      brokerURL: environment.apiUrl + '/ws',
      logRawCommunication: false,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.auth.jwt$.subscribe(async jwt => {
      if (this.client.connected()) {
        await this.client.deactivate();
      }

      if (jwt != null) {
        this.client.configure({
          connectHeaders: {
            Authorization: `Bearer ${jwt}`,
          },
        });

        this.client.activate();
      }
    });
  }


  subscribe(destination: string): Observable<AsyncEvent> {
    let headers = {};

    const jwt = this.auth.jwt;
    if (jwt != null) {
      headers = {Authorization: `Bearer ${jwt}`};
    }

    return this.client.watch(destination, headers)
      .pipe(
        map(msg => (JSON.parse(msg.body) as AsyncEvent)),
      );
  }

  send(destination: string, body: any): void {
    if (!this.client.connected()) {
      throw new Error("Not connected");
    }

    let headers = {};

    const jwt = this.auth.jwt;
    if (jwt != null) {
      headers = {Authorization: `Bearer ${jwt}`};
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body),
      headers,
    });
  }
}
