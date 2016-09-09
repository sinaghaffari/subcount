**Note:** This functionality is supported by Nightbot using `$(twitch subcount)` in a command string.

# subcount

## About
subcount is a simple web service written in Scala, using the Play! Framework and ElasticSearch. It's supposed to be a simple way of providing channel owners with up to date counts of their subscriber count in a format that can be used in nightbot commands.

## Usage
**Note:** This will only work in productions environments with [nginx](https://www.nginx.com/) already configured!

1. Create a twitch developer application [here](https://www.twitch.tv/kraken/oauth2/clients/new).
2. Set up an elasticsearch instance running at `localhost:9300`
3. Create the file `keys.conf` within the `conf` directory.
  - `keys.conf` needs to contain
    1. The Client ID of your twitch developer application.
    2. The Client Secret of your twitch developer application.
    3. The same Redirect URI registered to your twitch developer application.
    4. The scope you would like to request. This should **always** be `channel_subscriptions`.
  - Here is an example:
  ```
  subcount {
    clientID = "your client id"
    redirectURI = "your redirect uri"
    scope = "channel_subscriptions"
    clientSecret = "your client secret"
  }
  ```
4. Run the application by typing `sbt compile run` into your terminal.
  - **Note:** Obviously you'll need [SBT](http://www.scala-sbt.org/) or [Activator](https://www.lightbend.com/activator/download).

5. Go to `http://www.<Your Domain Here>.com/subcount/signin` to register and authorize your channel.
6. Go to `http://www.<Your Domain Here>.com/subcount/<Your Channel Name Here>` to view your subcount.
