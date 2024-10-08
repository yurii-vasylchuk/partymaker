import {GamePlayer} from '../../../commons/dto';

export type TournamentMatch = TournamentMatchTemplate & {
  roundId: number;
  winnerId: number;
  competitors: number[];
};

export type TournamentMatchTemplate = {
  type: 'DISTINCT_2' | 'DISTINCT_3' | 'SAME_FOR_ALL';
  competitorsTasks: CompetitorTask[];
};

interface AbstractCompetitorTask {
  type: 'IMAGE' | 'TEXT';
}

interface ImageCompetitorTask extends AbstractCompetitorTask {
  type: 'IMAGE';
  path: string;
}

interface TextCompetitorTask extends AbstractCompetitorTask {
  type: 'TEXT';
  text: string;
}

export type CompetitorTask = ImageCompetitorTask | TextCompetitorTask;

export type TournamentContext = {
  winners: number[];
  matches: TournamentMatch[];
  currentRound: number;
  currentMatchIdx: number;
  votes: { [voterId: number]: number };
  scoresPerPlace: number[];
  matchTemplates: TournamentMatchTemplate[];
  winDecider: 'GAME_MASTER' | 'VOTING';

  players: GamePlayer[];
}
