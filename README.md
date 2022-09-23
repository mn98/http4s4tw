# http4s4tw

A client-server experiment with [http4s](https://http4s.org).

UI components are built with React (via [Slinky](https://slinky.dev)) or [Calico](https://www.armanbilge.com/calico/). State is managed both within
the React components and within the main [cats-effect](https://typelevel.org/cats-effect/) `IOApp`.

## Compiling the client and running the server

At the `sbt` prompt, type:

```
> Compile / fastOptJS / webpack
> server / Compile / run
```

Then open `project/main.html` in a browser.
