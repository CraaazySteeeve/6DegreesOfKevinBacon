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
import java.util.Stack;

import com.SevenDegrees.dataModels.Actor;
import com.SevenDegrees.dataModels.Movie;

public class IMDBCrawler 
{
	private final String SEARCH_URL = "https://www.imdb.com/find?ref_=nv_sr_fn&q=";
	private final String SEARCH_SUFFIX = "&s=all";
	
	private final String IMDB_URL = "https://www.imdb.com";
	

	private final String nameAndLinkFinder = "result_text";
	private final String linkFinder = "href=\"";
	
	private final String movieFinder = "filmo-row";
	
	private final int MAX_MOVIE_COUNT = 70;
	
	private final int MAX_ACTORS_PER_MOVIE = 20;
	
	private final String actorFrom = "Lindsay Lohan";
	private final String actorTo = "Chris Pratt";
	
	public IMDBCrawler()
	{
		Actor startingActor = getActor(actorFrom);
		Actor endingActor = getActor(actorTo);
		List<Movie> fromActorsMovies = startingActor.getMovies();
		List<Movie> toActorsMovies = endingActor.getMovies();
		
		for(Movie fromMovie : fromActorsMovies)
		{
			for(Movie toMovie : toActorsMovies)
			{
				if(fromMovie.getMovieName().equals(toMovie.getMovieName()))
				{
					System.out.println(actorFrom + " was in " + fromMovie.getMovieName() + " with " + actorTo);
					return;
				}
			}
		}
		System.out.println("These actors do NOT have a single step relationship.");
		
		/* This is the beginning of the two step relationship.
		 * It checks all of the actors that the fromActor has worked with, to see if they share a movie with the toActor.
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
							System.out.println(actorFrom + " was in " + movieShared.getMovieName() + " with " + actorFromMovie.getActorName() + " who was in " + movieActorHasBeenIn.getMovieName() + " with " + actorTo);
						}
					}
				}
			}
		}
		System.out.println("Actor compilation for second step is complete.");
	}
	
	public Movie getSharedMovie(Actor actor1, Actor actor2)
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
	public Actor getActor(String actorName)
	{
		//Searches for an actor and gets their webpage.
		String webpage = getWebPage(SEARCH_URL + actorName.toLowerCase().replace(' ', '+') + SEARCH_SUFFIX);
		int profileInfoLink = webpage.indexOf(nameAndLinkFinder);
		int profileLinkIndex = webpage.indexOf(linkFinder, profileInfoLink) + linkFinder.length();
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
			currentIndex = actorWebpage.indexOf(movieFinder, currentIndex);
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
					int indexForMovieLink = actorWebpage.indexOf(linkFinder, currentIndex) + linkFinder.length();
		
					//check if the movie or tv show is valid.
					int startOfMovieProgress = actorWebpage.indexOf("class=\"", indexForMovieLink)+"class=\"".length();
					int endOfMovieProgress = actorWebpage.indexOf("\"", startOfMovieProgress);
					String movieProgress = actorWebpage.substring(startOfMovieProgress, endOfMovieProgress);
					if(movieProgress.equals("in_production") || movieProgress.equals("filmo-episodes"))
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
				int startOfLink = webpage.indexOf(linkFinder,  currentIndex) + linkFinder.length();
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
