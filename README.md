# http4s4tw

A client-server experiment with [http4s](https://http4s.org).

UI components are built with React (via [Slinky](https://slinky.dev)) and state is managed both within
the React components and within the main [cats-effect](https://typelevel.org/cats-effect/) `IOApp`.

## Running the client

At the `sbt` prompt, type:

```
> project client
> Compile / fastOptJS / webpack
```

Then open `project/main.html` in a browser.

## Running the server

The server side is a work in progress.
