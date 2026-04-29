//this configures CORS(cross-origin resource sharing
//frontend can call backend
//public class corsConfig implements WebMVCConfigurer
package report.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")
                .allowedOrigins(
                        "*"
                )
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}