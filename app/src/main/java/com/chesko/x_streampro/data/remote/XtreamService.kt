package com.chesko.x_streampro.data.remote

import com.chesko.x_streampro.data.model.Category
import com.chesko.x_streampro.data.model.LiveStream
import com.chesko.x_streampro.data.model.LoginResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamService {

    @GET("player_api.php")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): LoginResponse

    @GET("player_api.php")
    suspend fun getCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<Category>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams"
    ): List<LiveStream>

    @GET("player_api.php")
    suspend fun getLiveStreamsByCategory(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String,
        @Query("action") action: String = "get_live_streams"
    ): List<LiveStream>
}