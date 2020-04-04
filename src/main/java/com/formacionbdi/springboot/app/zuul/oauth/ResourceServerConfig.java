package com.formacionbdi.springboot.app.zuul.oauth;

import java.util.Arrays;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/*si cambiamos algun parametro de configuracion en el editor visualCode y si la aplicacion esta 
 * levantada y no queremos reniciarla, podemos evitar el reniciar mediante la implemnetacion de Actuator Refresh*/
@RefreshScope
@Configuration
/*habilitar la configuracion del servidor de recurso*/
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter{
	
	/*como solo es un valor no es conveniente inyectar Environment, solo lo obtenemos con Value*/
	@Value("${config.security.oauth.jwt.key}")
	private String jwtKey;

	/*se tiene que implementar dos metodos: uno para proteger las rutas y otro para configurar el token,
	 * con la misma estructura del servidor de autorizacion*/
	
	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		resources.tokenStore(tokenStore());
	}

	/*proteger cada ruta de ZuulServer*/
	@Override
	public void configure(HttpSecurity http) throws Exception {
		/* /**, esto significa que va aplicar el permitAll, a cualquier ruta que venga despues de oauth;
		 * tambien se aplica para cualquier tipo de peticion, post, get, ...*/
		http.authorizeRequests().antMatchers("/api/security/oauth/token").permitAll()
		.antMatchers(HttpMethod.GET, "/api/productos/listar", "/api/items/listar", "/api/usuarios/usuarios").permitAll()
		.antMatchers(HttpMethod.GET, "/api/productos/ver/{id}", "/api/items/ver/{id}/cantidad/{cantidad}",
				"/api/usuarios/usuarios/{id}").hasAnyRole("ADMIN","USER")
		/*.antMatchers(HttpMethod.POST, "/api/productos/crear", "/api/items/crear","/api/usuarios/usuarios").hasRole("ADMIN")
		.antMatchers(HttpMethod.PUT, "/api/productos/editar/{id}", "/api/items/editar/{id}", "/api/usuarios/usuarios/{id}").hasRole("ADMIN")
		.antMatchers(HttpMethod.DELETE, "/api/productos/eliminar/{id}", "/api/items/eliminar/{id}", "/api/usuarios/usuarios/{id}").hasRole("ADMIN")*/
		.antMatchers("/api/productos/**", "/api/items/**", "/api/usuarios/**").hasRole("ADMIN")
		//cualquier ruta que no se haya especificado en las reglas de arriba, va requerir autenticacion
		.anyRequest().authenticated()
		//la configuracion de SpringSecurity(arriba) se configura en el corsConfigurationSource()
		.and().cors().configurationSource(corsConfigurationSource());
	}

	
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

		CorsConfiguration corsConfig = new CorsConfiguration();
		/*aca se establece el origen de la aplicacion cliente mediante la ruta url, ejm: en angular
		 * seria http://localhost:4200, pero tambien se puede dejar esto mas generico con *, permite
		 * agregar de forma automatica cualquier origen; addAllowedOrigin un solo origin, setAllowedOrigins
		 * permite varios destinos*/
		corsConfig.setAllowedOrigins(Arrays.asList("*"));
		corsConfig.setAllowedMethods(Arrays.asList("POST","GET","PUT","DELETE","OPTIONS"));
		//agrillamos, porque por debajo la utiliza oauth2
		corsConfig.setAllowCredentials(true);
		//Authorization, cuando se envie el token en la cabecera para acceder a los recursos y cuando nos autenticamos
		corsConfig.setAllowedHeaders(Arrays.asList("Authorization","Content-Type"));
		
		/*pasar la condifuracion de corsConfig a la ruta url*/
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		/* /**, se aplique a todas las rutas */
		source.registerCorsConfiguration("/**", corsConfig);
		
		return source;
	}
	
	/*Permitira registrar un filtro de cors para que no solo sea el configurado en SpringSecurity
	 * sino tambien de forma global, en toda la aplicacion*/
	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter(){
		FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<CorsFilter>(new CorsFilter(corsConfigurationSource()));
		//se le da una prioridad alta
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		//encargado de registrar el CorsFilter
		return bean;
	}

	@Bean
	public JwtTokenStore tokenStore() {
		
		return new JwtTokenStore(accessTokenConverter());
	}

	/*el token tiene que ser identico que en el servidor donde se crea y se firma el token(servicio-oauth),
	 * con las misma llave, porque aca se valida que el token sea el correcto por ello se necesita la misma llave*/
	@Bean
	public JwtAccessTokenConverter accessTokenConverter() {
		JwtAccessTokenConverter tokenConverter = new JwtAccessTokenConverter();
		/*codigo secreto para validar la firma*/
		tokenConverter.setSigningKey(jwtKey);
		
		return tokenConverter;
	}
	
	
	
}
