//this configures CORS(cross-origin resource sharing

//CORS = Cross-Origin Resource Sharing
//It allows your frontend and backend to communicate when they are running on different origins
//frontend can call backend
//public class corsConfig implements WebMVCConfigurer
package report.config;

import org.springframework.context.annotation.Configuration;//makes this as a spring configuration class
import org.springframework.web.servlet.config.annotation.CorsRegistry; // used to register CORS rules
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; //allows you to customize Spring MVC behavior.

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**") // apply rules to /all endpoints -- GLOBAL CORS configuration
                .allowedOrigins(
                        "*"
                ) // allow request from any frontend origin
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}