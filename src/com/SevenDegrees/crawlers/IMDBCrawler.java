package com.SevenDegrees.crawlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

import com.SevenDegrees.dataModels.Actor;
import com.SevenDegrees.dataModels.Movie;

public class IMDBCrawler 
{
	//The base IMDB Url.
	private final String IMDB_URL = "https://www.imdb.com";
	
	//The URL to do an IMDB search.
	//The format is SEARCH_URL, then the name or movie to search for, with spaces replaced with +'s, and then the SEARCH_SUFFIX at the end.
	private final String SEARCH_URL = IMDB_URL + "/find?ref_=nv_sr_fn&q=";
	private final String SEARCH_SUFFIX = "&s=all";
	
	//IDENTIFIERS
	//Finds the name and link of a search result.
	private final String NAME_AND_LINK_IDENTIFIER = "result_text";
	//Used to find any links.
	private final String LINK_IDENTIFIER = "href=\"";
	//Used to find movies on an actor's page.
	private final String MOVIE_IDENTIFIER = "filmo-row";
	
	//SEARCH SETTINGS.
	//The max amount of movie included from a single actor. (In order of their most recent movies).
	private final int MAX_MOVIE_COUNT = 100;
	
	//The max amount of actors used from a single movie (In order of their default IMDB appearance)
	private final int MAX_ACTORS_PER_MOVIE = 20;
	
	//This decides whether the crawler will just find a single match and stop, or keep printing.
	private final boolean STOP_ON_FIRST_MATCH = false;
	
	//This can make it less fun when turned on, as it includes shows that the actors may not have been in at the same time, such as sketch shows, or guest cameos.
	private final boolean INCLUDE_TV_SHOWS = false;
	
	//This will filter out movies/tv shows that according to IMDB are still in production.
	private final boolean INCLUDE_IN_PRODUCTION_CONTENT = false;
	
	public IMDBCrawler()
	{
		//Gets the actors names from console.
		Scanner keyboard = new Scanner(System.in);
		System.out.print("Enter the starting Actor/Actress name: ");
		String startingActor = keyboard.nextLine();
		System.out.print("Enter the ending Actor/Actress name: ");
		String endingActor = keyboard.nextLine();
		
		beginSearch(startingActor, endingActor);
		keyboard.close();
	}
	
	private void beginSearch(String startingActorName, String endingActorName)
	{
		System.out.println("Searching from '" + startingActorName + "' to '" + endingActorName + "'.");
		Actor startingActor = getActor(startingActorName);
		Actor endingActor = getActor(endingActorName);
		
		//////////////////FIRST DEGREE CHECK
		List<Movie> fromActorsMovies = startingActor.getMovies();
		List<Movie> toActorsMovies = endingActor.getMovies();
		for(Movie fromMovie : fromActorsMovies)
		{
			for(Movie toMovie : toActorsMovies)
			{
				if(fromMovie.getMovieName().equals(toMovie.getMovieName()))
				{
					System.out.println(startingActor.getActorName() + " was in " + fromMovie.getMovieName() + " with " + endingActor.getActorName());
 
				}
			}
		}
		System.out.println("These actors do NOT have a single step relationship.");
		//////////////////END OF FIRST DEGREE CHECK
		
		
		/* This is the beginning of the two step relationship.
		 * It checks all of the actors that the fromActor has worked with, to see if they share a movie with the toActor.
		 * For this to check everything, it should also check the other direction obviously, however it currently takes so long that it's not worth it.
		 */
		for(Movie fromMovie : fromActorsMovies)
		{
			System.out.println("Checking against actors from: " + fromMovie.getMovieName());
			List<Actor> actorsInThisMovie = getActorsByMovie(fromMovie.getMovieLink());
			for(Actor actorFromMovie : actorsInThisMovie)
			{
				for(Movie movieActorHasBeenIn : actorFromMovie.getMovies())
				{
					for(Movie movieToActorHasBeenIn : toActorsMovies)
					{
						if(movieActorHasBeenIn.getMovieName().equals(movieToActorHasBeenIn.getMovieName()))
						{
							//Now we need to re-find the movie shared between fromActor and middleman actor.
							Movie movieShared = getSharedMovie(startingActor, actorFromMovie);
							System.out.println(startingActor.getActorName() + " was in " + movieShared.getMovieName() + " with " + actorFromMovie.getActorName() + " who was in " + movieActorHasBeenIn.getMovieName() + " with " + endingActor.getActorName());
							if(STOP_ON_FIRST_MATCH)
							{
								return;
							}
						}
					}
				}
			}
		}
		System.out.println("Tried every option from FromActor's cast mates, to toActor.");
		System.out.println("Finished.");
	}
	
	/**
	 * Returns a movie that is shared by both of the actors.
	 */
	private Movie getSharedMovie(Actor actor1, Actor actor2)
	{
		for(Movie movie1 : actor1.getMovies())
		{
			for(Movie movie2 : actor2.getMovies())
			{
				if(movie1.getMovieName().equals(movie2.getMovieName()))
				{
					return movie1;
				}
			}
		}
		return new Movie("NULL", "");
	}
	
	/**
	 * Returns an Actor object, retrieved from IMDB.
	 * @param actorName - The actors full name, first and last name.
	 * @return - The actor object.
	 */
	private Actor getActor(String actorName)
	{
		//Searches for an actor and gets their webpage.
		String webpage = getWebPage(SEARCH_URL + actorName.toLowerCase().replace(' ', '+') + SEARCH_SUFFIX);
		int profileInfoLink = webpage.indexOf(NAME_AND_LINK_IDENTIFIER);
		int profileLinkIndex = webpage.indexOf(LINK_IDENTIFIER, profileInfoLink) + LINK_IDENTIFIER.length();
		String profileLink = IMDB_URL + webpage.substring(profileLinkIndex, webpage.indexOf("\"", profileLinkIndex));
		

		return new Actor(actorName, profileLink, getMoviesByActor(profileLink));
	}
	
	/**
	 * Returns all of the credits that a single actor has.
	 * @param linkToActorPage - A link to the actors IMDB page.
	 * @return - A list of all of their movies.
	 */
	private List<Movie> getMoviesByActor(String linkToActorPage)
	{
		//Get their top X Filmography credits.
		List<Movie> movies = new ArrayList<Movie>();
		String actorWebpage = getWebPage(linkToActorPage);
		int counter = MAX_MOVIE_COUNT;
		int currentIndex = 0;
		while(counter>0)
		{
			currentIndex = actorWebpage.indexOf(MOVIE_IDENTIFIER, currentIndex);
			if(currentIndex >= 0)
			{
				//Filters out non-acting credits.
				int indexForActorCheck = actorWebpage.indexOf("id=\"", currentIndex) + "id=\"".length();
				String actorCheck = actorWebpage.substring(indexForActorCheck, indexForActorCheck+"actor".length());
				if(!actorCheck.equals("actor") && !actorCheck.equals("actre")) //Checks for actor and actress.
				{
					currentIndex = indexForActorCheck;
				}
				else
				{
					int indexForMovieLink = actorWebpage.indexOf(LINK_IDENTIFIER, currentIndex) + LINK_IDENTIFIER.length();
		
					//check if the movie or tv show is valid.
					int startOfMovieProgress = actorWebpage.indexOf("class=\"", indexForMovieLink)+"class=\"".length();
					int endOfMovieProgress = actorWebpage.indexOf("\"", startOfMovieProgress);
					String movieProgress = actorWebpage.substring(startOfMovieProgress, endOfMovieProgress);
					if((movieProgress.equals("in_production") && !INCLUDE_IN_PRODUCTION_CONTENT) || (movieProgress.equals("filmo-episodes") && !INCLUDE_TV_SHOWS))
					{
						//We have found a tv show, or an in-production movie/show, so bail.
						currentIndex = indexForMovieLink;
					}
					else
					{
						//Get the movies link.
						String movieLink = IMDB_URL + actorWebpage.substring(indexForMovieLink, actorWebpage.indexOf("\"", indexForMovieLink));
						
						//Get the movies name.
						String movieName = actorWebpage.substring(actorWebpage.indexOf(">", indexForMovieLink)+1, actorWebpage.indexOf("<", indexForMovieLink));
						
						//Gets the characters name (if there is one listed).
						int startOfName = actorWebpage.indexOf("<br/>", actorWebpage.indexOf("<", indexForMovieLink))+"<br/>".length();
						int endOfName = actorWebpage.indexOf("<", startOfName);
						String characterName = actorWebpage.substring(startOfName, endOfName);
						
						if(characterName.equals(""))
						{
							characterName = "N/A";
						}
						//System.out.println("Movie : " + movieName + " as " + characterName + " | Link: " + IMDB_URL + movieLink);
						currentIndex = indexForMovieLink;
						movies.add(new Movie(movieName, movieLink));
						counter--;
					}
				}
			}
			else
			{
				counter = 0;
			}
		}
		return movies;
	}
	
	//Should ensure link is to the full cast page instead of the normal page to maximise chance of positive match.
	/**
	 * Returns all of the actors from a movie. This function is very intense because it also gathers the movies that each of those actors are in.
	 * @param linkToMoviePage - The full link to the movie page.
	 * @return - A list of actors that belong to the movie.
	 */
	private List<Actor> getActorsByMovie(String linkToMoviePage)
	{
		String webpage = getWebPage(linkToMoviePage);
		List<Actor> actors = new ArrayList<Actor>();
		//Navigate to primary_photo, and keep taking link and name until there are no primary_photo's left.
		int currentIndex = 0;
		while(currentIndex != -1)
		{
			currentIndex = webpage.indexOf("primary_photo", currentIndex);
			if(currentIndex != -1)
			{
				int startOfLink = webpage.indexOf(LINK_IDENTIFIER,  currentIndex) + LINK_IDENTIFIER.length();
				int endOfLink = webpage.indexOf(">", startOfLink)-1;
				String link = IMDB_URL + (webpage.substring(startOfLink, endOfLink));
				
				currentIndex = endOfLink;
				int startOfName = webpage.indexOf("title=\"", currentIndex) + "title=\"".length();
				int endOfName = webpage.indexOf("\"", startOfName);
				String name = webpage.substring(startOfName, endOfName);
				Actor newActor = new Actor(name, link, getMoviesByActor(link));
				actors.add(newActor);
				if(actors.size() >= MAX_ACTORS_PER_MOVIE)
				{
					return actors;
				}

			}
		}
		return actors;
	}
	
	/**
	 * Returns the content of a webpage in a single string.
	 * @param urlString - The URL to request the webpage from.
	 * @return - The content of the webpage as a string.
	 */
	private String getWebPage(String urlString)
	{
		String webPage = "";
		URL url; 
		InputStream is = null; 
		BufferedReader br; 
		String line; 
		try 
		{ 
			url = new URL(urlString);
			is = url.openStream(); // throws an IOException 
			br = new BufferedReader(new InputStreamReader(is)); 
			while ((line = br.readLine()) != null) 
			{ 
				webPage += line; 
			} 

		} 
		catch (MalformedURLException mue) 
		{ 
			mue.printStackTrace(); 
		} 
		catch (IOException ioe) 
		{ 
			ioe.printStackTrace(); 
		} 
		finally 
		{ 
			try 
			{
				if (is != null) 
					is.close(); 
			} 
			catch (IOException ioe) 
			{ // nothing to see here 
				  
			} 
		}
		return webPage;
	}
}
