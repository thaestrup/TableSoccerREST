import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;

import Model.Game;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Collections;

import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Math;
import java.math.BigInteger;

import java.util.*;

import java.text.SimpleDateFormat;



/**
 * Created by super on 30/10/2016.
 */
public class AdditionalUtil {

    private static final String HOURSTOLOOKBACK = "1";  // Set to 1, when not debugging
    private static final int THRESSHOLDINMINUTES = 30;

    private static class Pair {
      private String player1;
      private String player2;

      public Pair() {
        player1 = null;
        player2 = null;
      }

      public Pair(String p1, String p2) {
        player1 = p1;
        player2 = p2;
      }

      public String getPlayer1() {
        return player1;
      }

      public String getPlayer2() {
        return player2;
      }

      public void setPlayer1(String p1) {
        player1 = p1;
      }

      public void setPlayer2(String p2) {
        player2 = p2;
      }

      @Override
      public String toString() {
        return "P1: " + player1 + " - P2:" + player2;
      }
    }

    private static LinkedList<String> generateAwesomeList(int maxPlayersNeeded, LinkedList<String> namesOfAvailablePlayers) {
      // So now we have the available players and the max result we should send back (which is the same as the precise result)
      Logger logger = LoggerFactory.getLogger(AdditionalUtil.class);
      // Get all matches 1 hour back
      List<Game> games = MoreUtil.getGamesForThisManyHoursBackInTime(HOURSTOLOOKBACK);


      //String currentLastUpdated = "";
      int maxNumberOfRoundsBack = 25;
      int foundNumberOfRoundsBack = 0;

      List<Game> gamesToLookAt = new LinkedList<Game>();
      Date currentLastUpdatedDate = null;

      for (Game game : games) {
        //logger.info("Game id: " + game.getId());
        String gameLastUpdated = game.getLastUpdated();
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.S");
        Date gameLastUpdatedDate = null;
        try {
          gameLastUpdatedDate = parser.parse(gameLastUpdated);
        } catch (Exception ex) {
          logger.info("FEJL - KUNNE IKKE PARSE");
        }
        if (currentLastUpdatedDate == null) {
          currentLastUpdatedDate = gameLastUpdatedDate;
        }
        //logger.info("gameLastUpdatedDate: " + gameLastUpdatedDate.toString());
        //logger.info("currentLastUpdatedDate: " + currentLastUpdatedDate.toString());

        long fromCurrentLastToThis = currentLastUpdatedDate.getTime() - gameLastUpdatedDate.getTime();
        //logger.info("######################Der er " + fromCurrentLastToThis + " millisekunders forskel");

        // Don't go futher, if there has been a long break
        if (fromCurrentLastToThis < THRESSHOLDINMINUTES * 60 * 1000) {
          if (!currentLastUpdatedDate.equals(gameLastUpdatedDate)) {
            //logger.info("De to datoer var IKKE ens");
            foundNumberOfRoundsBack++;
            currentLastUpdatedDate = gameLastUpdatedDate;
            //logger.info("Sætter currentLastUpdatedDate til " + gameLastUpdatedDate.toString()   + " (da de ikke var ens)");
          } else {
            //logger.info("De to datoer var ens");
            currentLastUpdatedDate = gameLastUpdatedDate;
            //logger.info("Sætter currentLastUpdatedDate til " + gameLastUpdatedDate.toString() + " selvom de er ens");
          }
          if (foundNumberOfRoundsBack < maxNumberOfRoundsBack) {
            gamesToLookAt.add(game);
          }
        } else {
          logger.info("There has been a break longer than " + THRESSHOLDINMINUTES + " minutes, so the last games will be ignored");
          break;
        }
      }

      logger.info("<<<<<<<<<Før var der: " + games.size() );
      logger.info("<<<<<<<<<Nu er der: " + gamesToLookAt.size());
      games = gamesToLookAt;
      //Collections.reverse(gamesToLookAt);


      // Doing it from the oldest to newest game, give 2^something points (just change the points_at_stake score for them, which is a hack)
      Collections.reverse(games);

      int currentValueForGame = 1;
      String currentLastUpdated = "";

      for (Game game : games) {
        logger.info("Game i efter: " + game.getId());
        String gameLastUpdated = game.getLastUpdated();
        //logger.info("gameLastUpdated:" + gameLastUpdated);
        //logger.info("currentLastUpdated:" + currentLastUpdated);

        if (!currentLastUpdated.equals(gameLastUpdated)) {
          if (currentLastUpdated == "") {
            // Don't change currentValueForGame this time
          } else {
            currentValueForGame = currentValueForGame * 2;
          }

          currentLastUpdated = gameLastUpdated;

          logger.info("currentValueForGame: " + currentValueForGame);
        } else {
          logger.info("currentValueForGame: (samme) " + currentValueForGame);
        }
        game.setPoints_at_stake(currentValueForGame);
      }

      // Then create array for all possible players
      Map<String, Integer> currentPlayerScore = new HashMap<String, Integer>();
      for (String playerName : namesOfAvailablePlayers) {
        currentPlayerScore.put(playerName, 0);
        //logger.info("Sætter ind for playerName: " + playerName);
      }

      // And create array for all pairs
      Map<String, Integer> currentBuddyScore = new HashMap<String, Integer>();
      for (String playerName1 : namesOfAvailablePlayers) {
        for (String playerName2 : namesOfAvailablePlayers) {
          currentBuddyScore.put(playerName1 + "<->" + playerName2, 0);
        }
      }

      for (Game game : games) {

        String pair1_player1 = game.getPlayer_red_1();
        String pair1_player2 = game.getPlayer_red_2();
        String pair2_player1 = game.getPlayer_blue_1();
        String pair2_player2 = game.getPlayer_blue_2();

        int currentPointsScore = game.getPoints_at_stake();
        if (namesOfAvailablePlayers.contains(pair1_player1) && !pair1_player1.equals("null")) {
          //logger.info("pair1_player1: " + pair1_player1);

            Integer currentNumber = currentPlayerScore.get(pair1_player1);
            if (currentNumber == null) currentNumber = 0;
            //logger.info("currentNumber: " + currentNumber);

            //logger.info("currentPointsScore:" + currentPointsScore);

            Integer nextNumber = currentNumber  + currentPointsScore;
            //logger.info("nextNumber:" + nextNumber);

           currentPlayerScore.put(pair1_player1, nextNumber);

         }

        if (namesOfAvailablePlayers.contains(pair1_player2) && !pair1_player2.equals("null")) {
          //logger.info("pair1_player2: " + pair1_player2);

          Integer currentNumber = currentPlayerScore.get(pair1_player2);
          if (currentNumber == null) currentNumber = 0;
          //logger.info("currentNumber: " + currentNumber);
          //logger.info("currentPointsScore:" + currentPointsScore);

          Integer nextNumber = currentNumber  + currentPointsScore;
          //logger.info("nextNumber:" + nextNumber);

          currentPlayerScore.put(pair1_player2, nextNumber);

         }

        if (namesOfAvailablePlayers.contains(pair2_player1) && !pair2_player1.equals("null")) {
          //logger.info("pair2_player1: " + pair2_player1);

            Integer currentNumber = currentPlayerScore.get(pair2_player1);
            if (currentNumber == null) currentNumber = 0;
            //logger.info("currentNumber: " + currentNumber);
            //logger.info("currentPointsScore:" + currentPointsScore);

            Integer nextNumber = currentNumber  + currentPointsScore;
            //logger.info("nextNumber:" + nextNumber);
            //logger.info("3.2.4");
            currentPlayerScore.put(pair2_player1, nextNumber);

         }
         if (namesOfAvailablePlayers.contains(pair2_player2) && !pair2_player2.equals("null")) {
           //logger.info("pair2_player2: " + pair2_player2);

             Integer currentNumber = currentPlayerScore.get(pair2_player2);
             if (currentNumber == null) currentNumber = 0;
             //logger.info("currentNumber: " + currentNumber);
             //logger.info("currentPointsScore:" + currentPointsScore);

             Integer nextNumber = currentNumber  + currentPointsScore;
             //logger.info("nextNumber:" + nextNumber);

            currentPlayerScore.put(pair2_player2, nextNumber);

          }
          // Pair 1
          boolean pair1ShallBeSaved = true;
          if (namesOfAvailablePlayers.contains(pair1_player1)) {
            String pair1String = "";
            if (!pair1_player2.equals("null")) {
              if (!namesOfAvailablePlayers.contains(pair1_player2)) {
                pair1ShallBeSaved = false;
              } else {
                pair1String = pair1_player1 + "<->" + pair1_player2;
              }
            } else {
              pair1String = pair1_player1 + "<->" + pair1_player1;
            }
            if (pair1ShallBeSaved) {
              Integer currentNumberPair1 = currentBuddyScore.get(pair1String);
              if (currentNumberPair1 == null) currentNumberPair1 = 0;
              Integer nextNumberPair1 = currentNumberPair1  + currentPointsScore;
              currentBuddyScore.put(pair1String, nextNumberPair1);
            }
          }

          // Pair 2
          boolean pair2ShallBeSaved = true;
          if (namesOfAvailablePlayers.contains(pair2_player1)) {
            String pair2String = "";
            if (!pair2_player2.equals("null")) {
              if (!namesOfAvailablePlayers.contains(pair2_player2)) {
                pair2ShallBeSaved = false;
              } else {
                pair2String = pair2_player1 + "<->" + pair2_player2;
              }
            } else {
              pair2String = pair2_player1 + "<->" + pair2_player1;
            }
            if (pair2ShallBeSaved) {
              Integer currentNumberPair2 = currentBuddyScore.get(pair2String);
              if (currentNumberPair2 == null) currentNumberPair2 = 0;
              Integer nextNumberPair2 = currentNumberPair2  + currentPointsScore;
              currentBuddyScore.put(pair2String, nextNumberPair2);
            }
          }
      }

      for (Map.Entry<String, Integer> entry : currentPlayerScore.entrySet()) {
          logger.info("currentPlayerScore: " + entry.getKey() + "/" + entry.getValue());
      }


      for (Map.Entry<String, Integer> entry : currentBuddyScore.entrySet()) {
          logger.info("currentBuddyScore: " + entry.getKey() + "/" + entry.getValue());
      }

      currentPlayerScore = sortByValue(currentPlayerScore);

      logger.info("Så er currentPlayerScore sorteret efter value");
      for (Map.Entry<String, Integer> entry : currentPlayerScore.entrySet()) {
          logger.info("currentPlayerScore: " + entry.getKey() + "/" + entry.getValue());
      }

      // Så tager vi personer direkte over i puljen, så længe de alle kan være method
      boolean stillLookingForMorePlayers = true;
      LinkedList<String> certainPlayers = new LinkedList<String>();
      LinkedList<String> possiblePlayers = new LinkedList<String>();
      Integer previousNumber = -1;
      int currentCertainPlayers = 0;
      logger.info("Vi skal bruge maxPlayersNeeded: " + maxPlayersNeeded);
      for (Map.Entry<String, Integer> entry : currentPlayerScore.entrySet()) {
        Integer val = entry.getValue();
        String key = entry.getKey();
        if (currentCertainPlayers < maxPlayersNeeded) {

          //logger.info("Vi er ikke fyldt op endnu, så måske skal vi bruge " + key);


          // Hvis denne værdi er anderledes end den sidste vi havde, ved vi at nogle spillere fra "possiblePlayers" kan bruges,
          // fordi vi stadig mangler spillere
          if (val > previousNumber ) {
            if (possiblePlayers.size() + certainPlayers.size() <= maxPlayersNeeded) {
              // Så er det ok at flytte det direkte over
              certainPlayers.addAll(possiblePlayers);
              currentCertainPlayers = certainPlayers.size();

              //logger.info("Vi flyttede " + possiblePlayers.size() + " over i certainPlayers, så den nu er " + certainPlayers.size() + " lang, fordi der var plads");
              //logger.info("Flyttet er:");
              for (String flyt : possiblePlayers) {
                //logger.info(" - " + flyt);
              }

              possiblePlayers = new LinkedList<String>();
              previousNumber = val;
              possiblePlayers.add(key);
            } else {
              // Så har vi flere i possiblePlayers end der kan bruges
              // Vi skal finde et antal af dem og smide over

              Collections.shuffle(possiblePlayers);
              LinkedList<String> randomPlayers = new LinkedList<String>();
              randomPlayers.addAll(possiblePlayers.subList(0, maxPlayersNeeded - currentCertainPlayers));
              for (String ran : randomPlayers) {
                //logger.info("Random player: " + ran);
              }
              certainPlayers.addAll(randomPlayers);
              currentCertainPlayers = certainPlayers.size();

            }
          } else {
            // Så var det samme nummer som før
            possiblePlayers.add(key);
          }

          //logger.info("currentPlayerScore: " + entry.getKey() + "/ ==>" + val);
        } else {
          //logger.info("Vi har nok spillere, og derfor skal vi ikke bruge " + key);
        }
      }

      // Til sidst skal vi rydde op, hvis der er noget i possiblePlayers
      if (possiblePlayers.size() + certainPlayers.size() <= maxPlayersNeeded) {
        // Så er det ok at flytte det direkte over
        certainPlayers.addAll(possiblePlayers);
        currentCertainPlayers = certainPlayers.size();
        //logger.info("2 Vi flyttede " + possiblePlayers.size() + " over i certainPlayers, så den nu er " + certainPlayers.size() + " lang, fordi der var plads");
        //logger.info("2 Flyttet er:");
        for (String flyt : possiblePlayers) {
          //logger.info("2 - " + flyt);
        }
        //possiblePlayers = new LinkedList<String>();
        //previousNumber = val;
        //possiblePlayers.add(key);
      } else {
        // Så har vi flere i possiblePlayers end der kan bruges
        // Vi skal finde et antal af dem og smide over

        Collections.shuffle(possiblePlayers);
        LinkedList<String> randomPlayers = new LinkedList<String>();
        randomPlayers.addAll(possiblePlayers.subList(0, maxPlayersNeeded - currentCertainPlayers));
        for (String ran : randomPlayers) {
          //logger.info("2 Random player: " + ran);
        }
        certainPlayers.addAll(randomPlayers);
        currentCertainPlayers = certainPlayers.size();

      }

      logger.info("Holdet bliver:");
      for (String cp : certainPlayers) {
        logger.info("- " + cp);
      }

      // Let's make some great pairs
      int choosenNumberOfTries = 10;
      int triesSoFar = 0;
      int bestPairsFoundInRound = -1;
      int previousUniquenessScore = Integer.MAX_VALUE;
      LinkedList<Pair> bestFoundPairs = null;

      for (triesSoFar = 0; triesSoFar < choosenNumberOfTries ; triesSoFar++) {
        if (previousUniquenessScore == 0) break;
        // Shuffle them
        Collections.shuffle(certainPlayers);

        int numberOfPlayers = certainPlayers.size();
        int playersForLastTable = numberOfPlayers % 4;
        boolean unevenNumberOfPlayers = ((numberOfPlayers % 2) == 1);

        int index = 0;
        Pair currentPair = null;
        LinkedList<Pair> allPairs = new LinkedList<Pair>();
        for (String player : certainPlayers) {
          if (index % 2 == 0) {
            // Then we have first player in Pair
            currentPair = new Pair();
            currentPair.setPlayer1(player);

            // If the last table has 3 players, just add to allPairs now (we will not get to the else below ever)
            if ((index + 1) == numberOfPlayers) {
              currentPair.setPlayer2(null);
              //logger.info("Vi har nu ramt den sidste spiller i et sæt med tre, så vi tilføjer til allPairs og gør ikke mere (spilleren var: " + player + ")");
              allPairs.add(currentPair);
            }
          } else {
            if (index + 1 == numberOfPlayers && playersForLastTable == 2) {
              // Then we at the last person at the last game with two players
              currentPair.setPlayer2(null);
              allPairs.add(currentPair);
              currentPair = new Pair();
              currentPair.setPlayer1(player);
              currentPair.setPlayer2(null);
              allPairs.add(currentPair);
            } else {
              currentPair.setPlayer2(player);
              allPairs.add(currentPair);
            }
          }
          index++;
        }

        logger.info("=== Forsøg " + triesSoFar + " ==========");
        for (Pair par : allPairs) {
          //logger.info(par.toString());
        }

        // Udregn uniquenessScore
        int uniquenessScore = 0;
        for (Pair par : allPairs) {
          String comb1;
          String comb2;
          if (!(par.getPlayer2() == null) && !par.getPlayer2().equals("null")) {
            comb1 = par.getPlayer1() + "<->" + par.getPlayer2();
            comb2 = par.getPlayer2() + "<->" + par.getPlayer1();
          } else {
            comb1 = par.getPlayer1() + "<->" + par.getPlayer1();
            // On purpose - Matches alone is worth double (to avoid them given to the same player often)
            comb2 = par.getPlayer1() + "<->" + par.getPlayer1();
          }

          Integer currentScoreComb1 = currentBuddyScore.get(comb1);
          if (currentScoreComb1 == null) currentScoreComb1 = 0;
          uniquenessScore = uniquenessScore  + currentScoreComb1;
          logger.info(comb1 + " tilføjede: " + currentScoreComb1 + " point");

          Integer currentScoreComb2 = currentBuddyScore.get(comb2);
          if (currentScoreComb2 == null) currentScoreComb2 = 0;
          uniquenessScore = uniquenessScore  + currentScoreComb2;
          logger.info(comb2 + " tilføjede: " + currentScoreComb2 + " point");

          logger.info("uniquenessScore er nu " + uniquenessScore);


        }

        //logger.info("Tidligere bedste uniquenessScore var " + previousUniquenessScore);
        //logger.info("Denne uniquenessScore er " + uniquenessScore);
        if (uniquenessScore < previousUniquenessScore) {
          previousUniquenessScore = uniquenessScore;
          bestFoundPairs = allPairs;
          logger.info("Så blev der sat nye, bedste par. Disse er: ");
          for (Pair par : bestFoundPairs) {
            logger.info(par.toString());
          }
          bestPairsFoundInRound = triesSoFar;
        } else {
          logger.info("Denne kombination gav " + uniquenessScore + " og var ikke bedre end " + previousUniquenessScore + " fundet i runde " + bestPairsFoundInRound + ". Derfor er parrene stadig som før:");
          //for (Pair par : bestFoundPairs) {
        //    logger.info(par.toString());
          //}
        }
      }


      logger.info("De valgte par er således (fundet i runde " + bestPairsFoundInRound + " med score " + previousUniquenessScore + ")!!!");
      for (Pair par : bestFoundPairs) {
        logger.info(par.toString());
      }

      LinkedList<String> awesomePlayerList = new LinkedList<String>();

      for (Pair par : bestFoundPairs) {
        awesomePlayerList.add(par.getPlayer1());
        awesomePlayerList.add(par.getPlayer2());
      }



      return awesomePlayerList;

    }



    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map ) {
        List<Map.Entry<K, V>> list =
            new LinkedList<Map.Entry<K, V>>( map.entrySet() );
        Collections.sort( list, new Comparator<Map.Entry<K, V>>()
        {
            public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
            {
                return (o1.getValue()).compareTo( o2.getValue() );
            }
        } );

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list)
        {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }
}
