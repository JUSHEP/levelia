package com.levelia.levelia.di

import com.levelia.levelia.data.remote.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/** Un único cliente de Supabase para toda la app (auth + base de datos). */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = SupabaseConfig.URL,
        supabaseKey = SupabaseConfig.CLAVE_PUBLICA,
    ) {
        // Columnas nuevas en la base no rompen las versiones de la app ya instaladas (por defecto el JSON es estricto)
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
        install(Auth)
        install(Postgrest)
    }
}
