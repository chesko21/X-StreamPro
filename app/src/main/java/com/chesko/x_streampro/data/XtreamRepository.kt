package com.chesko.x_streampro.data

import com.chesko.x_streampro.data.model.Category
import com.chesko.x_streampro.data.model.LiveStream
import com.chesko.x_streampro.data.model.LoginResponse
import com.chesko.x_streampro.data.remote.RetrofitClient

class XtreamRepository {

    suspend fun login(baseUrl: String, user: String, pass: String): LoginResponse {
        return RetrofitClient.createService(baseUrl).login(user, pass)
    }

    suspend fun getCategories(baseUrl: String, user: String, pass: String): List<Category> {
        return RetrofitClient.createService(baseUrl).getCategories(user, pass)
    }

    suspend fun getLiveStreams(baseUrl: String, user: String, pass: String, categoryId: String? = null): List<LiveStream> {
        val service = RetrofitClient.createService(baseUrl)
        return if (categoryId == null) {
            service.getLiveStreams(user, pass)
        } else {
            service.getLiveStreamsByCategory(user, pass, categoryId)
        }
    }
}