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
    }

    section {
      h2 'Players'
      p 'Implements the HTTP methods: Get, Put, Post and delete'
      p 'As described here: https://en.wikipedia.org/wiki/Representational_state_transfer'
      p 'If you provide a name in the message body of PUT on a named player (Ex: /players/alsk) then the name in the body is ignored.'
      p '[{"name":"alsk","playerReady":true,"oprettet":"2016-08-03 13:47:47.0"},{"name":"monn","playerReady":false,"oprettet":"2016-08-03 13:47:50.0"},{"name":"dwp","playerReady":false,"oprettet":"2016-08-03 13:47:55.0"},{"name":"cani","playerReady":false,"oprettet":"2016-08-03 13:47:59.0"},{"name":"frmi","playerReady":false,"oprettet":"2016-08-03 13:56:01.0"},{"name":"frmi1","playerReady":false,"oprettet":"2016-08-03 15:56:01.0"}]'
    }

    footer {}
  }
}
