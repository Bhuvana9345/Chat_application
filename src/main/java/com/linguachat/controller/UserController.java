package com.linguachat.controller;

import com.linguachat.dto.UserResponse;
import com.linguachat.service.UserService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(Principal principal) {
        return userService.toResponse(userService.getByUsername(principal.getName()));
    }

    @GetMapping
    public List<UserResponse> users(Principal principal, @RequestParam(required = false) String search) {
        return userService.listUsers(principal.getName(), search);
    }

    @DeleteMapping("/me")
    public void deleteMe(Principal principal) {
        userService.deleteAccount(principal.getName());
    }
}
