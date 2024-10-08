import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';
import {Principal} from '../commons/dto';

@Injectable({
  providedIn: 'root',
})
export class AuthenticationService {
  private readonly http = inject(HttpClient);
  #jwt?: string;
  #jwt$ = new BehaviorSubject<string | null>(null);

  #principal?: Principal;
  #principal$ = new BehaviorSubject<Principal | null>(null);

  get jwt(): string | undefined {
    return this.#jwt;
  }

  get jwt$(): Observable<string | null> {
    return this.#jwt$.asObservable();
  }

  get principal(): Principal | undefined {
    return this.#principal;
  }

  get principal$(): Observable<Principal | null> {
    return this.#principal$.asObservable();
  }

  exchangeAuthToken(token: string): void {
    this.http.get<AccessTokenResponse>('/api/common/access-token', {params: {authToken: token}})
      .subscribe(rsp => {
          if (rsp.accessToken == null) {
            throw Error("Server respond with null access token");
          }
          this.#jwt = rsp.accessToken;
          this.#jwt$.next(rsp.accessToken);

          const principal = this.parseJwt(rsp.accessToken);
          this.#principal = principal;
          this.#principal$.next(principal);
        },
      );
  }

  private parseJwt(jwt: string): Principal {
    const claims = JSON.parse(atob(jwt.split('.')[1]));
    return {
      id: Number.parseInt(claims['sub']),
      roles: claims['roles'].split(','),
      username: claims['username'],
      gameId: Number.parseInt(claims['gameId']),
    };
  }
}

type AccessTokenResponse = {
  accessToken: string;
};
