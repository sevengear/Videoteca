package com.example.videoteca;

import java.util.List;

/**
 * Created by Miguel Á. Núñez on 22/06/2018.
 */
public final class MovieList {
    public static List<Movie> list;

    private static Movie buildMovieInfo(String category, String title,
                                        String description, String studio,
                                        String videoUrl, String cardImageUrl,
                                        String bgImageUrl) {
        Movie movie = new Movie();
        movie.setId(list.size());
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setStudio(studio);
        movie.setCategory(category);
        movie.setCardImageUrl(cardImageUrl);
        movie.setBackgroundImageUrl(bgImageUrl);
        movie.setVideoUrl(videoUrl);
        return movie;
    }
}