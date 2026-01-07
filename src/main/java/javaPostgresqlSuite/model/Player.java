package model;

import enums.PlayerPositionEnum;

public class Player {
    private Integer id;
    private String name;
    private Integer age;
    private PlayerPositionEnum position;
    private Team team;
    private Integer goalNb; // nouvel attribut

    public Player() {}

    public Player(Integer id, String name, Integer age, PlayerPositionEnum position) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.position = position;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public PlayerPositionEnum getPosition() { return position; }
    public void setPosition(PlayerPositionEnum position) { this.position = position; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public Integer getGoalNb() { return goalNb; }
    public void setGoalNb(Integer goalNb) { this.goalNb = goalNb; }
}
