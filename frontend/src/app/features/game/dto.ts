import {GameStateChangedEvent} from '../../commons/dto';

export type GameState = Omit<GameStateChangedEvent, 'type'>;
