package repository;

import db.DBConnection;
import enums.ContinentEnum;
import enums.PlayerPositionEnum;
import model.Player;
import model.Team;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataRetriever {

    private DBConnection dbConnection;

    public DataRetriever() {
        this.dbConnection = new DBConnection();
    }

    public Team findTeamById(Integer id) {
        Team team = null;
        String teamSql = "SELECT id, name, continent FROM team WHERE id = ?";
        String playersSql = "SELECT id, name, age, position FROM player WHERE id_team = ?";

        try (Connection c = dbConnection.getDBConnection()) {

            try (PreparedStatement ps = c.prepareStatement(teamSql)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    team = new Team(
                            rs.getInt("id"),
                            rs.getString("name"),
                            ContinentEnum.valueOf(rs.getString("continent"))
                    );
                }
            }

            if (team != null) {
                try (PreparedStatement ps = c.prepareStatement(playersSql)) {
                    ps.setInt(1, id);
                    ResultSet rs = ps.executeQuery();

                    List<Player> players = new ArrayList<>();
                    while (rs.next()) {
                        Player player = new Player(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getInt("age"),
                                PlayerPositionEnum.valueOf(rs.getString("position"))
                        );
                        player.setTeam(team);
                        players.add(player);
                    }
                    team.setPlayers(players);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return team;
    }

    public List<Player> findPlayers(int page, int size) {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT id, name, age, position FROM player ORDER BY id LIMIT ? OFFSET ?";

        try (Connection c = dbConnection.getDBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, size);
            ps.setInt(2, (page - 1) * size);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                players.add(new Player(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        PlayerPositionEnum.valueOf(rs.getString("position"))
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return players;
    }

    public List<Player> createPlayers(List<Player> newPlayers) {
        String checkSql = "SELECT COUNT(*) FROM player WHERE name = ? AND age = ? AND position = ?::player_position_enum";
        String insertSql = "INSERT INTO player (name, age, position, id_team) VALUES (?, ?, ?::player_position_enum, ?) RETURNING id";

        Connection c = null;
        try {
            c = dbConnection.getDBConnection();
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement(checkSql)) {
                for (Player player : newPlayers) {
                    ps.setString(1, player.getName());
                    ps.setInt(2, player.getAge());
                    ps.setString(3, player.getPosition().name());

                    ResultSet rs = ps.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        c.rollback();
                        throw new RuntimeException("Le joueur " + player.getName() + " existe déjà");
                    }
                }
            }

            List<Player> createdPlayers = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                for (Player player : newPlayers) {
                    ps.setString(1, player.getName());
                    ps.setInt(2, player.getAge());
                    ps.setString(3, player.getPosition().name());

                    if (player.getTeam() != null && player.getTeam().getId() != null) {
                        ps.setInt(4, player.getTeam().getId());
                    } else {
                        ps.setNull(4, Types.INTEGER);
                    }

                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        player.setId(rs.getInt("id"));
                        createdPlayers.add(player);
                    }
                }
            }

            c.commit();
            return createdPlayers;

        } catch (SQLException e) {
            if (c != null) {
                try {
                    c.rollback();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new RuntimeException(e);
        } finally {
            if (c != null) {
                try {
                    c.setAutoCommit(true);
                    c.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Team saveTeam(Team teamToSave) {
        String checkSql = "SELECT id FROM team WHERE id = ?";
        String insertSql = "INSERT INTO team (name, continent) VALUES (?, ?::continent_enum) RETURNING id";
        String updateSql = "UPDATE team SET name = ?, continent = ?::continent_enum WHERE id = ?";
        String updatePlayerSql = "UPDATE player SET id_team = ? WHERE id = ?";
        String removeSql = "UPDATE player SET id_team = NULL WHERE id_team = ? AND id NOT IN (%s)";

        Connection c = null;
        try {
            c = dbConnection.getDBConnection();
            c.setAutoCommit(false);

            boolean exists = false;
            Integer teamId = teamToSave.getId();

            if (teamId != null) {
                try (PreparedStatement ps = c.prepareStatement(checkSql)) {
                    ps.setInt(1, teamId);
                    ResultSet rs = ps.executeQuery();
                    exists = rs.next();
                }
            }

            if (exists) {
                try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                    ps.setString(1, teamToSave.getName());
                    ps.setString(2, teamToSave.getContinent().name());
                    ps.setInt(3, teamId);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                    ps.setString(1, teamToSave.getName());
                    ps.setString(2, teamToSave.getContinent().name());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        teamId = rs.getInt("id");
                        teamToSave.setId(teamId);
                    }
                }
            }

            List<Player> players = teamToSave.getPlayers();

            if (players != null && !players.isEmpty()) {
                try (PreparedStatement ps = c.prepareStatement(updatePlayerSql)) {
                    for (Player player : players) {
                        ps.setInt(1, teamId);
                        ps.setInt(2, player.getId());
                        ps.executeUpdate();
                    }
                }

                List<Integer> ids = players.stream().map(Player::getId).collect(Collectors.toList());
                String placeholders = ids.stream().map(i -> "?").collect(Collectors.joining(","));
                String query = String.format(removeSql, placeholders);

                try (PreparedStatement ps = c.prepareStatement(query)) {
                    ps.setInt(1, teamId);
                    for (int i = 0; i < ids.size(); i++) {
                        ps.setInt(i + 2, ids.get(i));
                    }
                    ps.executeUpdate();
                }
            } else {
                String removeAll = "UPDATE player SET id_team = NULL WHERE id_team = ?";
                try (PreparedStatement ps = c.prepareStatement(removeAll)) {
                    ps.setInt(1, teamId);
                    ps.executeUpdate();
                }
            }

            c.commit();
            return teamToSave;

        } catch (SQLException e) {
            if (c != null) {
                try {
                    c.rollback();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new RuntimeException(e);
        } finally {
            if (c != null) {
                try {
                    c.setAutoCommit(true);
                    c.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<Team> findTeamsByPlayerName(String playerName) {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT DISTINCT t.id, t.name, t.continent FROM team t INNER JOIN player p ON p.id_team = t.id WHERE LOWER(p.name) LIKE LOWER(?) ORDER BY t.id";

        try (Connection c = dbConnection.getDBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, "%" + playerName + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                teams.add(new Team(
                        rs.getInt("id"),
                        rs.getString("name"),
                        ContinentEnum.valueOf(rs.getString("continent"))
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return teams;
    }

    public List<Player> findPlayersByCriteria(String playerName, PlayerPositionEnum position, String teamName, ContinentEnum continent, int page, int size) {
        List<Player> players = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT p.id, p.name, p.age, p.position FROM player p LEFT JOIN team t ON p.id_team = t.id WHERE 1=1");

        if (playerName != null && !playerName.trim().isEmpty()) {
            sql.append(" AND LOWER(p.name) LIKE LOWER(?)");
        }
        if (position != null) {
            sql.append(" AND p.position = ?::player_position_enum");
        }
        if (teamName != null && !teamName.trim().isEmpty()) {
            sql.append(" AND LOWER(t.name) LIKE LOWER(?)");
        }
        if (continent != null) {
            sql.append(" AND t.continent = ?::continent_enum");
        }

        sql.append(" ORDER BY p.id LIMIT ? OFFSET ?");

        try (Connection c = dbConnection.getDBConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {

            int index = 1;

            if (playerName != null && !playerName.trim().isEmpty()) {
                ps.setString(index++, "%" + playerName + "%");
            }
            if (position != null) {
                ps.setString(index++, position.name());
            }
            if (teamName != null && !teamName.trim().isEmpty()) {
                ps.setString(index++, "%" + teamName + "%");
            }
            if (continent != null) {
                ps.setString(index++, continent.name());
            }

            ps.setInt(index++, size);
            ps.setInt(index, (page - 1) * size);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                players.add(new Player(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        PlayerPositionEnum.valueOf(rs.getString("position"))
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return players;
    }
}