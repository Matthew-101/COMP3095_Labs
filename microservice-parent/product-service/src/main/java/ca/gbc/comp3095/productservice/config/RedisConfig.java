package ca.gbc.comp3095.productservice.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // ===============================================================
        // STEP 1: Build a Jackson ObjectMapper for Redis values
        //         WHY: Our cache stores mixed value types in the SAME cache
        //              (String ids, ProductResponse, List<ProductResponse>).
        //              To re-hydrate them correctly, we include type info.
        //
        // DECISION: Use PROPERTY-style default typing so the JSON contains
        //           {"@class":"fully.qualified.Type", ...}. This keeps
        //           writer/reader in sync for lists, strings, maps, etc.
        //
        // NOTE: This is safe here because the cache is our own data and we
        //       control both serialization and deserialization.
        // ===============================================================
        ObjectMapper mapper = new ObjectMapper();
        var ptv = BasicPolymorphicTypeValidator.builder()
                // Teaching note: in a generic cache we allow all subtypes.
                // If you want to be stricter, whitelist your base packages.
                .allowIfSubType(Object.class)
                .build();

        mapper.activateDefaultTyping(
                ptv,
                ObjectMapper.DefaultTyping.EVERYTHING,   // include type for all values (including List/String)
                JsonTypeInfo.As.PROPERTY                 // embeds "@class" property
        );

        // ===============================================================
        // STEP 2: Choose serializers
        //         - Keys: plain strings (human-readable keys in Redis)
        //         - Values: Generic Jackson JSON with our mapper (adds @class)
        // ===============================================================
        var keySerializer   = new StringRedisSerializer();
        var valueSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        // ===============================================================
        // STEP 3: Define cache defaults (TTL, null handling, serializers)
        // ===============================================================
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // demo TTL; tune per domain
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(keySerializer)
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
                );

        // ===============================================================
        // STEP 4: Build the CacheManager
        //         We explicitly name PRODUCT_CACHE but it inherits defaults.
        // ===============================================================
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withCacheConfiguration("PRODUCT_CACHE", defaults)
                .build();
    }
}
