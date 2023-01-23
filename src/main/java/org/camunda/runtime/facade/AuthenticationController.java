package org.camunda.runtime.facade;

import javax.servlet.http.HttpSession;
import org.camunda.runtime.exception.UnauthorizedException;
import org.camunda.runtime.facade.dto.AuthUser;
import org.camunda.runtime.facade.dto.Authentication;
import org.camunda.runtime.jsonmodel.User;
import org.camunda.runtime.security.SecurityUtils;
import org.camunda.runtime.security.annotation.IsAuthenticated;
import org.camunda.runtime.service.KeycloakService;
import org.camunda.runtime.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController extends AbstractController {

  private final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

  private OrganizationService organizationService;
  private KeycloakService keycloakService;

  public AuthenticationController(
      OrganizationService organizationService, KeycloakService keycloakService) {
    this.keycloakService = keycloakService;
    this.organizationService = organizationService;
  }

  @RequestMapping(value = "/login", method = RequestMethod.POST, produces = "application/json")
  @ResponseStatus(HttpStatus.OK)
  public AuthUser login(@RequestBody Authentication auth) {
    User user =
        organizationService.getUserByUsernameAndPassword(auth.getUsername(), auth.getPassword());
    if (user == null) {
      throw new UnauthorizedException("Credentials not recognized");
    }
    return getAuthUser(user);
  }

  @IsAuthenticated
  @GetMapping("/user")
  public AuthUser getUser() {
    if (isKeycloakAuth()) {
      return keycloakService.getUser(getRequest());
    }
    User user = organizationService.getUserByUsername(getAuthenticatedUsername());
    return getAuthUser(user);
  }

  @GetMapping("/logout")
  public void logout() {
    if (isKeycloakAuth()) {
      keycloakService.logout(getRequest());
    } else {
      HttpSession session = getRequest().getSession(false);
      if (session != null) {
        session.invalidate();
      }
    }
  }

  private AuthUser getAuthUser(User user) {
    AuthUser authUser = new AuthUser();
    BeanUtils.copyProperties(user, authUser);

    authUser.setToken(SecurityUtils.getJWTToken(user));
    return authUser;
  }

  @Override
  public Logger getLogger() {
    return logger;
  }
}
