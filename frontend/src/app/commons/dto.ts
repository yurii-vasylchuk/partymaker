export interface AsyncEvent {
  type: AsyncEventType;
}

export interface AsyncAction {
  type: AsyncActionType;
}

export interface PlayerReadyAction extends AsyncAction {
  type: 'READY';
}

export interface ChoseWinnerAction extends AsyncAction {
  type: 'CHOSE_WINNER',
  winnerId: number,
}

export interface GameStateChangedEvent extends AsyncEvent {
  type: 'GAME_STATE_CHANGED';
  name: string;
  status: GameStatus;
  players: GamePlayer[];
  currentStage: StageDescriptor;
  scores: { [pid: number]: number };
  lastStageScores: { [pid: number]: number };
}

export type GamePlayer = {
  userId: number;
  username: string;
  chosenAvatar: string;
  nickname: string;
  chosenTraits: { [category: string]: string };
};

export type GameStatus = 'NEW' | 'ON_STAGE' | 'STAGE_COMPLETED' | 'FINALIZED';
export type AsyncEventType = 'GAME_STATE_CHANGED';
export type AsyncActionType =
  'CHOOSE_AVATAR'
  | 'CHOOSE_NICKNAME'
  | 'CHOOSE_TRAITS'
  | 'GUESSES_CHANGED'
  | 'PLACEMENTS_SET'
  | 'READY'
  | 'NEXT_TASK'
  | 'PREV_TASK'
  | 'NEXT_MATCH'
  | 'CHOSE_WINNER'
  | 'VOTE';

export type StageDescriptor = {
  name: string;
  description: string;
  order: number;
  type: string;
  scores: { [pid: number]: number };
  readiness?: {
    total: number;
    ready: number;
  }
  ctx: unknown;
}

export type Principal = {
  id: number;
  roles: string[];
  username: string;
  gameId: number;
}
