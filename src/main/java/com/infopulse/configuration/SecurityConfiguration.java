package com.infopulse.configuration;

import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakPreAuthActionsFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends KeycloakWebSecurityConfigurerAdapter {

    /**
     * Configure authentication manager.
     * @param auth auth manager
     * @throws Exception exception is throw
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(keycloakAuthenticationProvider());
    }

    /**
     * Sets authentication strategy.
     * Null authentication sessions strategy is used.
     * @return authentication strategy
     */
    @Bean
    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new NullAuthenticatedSessionStrategy();
    }

    /**
     * Sets filter registration bean.
     * Bean is disabled by default.
     * @param filter filter
     * @return registration bean
     */
    @Bean
    public FilterRegistrationBean keycloakAuthenticationProcessingFilterRegistrationBean(
            KeycloakAuthenticationProcessingFilter filter) {
        final FilterRegistrationBean registrationBean = new FilterRegistrationBean(filter);
        registrationBean.setEnabled(false);
        return registrationBean;
    }

    /**
     * Gets keycloak authentication provider.
     * @return provider
     */
    @Override
    protected KeycloakAuthenticationProvider keycloakAuthenticationProvider() {
        final SimpleAuthorityMapper mapper = new SimpleAuthorityMapper();
        mapper.setPrefix("ROLE_");
        mapper.setConvertToUpperCase(true);

        final KeycloakAuthenticationProvider provider = super.keycloakAuthenticationProvider();
        provider.setGrantedAuthoritiesMapper(mapper);

        return provider;
    }

    /**
     * Exception translation filter to handle <code>AuthenticationException</code> thrown within the filter chain.
     * @return Exception translation filter, which uses default Keycloak <code>authenticationEntryPoint</code>
     * @throws Exception exception is throw
     */
    public ExceptionTranslationFilter exceptionTranslationFilter() throws Exception {
        // TODO: default message is returned in response (e.g., "Unauthorized" for 401)
        // - extend KeycloakAuthenticationEntryPoint to return more meaningful error message
        return new ExceptionTranslationFilter(authenticationEntryPoint());
    }

    /**
     * Allows url encoded slash http firewall.
     * @return firewall
     */
    @Bean
    public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
        final StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowBackSlash(true);
        firewall.setAllowUrlEncodedPercent(true);
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowSemicolon(true);
        return firewall;
    }

    /**
     * Configure http security.
     * @param http http
     * @throws Exception exception is throw
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                //TODO: remove this line before going live!
                .csrf().disable()
                .cors().disable()
                //.csrf().requireCsrfProtectionMatcher(keycloakCsrfRequestMatcher())
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .sessionAuthenticationStrategy(sessionAuthenticationStrategy())
                .and()
                .addFilterBefore(keycloakPreAuthActionsFilter(), LogoutFilter.class)
                .addFilterBefore(keycloakAuthenticationProcessingFilter(), X509AuthenticationFilter.class)
                .addFilterBefore(exceptionTranslationFilter(), KeycloakPreAuthActionsFilter.class)
                .authorizeRequests()

                .antMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                .antMatchers(HttpMethod.GET,"/users").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/ban").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/ban").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/registration").permitAll()
                .antMatchers("/socket/**").permitAll()
                .anyRequest().permitAll()
                .and()
                // logout settings - copied from KeycloakWebSecurityConfigurerAdapter
                .logout()
                .addLogoutHandler(keycloakLogoutHandler())
                .logoutUrl("/sso/logout").permitAll()
                .logoutSuccessUrl("/");

    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
