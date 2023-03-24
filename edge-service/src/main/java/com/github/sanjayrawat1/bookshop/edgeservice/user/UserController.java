package com.github.sanjayrawat1.bookshop.edgeservice.user;

import java.util.List;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author Sanjay Singh Rawat
 */
@RestController
public class UserController {

    @GetMapping("/user")
    public Mono<User> getUser() {
        return ReactiveSecurityContextHolder
            // gets SecurityContext from the currently authenticated user from ReactiveSecurityContextHolder
            .getContext()
            // gets Authentication from SecurityContext
            .map(SecurityContext::getAuthentication)
            // gets the Principal from Authentication.
            // for OIDC, it's of type OidcUser
            .map(authentication -> (OidcUser) authentication.getPrincipal())
            .map(oidcUser -> new User(oidcUser.getPreferredUsername(), oidcUser.getGivenName(), oidcUser.getFamilyName(), List.of("employee", "customer")));
    }
}
