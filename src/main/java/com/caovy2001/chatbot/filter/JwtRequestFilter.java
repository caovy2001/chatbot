package com.caovy2001.chatbot.filter;

import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.repository.UserRepository;
import com.caovy2001.chatbot.service.user.enumeration.UserRole;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    @Autowired
    private UserRepository userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        UsernamePasswordAuthenticationToken authentication = null;

        if (request.getHeader("Authorization") != null) {
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader.startsWith("Token ")) {
                String token = authorizationHeader.substring(6);
                UserEntity userEntityToken = userRepo.findByToken(token).orElse(null);

                if (userEntityToken != null) {
                    if (userEntityToken.getRole() == UserRole.USER) {
                        Collection<GrantedAuthority> authorities = new ArrayList<>();
                        authorities.add(new SimpleGrantedAuthority("ALLOW_ACCESS"));
                        authentication = new UsernamePasswordAuthenticationToken(userEntityToken, null,
                                authorities);
                    } else if (userEntityToken.getRole() == UserRole.ADMIN) {
                        String userIdHeader = request.getHeader("userId");
                        if (StringUtils.isNotBlank(userIdHeader)) {
                            UserEntity userEntityHeader = userRepo.findById(userIdHeader).orElse(null);
                            if (userEntityHeader != null) {
                                if (userEntityHeader.getRole() == UserRole.USER) {
                                    Collection<GrantedAuthority> authorities = new ArrayList<>();
                                    authorities.add(new SimpleGrantedAuthority("ALLOW_ACCESS"));
                                    authentication = new UsernamePasswordAuthenticationToken(userEntityHeader, null,
                                            authorities);
                                } else if (userEntityHeader.getRole() == UserRole.ADMIN) {
                                    Collection<GrantedAuthority> authorities = new ArrayList<>();
                                    authorities.add(new SimpleGrantedAuthority("ADMIN"));
                                    authentication = new UsernamePasswordAuthenticationToken(null, null,
                                            authorities);
                                }
                            }
                        }
                    }

                }
            }
        }

        if (authentication != null) {
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        else
            SecurityContextHolder.getContext().setAuthentication(null);

        filterChain.doFilter(request, response);

    }
}
