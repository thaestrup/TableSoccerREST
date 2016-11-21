yieldUnescaped '<!DOCTYPE html>'
html {
  head {
    meta(charset:'utf-8')
    title("Ratpack: $title")

    meta(name: 'apple-mobile-web-app-title', content: 'Ratpack')
    meta(name: 'description', content: '')
    meta(name: 'viewport', content: 'width=device-width, initial-scale=1')

    link(href: '/images/favicon.ico', rel: 'shortcut icon')
  }
  body {
    header {
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

    footer {}
  }
}
