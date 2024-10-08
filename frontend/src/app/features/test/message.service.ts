import {Injectable} from '@angular/core';
import {StompService} from '../../service/stomp.service';

@Injectable({
  providedIn: 'root',
})
export class MessageService {

  constructor(private stompService: StompService) {
  }
}
