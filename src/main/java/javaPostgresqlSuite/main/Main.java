package main;

import enums.ContinentEnum;
import enums.PlayerPositionEnum;
import model.Player;
import model.Team;
import repository.DataRetriever;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        DataRetriever data = new DataRetriever();

        Team real = data.findTeamById(1);
        System.out.println("Équipe : " + real.getName());
        real.getPlayers().forEach(p -> System.out.println("  " + p.getName()));

        try {
            System.out.println("Total buts : " + real.getPlayersGoals());
        } catch (IllegalStateException e) {
            System.out.println("Impossible de calculer les buts : " + e.getMessage());
        }

        Team interMiami = data.findTeamById(5);
        System.out.println(interMiami.getName() + " | Joueurs : " + interMiami.getPlayersCount());

        data.findPlayers(1, 2).forEach(p -> System.out.println("Joueur page 1: " + p.getName()));

        data.findTeamsByPlayerName("an").forEach(t -> System.out.println("Équipe avec 'an' : " + t.getName()));

        data.findPlayersByCriteria("ud", PlayerPositionEnum.MIDF, "Madrid", ContinentEnum.EUROPA, 1, 10)
                .forEach(p -> System.out.println("Filtré : " + p.getName()));

        try {
            data.createPlayers(List.of(
                    new Player(null, "Jude Bellingham", 23, PlayerPositionEnum.STR),
                    new Player(null, "Pedri", 24, PlayerPositionEnum.MIDF)
            ));
        } catch (RuntimeException e) {
            System.out.println("Exception attendue : " + e.getMessage());
        }

        List<Player> newPlayers = data.createPlayers(List.of(
                new Player(null, "Vini", 25, PlayerPositionEnum.STR),
                new Player(null, "Pedri", 24, PlayerPositionEnum.MIDF)
        ));

        Optional<Player> vini = newPlayers.stream().filter(p -> p.getName().equals("Vini")).findFirst();
        vini.ifPresent(real.getPlayers()::add);

        real = data.saveTeam(real);

        try {
            System.out.println("Total buts après ajout : " + real.getPlayersGoals());
        } catch (IllegalStateException e) {
            System.out.println("Impossible de calculer les buts après ajout : " + e.getMessage());
        }

        Team barca = data.findTeamById(2);
        if (barca == null) barca = new Team(2, "FC Barcelone", ContinentEnum.EUROPA);
        barca.setPlayers(new ArrayList<>());
        data.saveTeam(barca);
        System.out.println("FC Barcelone sauvegardée avec " + barca.getPlayersCount() + " joueurs");
    }
}
