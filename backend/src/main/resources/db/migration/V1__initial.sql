CREATE TABLE games
(
    id      INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name    VARCHAR(255) NOT NULL UNIQUE,
    context VARCHAR      NOT NULL,
    status  VARCHAR(50)  NOT NULL
);

CREATE TABLE stages
(
    id          INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    stage_order INT          NOT NULL,
    type        VARCHAR(100) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR,
    game_id     INT          NOT NULL REFERENCES games (id),
    context     VARCHAR      NOT NULL
);

ALTER TABLE games
    ADD COLUMN current_stage INT REFERENCES stages (id);

CREATE TABLE users
(
    id       INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    username VARCHAR(50) NOT NULL,
    token    VARCHAR(255) UNIQUE,
    roles    VARCHAR(255),
    game_id  INT REFERENCES games (id),

    CONSTRAINT u_username_game UNIQUE (username, game_id)
);
