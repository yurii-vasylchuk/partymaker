import {GamePlayer} from './dto';

export function hasRole(role: string, jwt: string): boolean {
  if (jwt == null) {
    return false;
  }
  const roles: string[] = JSON.parse(atob(jwt?.split('.')[1]))['roles'].split(',');
  return roles.includes(role);
}


export function fullName({username, nickname}: GamePlayer): string {
  return `${username} aka ${nickname}`;
}
