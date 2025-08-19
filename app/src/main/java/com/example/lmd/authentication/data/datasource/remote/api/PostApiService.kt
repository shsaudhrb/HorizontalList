package com.example.lmd.authentication.data.datasource.remote.api

import com.example.lmd.authentication.data.model.Post
import retrofit2.http.GET

interface PostApiService {
    @GET("posts")
    suspend fun getPosts(): List<Post>
}
