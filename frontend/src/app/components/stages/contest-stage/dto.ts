import {AsyncAction, GamePlayer, PlayerReadyAction} from '../../../commons/dto';

export type ContestStageContext = {
  players: GamePlayer[];
  playersReadiness: { [playerId: number]: boolean };

  availableTasks: ContestTask[];
  distributionType: ContestDistributionType;
  isTaskDetailsPrivate: boolean;
  usersPerTask: number;
  winningPlaces: number;
  scoresPerPlace: number[];

  currentTaskIdx: number;
  tasksDistribution: ContestUserTaskLink[];

  placements: { [voterId: number]: { [place: number]: number } }
  state: ContestState;
}

export type ContestState = 'TASK' | 'RANKING';

export type ContestDistributionType = 'RANDOM' | 'PREDEFINED';

export type ContestUserTaskLink = {
  usersIds: number[];
  taskIdx: number;
};

export type ContestTask = {
  title: string;
  description: string;
};

export interface PlacementsSetAction extends AsyncAction {
  type: 'PLACEMENTS_SET';
  // Place to userId
  placements: { [place: number]: number | null };
}

export type ContestActions = PlacementsSetAction | PlayerReadyAction;
