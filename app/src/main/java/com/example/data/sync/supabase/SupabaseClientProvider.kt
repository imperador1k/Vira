package com.example.data.sync.supabase

import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Provides a lazily initialized, singleton SupabaseClient.
 * Credentials are read safely from BuildConfig (populated from local.properties or CI environment).
 * If credentials are not configured, returns null to allow seamless offline/local-only operation.
 */
object SupabaseClientProvider {

    @Volatile
    private var instance: SupabaseClient? = null

    fun getClient(): SupabaseClient? {
        val url = BuildConfig.SUPABASE_URL.trim()
        val key = BuildConfig.SUPABASE_KEY.trim()

        if (url.isBlank() || key.isBlank()) {
            return null
        }

        return instance ?: synchronized(this) {
            instance ?: createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = key
            ) {
                install(Postgrest)
                install(Auth)
            }.also { instance = it }
        }
    }
}
