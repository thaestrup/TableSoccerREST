yieldUnescaped '<!DOCTYPE html>'
html {
  head {
    meta(charset:'utf-8')
    title("API")

    meta(name: 'apple-mobile-web-app-title', content: 'Ratpack')
    meta(name: 'description', content: '')
    meta(name: 'viewport', content: 'width=device-width, initial-scale=1')

    link(href: '/images/favicon.ico', rel: 'shortcut icon')
  }
  body {
    header {
      h1 'PLEASE NOTICE - THIS PAGE IS PRESENTLY NOT UP-TO-DATE! (And maybe it won't be until version 2?)'
      p '-----------------------------------------------'
      h1 'Rest API'
      p 'There exist Players, Games and Results'

      p 'Remember to set the request header "Content-Type" to "application/json"'

      p 'Remember to pay attention if the request nned to be an array "[]" or a single object'
    }

    section {
      h2 'Players'
      p 'Implements the HTTP methods: Get, Put, Post and delete'
      p 'As described here: https://en.wikipedia.org/wiki/Representational_state_transfer'
      p 'If you provide a name in the message body of PUT on a named player (Ex: /players/alsk) then the name in the body is ignored.'

      h3 'Get'
      h4 'http://localhost:5050/players/'
      p 'Returns: [{"name":"alsk","playerReady":true,"oprettet":"2016-08-03 13:47:47.0"},{"name":"monn","playerReady":false,"oprettet":"2016-08-03 13:47:50.0"},{"name":"dwp","playerReady":false,"oprettet":"2016-08-03 13:47:55.0"},{"name":"cani","playerReady":false,"oprettet":"2016-08-03 13:47:59.0"},{"name":"frmi","playerReady":false,"oprettet":"2016-08-03 13:56:01.0"},{"name":"frmi1","playerReady":false,"oprettet":"2016-08-03 15:56:01.0"}]'

      h4 'http://localhost:5050/players/alsk'
      p 'returns: {"name":"alsk","playerReady":true,"oprettet":"2019-08-03 17:47:47.0"}'

      h3 'Post'
      h4 'http://localhost:5050/players/'
      p 'Replace all data with given data'
      p 'Input: [{"name":"alsk","playerReady":true,"oprettet":"2016-08-03 13:47:47.0"},{"name":"monn","playerReady":false,"oprettet":"2016-08-03 13:47:50.0"},{"name":"dwp","playerReady":false,"oprettet":"2016-08-03 13:47:55.0"},{"name":"cani","playerReady":false,"oprettet":"2016-08-03 13:47:59.0"},{"name":"frmi","playerReady":false,"oprettet":"2016-08-03 13:56:01.0"},{"name":"frmi1","playerReady":false,"oprettet":"2016-08-03 15:56:01.0"}]'
      p 'Returns text for status'

      h4 'http://localhost:5050/players/alsk'
      p 'Not implemented'

      h3 'Put'
      h4 'http://localhost:5050/players/'
      p 'Overwirte players, if not exsiting then create new players'
      p 'Input: [{"name":"alsk","playerReady":true,"oprettet":"2016-08-03 13:47:47.0"},{"name":"monn","playerReady":false,"oprettet":"2016-08-03 13:47:50.0"},{"name":"dwp","playerReady":false,"oprettet":"2016-08-03 13:47:55.0"},{"name":"cani","playerReady":false,"oprettet":"2016-08-03 13:47:59.0"},{"name":"frmi","playerReady":false,"oprettet":"2016-08-03 13:56:01.0"},{"name":"frmi1","playerReady":false,"oprettet":"2016-08-03 15:56:01.0"}]'
      p 'Returns text for status'

      h4 'http://localhost:5050/players/alsk'
      p 'Overwirte player, if not exsiting then create new player'
      p 'The intput does not contain name, you are able to give it but will be ignored.'
      p 'Input: {"playerReady":true,"oprettet":"2019-08-03 17:47:47.0"}'
      p 'Returns text for status'

      h3 'Delete'
      h4 'http://localhost:5050/players/'
      p 'Delete all players'
      p 'Returns text for status'

      h4 'http://localhost:5050/players/alsk'
      p 'Delete one player'

    }

    section {
      h2 'Games'
      h4 'http://localhost:5050/games/'
      p 'same as with players, the game object is just like:'
      p '{"id":262,"player_red_1":"MOKL","player_red_2":"fadfsa","player_blue_1":"asdasd","player_blue_2":"cjo","lastUpdated":"2016-10-22 21:21:30.0","match_winner":"red","points_at_stake":1,"winning_table":1}'

      h3 'POST'
      h4 'http://localhost:5050/games/'
      p 'Adds the given games'
      p 'Example input'
      p '[{"player_red_1":"MOKL","player_red_2":"fadfsa","player_blue_1":"asdasd","player_blue_2":"cjo","lastUpdated":"2016-10-22 21:21:30.0","match_winner":"red","points_at_stake":1,"winning_table":1},{"player_red_1":"peeh","player_red_2":"jmn","player_blue_1":"KRBA","player_blue_2":"Q1RS","lastUpdated":"2016-10-22 21:21:36.0","match_winner":"red","points_at_stake":1,"winning_table":1},{"player_red_1":"peeh","player_red_2":"jmn","player_blue_1":"KRBA","player_blue_2":"Q1RS","lastUpdated":"2016-10-22 21:21:37.0","match_winner":"red","points_at_stake":1,"winning_table":1},{"player_red_1":"peeh","player_red_2":"jmn","player_blue_1":"KRBA","player_blue_2":"Q1RS","lastUpdated":"2016-10-22 21:21:37.0","match_winner":"blue","points_at_stake":1,"winning_table":1}]'
      p 'Example output'
      p '{"newGameIDs":["17","18","19","20"]}'
      }

     section {
     h2 'StatisticsPlayersLastPlayed'
     p 'Only implemented GET'
     p 'Returns all players represented in games and when they last played, not sorted.'
     h3 'GET'
     h4 'http://localhost:5050/statisticsPlayersLastPlayed'
     p 'Returns'
     p '{"fgdgdfg":1480278653000,"monn":1480278653000,"dwp111":1480278653000,"null":1480262909000,"cani":1480277999000,"frmi":1480277999000,"fgdgdfg1":1480278653000,"dwp1":1480277976000,"frmi1":1480278653000,"asfasfaf":1480278653000,"alsk":1480278653000}'
     }


    section {
      h2 'RandomTournament'
      p 'Only implemented POST'
      h3 'POST'
      p 'http://localhost:5050/tournament/randomTournament'
      p 'Generates numberOfGames between the players: Add nulls when there are no players left to take the seats in the game'
      p 'Will never duplicate any player in the games created, will stop if there are no players left and will fill out last games with nulls'
      p 'Example input'
      p '{"numberOfGames":2,"players":[{"name":"dwp111"},{"name":"cani111"},{"name":"frmi1"},{"name":"frmi1"},{"name":"alsk"},{"name":"monn"},{"name":"asfasfaf"},{"name":"fgdgdfg"},{"name":"fgdgdfg1"}]}'
      p 'Example output'
      p '[{"id":-1,"player_red_1":"dwp111","player_red_2":"frmi1","player_blue_1":"monn","player_blue_2":"alsk","lastUpdated":"2016-11-28 19:18:07.846","match_winner":"","points_at_stake":-1,"winning_table":-1},{"id":-1,"player_red_1":"fgdgdfg1","player_red_2":"asfasfaf","player_blue_1":"cani111","player_blue_2":"frmi1","lastUpdated":"2016-11-28 19:18:07.868","match_winner":"","points_at_stake":-1,"winning_table":-1}]'
     }


    section {
      h2 'LastFirstTournament'
      p 'Only implemented POST'
      h3 'POST'
      p 'http://localhost:5050/tournament/lastFirstTournament'
      p 'Generates numberOfGames between the players: Add nulls when there are no players left to take the seats in the game'
      p 'Players that never played goes first, then in sorted order where the players that played farthest into the past goes first. '
      p 'Will never duplicate any player in the games created, will stop if there are no players left and will fill out last games with nulls'
      p 'Example input'
      p '{"numberOfGames":2,"players":[{"name":"dwp111"},{"name":"cani111"},{"name":"frmi1"},{"name":"frmi1"},{"name":"alsk"},{"name":"monn"},{"name":"asfasfaf"},{"name":"fgdgdfg"},{"name":"fgdgdfg1"}]}'
      p 'Example output'
      p '[{"id":-1,"player_red_1":"dwp111","player_red_2":"frmi1","player_blue_1":"monn","player_blue_2":"alsk","lastUpdated":"2016-11-28 19:18:07.846","match_winner":"","points_at_stake":-1,"winning_table":-1},{"id":-1,"player_red_1":"fgdgdfg1","player_red_2":"asfasfaf","player_blue_1":"cani111","player_blue_2":"frmi1","lastUpdated":"2016-11-28 19:18:07.868","match_winner":"","points_at_stake":-1,"winning_table":-1}]'
     }

     section {
     h2 'PointsPrPlayer'
     p 'Only implemented GET and POST'
     p 'Returns all players represented in games and score, number of games, name and ranking relative to all other players'
     p 'Sorted on ranking'
     p 'Winner gets one point pr. win.'
     h3 'GET'
     h4 'http://localhost:5050/pointsPrPlayer'
     p 'Returns'
     p '[{"position":0,"points":10,"numberOfGames":15,"name":"jmn"},{"position":0,"points":10,"numberOfGames":15,"name":"peeh"},{"position":1,"points":5,"numberOfGames":15,"name":"Q1RS"},{"position":1,"points":5,"numberOfGames":15,"name":"KRBA"},{"position":1,"points":5,"numberOfGames":5,"name":"MOKL"},{"position":1,"points":5,"numberOfGames":5,"name":"fadfsa"}]'

     h3 'POST'
     h4 'http://localhost:5050/pointsPrPlayer'
     p 'Example input'
     p '{"winnerPoints":3,"loserPoints":-1,"drawPoints":1}'
     p 'Returns'
     p '[{"position":0,"points":30,"numberOfGames":15,"name":"jmn"},{"position":0,"points":30,"numberOfGames":15,"name":"peeh"},{"position":1,"points":15,"numberOfGames":5,"name":"MOKL"},{"position":1,"points":15,"numberOfGames":5,"name":"fadfsa"},{"position":2,"points":5,"numberOfGames":15,"name":"Q1RS"},{"position":2,"points":5,"numberOfGames":15,"name":"KRBA"},{"position":3,"points":-5,"numberOfGames":5,"name":"asdasd"},{"position":3,"points":-5,"numberOfGames":5,"name":"cjo"}]'
     }

    footer {}
  }
}
