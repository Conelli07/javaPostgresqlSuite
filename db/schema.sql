CREATE TYPE continent_enum AS ENUM ('AFRICA','EUROPA','ASIA','AMERICA');
CREATE TYPE position_enum AS ENUM ('GK','DEF','MIDF','STR');


CREATE TABLE team (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    continent continent_enum NOT NULL
);


CREATE TABLE player (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INTEGER NOT NULL,
    position player_position_enum NOT NULL,
    id_team INTEGER,
    FOREIGN KEY (id_team) REFERENCES team(id) ON DELETE SET NULL,
    UNIQUE(name, age, position)
);

ALTER TABLE player ADD COLUMN IF NOT EXISTS goal_nb INTEGER;

UPDATE player SET goal_nb = 0 WHERE name = 'Thibaut Courtois';
UPDATE player SET goal_nb = 2 WHERE name = 'Dani Carvajal';
UPDATE player SET goal_nb = 5 WHERE id = 3 AND name = 'Jude Bellingham';
UPDATE player SET goal_nb = NULL WHERE name = 'Robert Lewandowski';
UPDATE player SET goal_nb = NULL WHERE name = 'Antoine Griezmann';

