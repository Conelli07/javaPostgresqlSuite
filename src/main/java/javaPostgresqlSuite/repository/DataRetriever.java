package repository;
import db.DBConnection;
import enums.ContinentEnum;
import enums.PlayerPositionEnum;
import model.Player;
import model.Team;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class DataRetriever {

    private final DBConnection dbConnection;

    public DataRetriever() {
        this.dbConnection = new DBConnection();
    }

    private Player mapPlayer(ResultSet rs, Team team) throws SQLException {
        Player p = new Player(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("age"),
                PlayerPositionEnum.valueOf(rs.getString("position"))
        );
        p.setTeam(team);
        int goal = rs.getInt("goal_nb");
        if (!rs.wasNull()) p.setGoalNb(goal);
        return p;
    }

    public Team findTeamById(Integer id) {
        Team team = null;
        String teamSql = "SELECT id, name, continent FROM team WHERE id=?";
        String playerSql = "SELECT id, name, age, position, goal_nb FROM player WHERE id_team=?";

        try (Connection c = dbConnection.getDBConnection()) {
            try (PreparedStatement ps = c.prepareStatement(teamSql)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) team = new Team(rs.getInt("id"), rs.getString("name"), ContinentEnum.valueOf(rs.getString("continent")));
            }

            if (team != null) {
                try (PreparedStatement ps = c.prepareStatement(playerSql)) {
                    ps.setInt(1, id);
                    ResultSet rs = ps.executeQuery();
                    List<Player> players = new ArrayList<>();
                    while (rs.next()) players.add(mapPlayer(rs, team));
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
        String sql = "SELECT id, name, age, position, goal_nb FROM player ORDER BY id LIMIT ? OFFSET ?";

        try (Connection c = dbConnection.getDBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, (page - 1) * size);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) players.add(mapPlayer(rs, null));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return players;
    }

    public List<Player> createPlayers(List<Player> newPlayers) {
        String checkSql = "SELECT COUNT(*) FROM player WHERE name=? AND age=? AND position=?::player_position_enum";
        String insertSql = "INSERT INTO player (name, age, position, id_team, goal_nb) VALUES (?,?,?::player_position_enum,?,?) RETURNING id";

        try (Connection c = dbConnection.getDBConnection()) {
            c.setAutoCommit(false);
            List<Player> created = new ArrayList<>();

            try (PreparedStatement check = c.prepareStatement(checkSql);
                 PreparedStatement insert = c.prepareStatement(insertSql)) {

                for (Player p : newPlayers) {
                    check.setString(1, p.getName());
                    check.setInt(2, p.getAge());
                    check.setString(3, p.getPosition().name());
                    ResultSet rsCheck = check.executeQuery();
                    if (rsCheck.next() && rsCheck.getInt(1) > 0) throw new RuntimeException("Le joueur " + p.getName() + " existe déjà");

                    insert.setString(1, p.getName());
                    insert.setInt(2, p.getAge());
                    insert.setString(3, p.getPosition().name());
                    if (p.getTeam() != null && p.getTeam().getId() != null) insert.setInt(4, p.getTeam().getId());
                    else insert.setNull(4, Types.INTEGER);
                    if (p.getGoalNb() != null) insert.setInt(5, p.getGoalNb());
                    else insert.setNull(5, Types.INTEGER);

                    ResultSet rsInsert = insert.executeQuery();
                    if (rsInsert.next()) p.setId(rsInsert.getInt("id"));
                    created.add(p);
                }
                c.commit();
                return created;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Team saveTeam(Team team) {
        String checkSql = "SELECT id FROM team WHERE id=?";
        String insertSql = "INSERT INTO team (name, continent) VALUES (?,?::continent_enum) RETURNING id";
        String updateSql = "UPDATE team SET name=?, continent=?::continent_enum WHERE id=?";
        String updatePlayerSql = "UPDATE player SET id_team=?, goal_nb=? WHERE id=?";

        try (Connection c = dbConnection.getDBConnection()) {
            c.setAutoCommit(false);
            Integer teamId = team.getId();
            boolean exists = false;

            if (teamId != null) {
                try (PreparedStatement ps = c.prepareStatement(checkSql)) {
                    ps.setInt(1, teamId);
                    exists = ps.executeQuery().next();
                }
            }

            if (exists) {
                try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                    ps.setString(1, team.getName());
                    ps.setString(2, team.getContinent().name());
                    ps.setInt(3, teamId);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                    ps.setString(1, team.getName());
                    ps.setString(2, team.getContinent().name());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) team.setId(rs.getInt("id"));
                    teamId = team.getId();
                }
            }

            List<Player> players = team.getPlayers();
            if (players != null) {
                for (Player p : players) {
                    try (PreparedStatement ps = c.prepareStatement(updatePlayerSql)) {
                        ps.setInt(1, teamId);
                        ps.setInt(2, p.getGoalNb() != null ? p.getGoalNb() : 0);
                        ps.setInt(3, p.getId());
                        ps.executeUpdate();
                    }
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement("UPDATE player SET id_team=NULL WHERE id_team=?")) {
                    ps.setInt(1, teamId);
                    ps.executeUpdate();
                }
            }

            c.commit();
            return team;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Team> findTeamsByPlayerName(String name) {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT DISTINCT t.id, t.name, t.continent FROM team t JOIN player p ON p.id_team=t.id WHERE LOWER(p.name) LIKE LOWER(?) ORDER BY t.id";

        try (Connection c = dbConnection.getDBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + name + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) teams.add(new Team(rs.getInt("id"), rs.getString("name"), ContinentEnum.valueOf(rs.getString("continent"))));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return teams;
    }

    public List<Player> findPlayersByCriteria(String name, PlayerPositionEnum pos, String teamName, ContinentEnum cont, int page, int size) {
        List<Player> players = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT p.id, p.name, p.age, p.position, p.goal_nb FROM player p LEFT JOIN team t ON p.id_team=t.id WHERE 1=1");

        if (name != null && !name.isEmpty()) sql.append(" AND LOWER(p.name) LIKE LOWER(?)");
        if (pos != null) sql.append(" AND p.position=?::player_position_enum");
        if (teamName != null && !teamName.isEmpty()) sql.append(" AND LOWER(t.name) LIKE LOWER(?)");
        if (cont != null) sql.append(" AND t.continent=?::continent_enum");

        sql.append(" ORDER BY p.id LIMIT ? OFFSET ?");

        try (Connection c = dbConnection.getDBConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {

            int i = 1;
            if (name != null && !name.isEmpty()) ps.setString(i++, "%" + name + "%");
            if (pos != null) ps.setString(i++, pos.name());
            if (teamName != null && !teamName.isEmpty()) ps.setString(i++, "%" + teamName + "%");
            if (cont != null) ps.setString(i++, cont.name());
            ps.setInt(i++, size);
            ps.setInt(i, (page - 1) * size);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) players.add(mapPlayer(rs, null));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return players;
    }
}
