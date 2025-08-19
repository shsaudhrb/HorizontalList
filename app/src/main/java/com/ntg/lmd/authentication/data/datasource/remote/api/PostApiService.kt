package com.ntg.lmd.authentication.data.datasource.remote.api

import com.ntg.lmd.authentication.data.model.Post
import retrofit2.http.GET

interface PostApiService {
    @GET("posts")
    suspend fun getPosts(): List<Post>
}
