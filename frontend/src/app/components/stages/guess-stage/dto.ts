import {AsyncAction, GamePlayer, PlayerReadyAction} from '../../../commons/dto';

export interface GuessChangedAction extends AsyncAction {
  type: 'GUESSES_CHANGED';
  guesses: { [key: number]: number };
}

export type GuessStageContext = {
  playersReadiness: { [playerId: number]: boolean };
  players: GamePlayer[];
  guesses: { [guesserId: number]: { [profileId: number]: number } };
};

export type GuessActions = GuessChangedAction | PlayerReadyAction;
