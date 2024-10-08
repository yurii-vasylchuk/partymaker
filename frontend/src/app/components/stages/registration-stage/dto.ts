import {AsyncAction, PlayerReadyAction} from '../../../commons/dto';

export type RegistrationStageContext = {
  playersIds: number[];
  chosenAvatars: { [key: number]: string };
  chosenTraits: { [key: number]: { [key: string]: string } };
  chosenNicknames: { [key: number]: string };
  playersReadiness: { [key: string]: boolean };
  availableAvatars: string[];
  availableTraits: { [key: string]: string[] };
}

export interface ChooseAvatarAction extends AsyncAction {
  type: 'CHOOSE_AVATAR';
  avatar: string;
}

export interface ChooseNicknameAction extends AsyncAction {
  type: 'CHOOSE_NICKNAME';
  nickname: string;
}

export interface ChooseTraitsAction extends AsyncAction {
  type: 'CHOOSE_TRAITS';
  traits: { [key: string]: string };
}

export type RegistrationAction = ChooseAvatarAction | ChooseNicknameAction | ChooseTraitsAction | PlayerReadyAction;
