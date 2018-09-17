package com.SevenDegrees.dataModels;

public class Movie 
{
	private String movieName;
	
	private String movieLink;
	
	public Movie(String movieNameIn, String movieLinkIn)
	{
		movieName = movieNameIn;
		movieLink = movieLinkIn;
	}
	
	public String getMovieName()
	{
		return movieName;
	}
	
	public String getMovieLink()
	{
		return movieLink;
	}
	
}
