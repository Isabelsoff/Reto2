package com.isa.reto2.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class ErResponse(val result: String?, val rates: Map<String, Double>?)

interface ErApiService {
    @GET("v6/latest/USD")
    suspend fun latestUsd(): ErResponse

    companion object {
        val instance: ErApiService by lazy {
            Retrofit.Builder()
                .baseUrl("https://open.er-api.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ErApiService::class.java)
        }
    }
}
