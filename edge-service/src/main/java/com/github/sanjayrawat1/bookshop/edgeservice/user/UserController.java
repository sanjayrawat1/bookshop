package com.github.sanjayrawat1.bookshop.edgeservice.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author Sanjay Singh Rawat
 */
@Slf4j
@RestController
public class UserController {

    @GetMapping("/user")
    public Mono<User> getUser(@AuthenticationPrincipal OidcUser oidcUser) {
        log.info("Fetching information about the currently authenticated user");
        var user = new User(oidcUser.getPreferredUsername(), oidcUser.getGivenName(), oidcUser.getFamilyName(), oidcUser.getClaimAsStringList("roles"));
        return Mono.just(user);
    }
}
