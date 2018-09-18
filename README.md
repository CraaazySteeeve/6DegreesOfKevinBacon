# 6DegreesOfKevinBacon
A Java app which uses IMDB to find connections between two actors.

WORK IN PROGRESS

Currently when you run the project in an IDE it will ask for two actor names in console.
Once provided, it will search through IMDB and tell you either a movie they have been, or a 2-degree connection from the first actor to the second.
It is currently quite slow but I've found, even in its limited state, it finds atleast one connection in a reasonable time depending on the actors.

FUTURE TASKS:
- Add timing logs to find timesinks and optimize them.
- Add multithreading, to maximise search speed.
- Rework the searching so it happens recursively (instead of writing each degree by hand) (This is not useful until it's MUCH faster though).
- Also rework how the chain of actors is saved, right now it's re-calculated which is expensive.
- Optimize the matching/searching somehow.
- Make sure I am not breaking some sort of web ettiquite by using IMDB like this.
