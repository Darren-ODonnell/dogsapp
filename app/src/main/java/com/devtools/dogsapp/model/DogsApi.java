package com.devtools.dogsapp.model;

import java.util.List;

import io.reactivex.Single;
import retrofit2.http.GET;

public interface DogsApi {
    String host = "";
    String endpoint = "DevTides/dogsApi/master/dogs.json";

    @GET(endpoint)
     Single<List<DogBreed>> getDogs();
}
