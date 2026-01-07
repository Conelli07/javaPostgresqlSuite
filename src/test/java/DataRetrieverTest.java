import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import enums.ContinentEnum;
import enums.PlayerPositionEnum;
import model.Player;
import model.Team;
import repository.DataRetriever;

public class DataRetrieverTest {

    private DataRetriever dataRetriever;

    @BeforeEach
    void setUp() {
        dataRetriever = new DataRetriever();
    }

    @Test
    void testFindTeamById_realMadrid() {
        Team team = dataRetriever.findTeamById(1);

        assertNotNull(team);
        assertEquals("Real Madrid CF", team.getName());
        assertNotNull(team.getPlayers());
        assertEquals(3, team.getPlayers().size());
    }

    @Test
    void testFindTeamById_interMiami() {
        Team team = dataRetriever.findTeamById(5);

        assertNotNull(team);
        assertEquals("Inter Miami CF", team.getName());
        assertNotNull(team.getPlayers());
        assertTrue(team.getPlayers().isEmpty());
    }

    @Test
    void testFindPlayers_page1_size2() {
        List<Player> players = dataRetriever.findPlayers(1, 2);

        assertEquals(2, players.size());
        assertEquals("Thibaut Courtois", players.get(0).getName());
        assertEquals("Dani Carvajal", players.get(1).getName());
    }

    @Test
    void testFindPlayers_page3_size5() {
        List<Player> players = dataRetriever.findPlayers(3, 5);

        assertTrue(players.isEmpty());
    }

    @Test
    void testFindTeamsByPlayerName() {
        List<Team> teams = dataRetriever.findTeamsByPlayerName("an");

        assertEquals(2, teams.size());
        assertEquals("Real Madrid CF", teams.get(0).getName());
        assertEquals("Atletico Madrid CF", teams.get(1).getName());
    }

    @Test
    void testFindPlayersByCriteria() {
        List<Player> players = dataRetriever.findPlayersByCriteria(
                "ud",
                PlayerPositionEnum.MIDF,
                "Madrid",
                ContinentEnum.EUROPA,
                1,
                10
        );

        assertEquals(1, players.size());
        assertEquals("Jude Bellingham", players.get(0).getName());
    }

    @Test
    void testCreatePlayers_shouldFail() {
        Player p1 = new Player(null, "Jude Bellingham", 23, PlayerPositionEnum.STR);
        Player p2 = new Player(null, "Pedri", 24, PlayerPositionEnum.MIDF);

        assertThrows(RuntimeException.class, () -> {
            dataRetriever.createPlayers(List.of(p1, p2));
        });
    }

    @Test
    void testCreatePlayers_success() {
        Player p1 = new Player(null, "TestPlayerA", 30, PlayerPositionEnum.STR);
        Player p2 = new Player(null, "TestPlayerB", 28, PlayerPositionEnum.MIDF);

        List<Player> result = dataRetriever.createPlayers(List.of(p1, p2));

        assertEquals(2, result.size());
        assertNotNull(result.get(0).getId());
        assertNotNull(result.get(1).getId());
    }

    @Test
    void testSaveTeam_addPlayer() {
        Team team = dataRetriever.findTeamById(1);
        int initialSize = team.getPlayers().size();

        Player vini = new Player(6, "Vini", 25, PlayerPositionEnum.STR);
        team.getPlayers().add(vini);

        Team updatedTeam = dataRetriever.saveTeam(team);

        assertEquals(initialSize + 1, updatedTeam.getPlayers().size());
    }

    @Test
    void testSaveTeam_removeAllPlayers() {
        Team team = dataRetriever.findTeamById(2);

        team.setPlayers(List.of());

        Team updatedTeam = dataRetriever.saveTeam(team);

        assertNotNull(updatedTeam.getPlayers());
        assertTrue(updatedTeam.getPlayers().isEmpty());
    }
}
