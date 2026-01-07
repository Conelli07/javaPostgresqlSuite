package model;

import enums.ContinentEnum;
import java.util.ArrayList;
import java.util.List;

public class Team {
    private Integer id;
    private String name;
    private ContinentEnum continent;
    private List<Player> players = new ArrayList<>();

    public Team() {}

    public Team(Integer id, String name, ContinentEnum continent) {
        this.id = id;
        this.name = name;
        this.continent = continent;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ContinentEnum getContinent() { return continent; }
    public void setContinent(ContinentEnum continent) { this.continent = continent; }

    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }

    public int getPlayersCount() {
        return players.size();
    }

    public int getPlayersGoals() {
        int totalGoals = 0;

        for (Player player : players) {
            if (player.getGoalNb() == null) {
                throw new IllegalStateException(
                        "Impossible de calculer les buts : le joueur "
                                + player.getName()
                                + " n'a pas de nombre de buts défini."
                );
            }
            totalGoals += player.getGoalNb();
        }

        return totalGoals;
    }
}
