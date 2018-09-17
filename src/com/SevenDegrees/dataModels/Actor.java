package com.SevenDegrees.dataModels;

import java.util.List;

public class Actor 
{
	//Their Facebook user id.
	private String actorName;
	
	private String linkToTheirPage;
	
	private List<Movie> movies;
	
	public Actor(String actorNameIn, String linkToTheirPageIn, List<Movie> moviesIn)
	{
		actorName = actorNameIn;
		linkToTheirPage = linkToTheirPageIn;
		movies = moviesIn;
	}
	
	public String getActorName()
	{
		return actorName;
	}
	
	public String getLinkToTheirPage()
	{
		return linkToTheirPage;
	}
	
	public List<Movie> getMovies()
	{
		return movies;
	}
	
	public void printDebugMessage()
	{
		System.out.println("Actor: " + getActorName() + " | Link: " + getLinkToTheirPage());
		System.out.println("Movies: ");
		for(int i = 0; i < movies.size(); i++)
		{
			System.out.println(movies.get(i).getMovieName() + " -- " + movies.get(i).getMovieLink());
		}
	}
}
