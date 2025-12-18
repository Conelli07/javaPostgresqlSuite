INSERT INTO team (id, name, continent) VALUES
    (1,'Real Madrid CF','EUROPA'),
    (2,'FC Barcelone','EUROPA'),
    (3,'Atlético de Madrid','EUROPA'),
    (4,'Al Ahly SC','AFRICA'),
    (5,'Inter Miami CF','AMERICA');

SELECT setval('team_id_seq', (SELECT MAX(id) FROM team));

INSERT INTO player (id, name, age, position, id_team) VALUES
    (1,'Thibaut Courtois',32,'GK',1),
    (2,'Dani Carvajal',33,'DEF',1),
    (3,'Jude Bellingham',21,'MIDF',1),
    (4,'Robert Lewandowski',36,'STR',2),
    (5,'Antoine Griezmann',33,'STR',3);

SELECT setval('player_id_seq', (SELECT MAX(id) FROM player));