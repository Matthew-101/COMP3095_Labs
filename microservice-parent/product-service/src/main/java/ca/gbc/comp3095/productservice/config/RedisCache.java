package ca.gbc.comp3095.productservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCache {


    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {


        // ============================================================
        // STEP 1: Build a Jackson ObjectMapper for our Redis values
        //      WHY: Our cache stores mixed value types in the SAME cache
        //      (String id, ProductResponse, List<ProductResponse>)
        // =============================================================

        // In generic cache we typically allow all subtypes
        // If you wanted to be sticker, allow any of your base classes
         var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();


         ObjectMapper mapper = JsonMapper.builder()
                 .activateDefaultTypingAsProperty(
                  ptv,
                         DefaultTyping.NON_FINAL_AND_RECORDS,  //NON_FINAL alone skips records -- this config added them
                         "@class"
                 )
                 .build();


        // ============================================================
        // STEP2: Choose a Serializer
        //      - Keys: plain strings
        //      - Values: Jackson JSON with our mapper
        // ===========================================================
        var keySerializer = new StringRedisSerializer();
        var valueSerializer = new JacksonJsonRedisSerializer<>(mapper, Object.class);



        // ================================================================
        // STEP3: Define cache defaults (TTL, null handling, serializers)
        // ================================================================
        RedisCacheConfiguration defaults =  RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1))      // why 1 minute - Demo TTL  - normally you would fine tune, per domain
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(keySerializer)
                )

                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
                );



        // ================================================================
        // STEP 4: Build the CacheManager
        //    We explicitly name our cache "PRODUCT_CACHE"
        // ================================================================
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaults)
                .withCacheConfiguration("PRODUCT_CACHE", defaults)
                .build();

    }


}
