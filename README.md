# Dribbble most likers
Calculate Dribbble stats using dribble public api.

## Task
1. For given Dribbble user find all followers
2. For each follower find all shots
3. For each shot find all "likers"
4. Calculate Top10 «likers». People with greater like count descending.

Implement an api endpoint where user login is a parameter. Ex. http://0.0.0.0:9000/top10?login=alagoon
Output the results as json.

## Technologies used
* Scala
* Play!
* Akka
