package com.avijit.appointmentmanagementsystem.Controllers;

import com.avijit.appointmentmanagementsystem.Config.Security.JwtAuthentication;
import com.avijit.appointmentmanagementsystem.DTO.*;
import com.avijit.appointmentmanagementsystem.Exception.NotExist;
import com.avijit.appointmentmanagementsystem.Services.UserServiceInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class UserController {
    private final UserServiceInterface userServiceInterface;

    public UserController(UserServiceInterface userServiceInterface) {
        this.userServiceInterface = userServiceInterface;
    }

    // User registration ************************************************************************************************
    @PostMapping("/user/register")
    public ResponseEntity<String> userRegister(@RequestBody UserRegisterRequestDto userRequestDto, HttpServletResponse httpServletResponse) throws IOException, NotExist {
        if (userRequestDto.getEmail().isEmpty() || userRequestDto.getName() == null || userRequestDto.getPassword() == null) {
            return new ResponseEntity<>("Please fill all the fields", HttpStatus.BAD_REQUEST);
        }
        userServiceInterface.userRegister(userRequestDto, httpServletResponse);

        return new ResponseEntity<>("User created successfully!!", HttpStatus.CREATED);
    }

    // User login ******************************************************************************************************
    @PostMapping("/user/login")
    public ResponseEntity<String> userLogin(@Validated @RequestBody LogInRequestDto logInRequestDto, HttpServletResponse httpServletResponse) throws NotExist, IOException {
        if (userServiceInterface.userLogin(logInRequestDto)) {
            String tokenGen = JwtAuthentication.generateToken(logInRequestDto.getUsername());
            String jwtToken = "Bearer " + tokenGen;
            String encodedValue = URLEncoder.encode(jwtToken, StandardCharsets.UTF_8);
            Cookie cookie = new Cookie("authorization", encodedValue);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setMaxAge(3600);
            cookie.setPath("/");
            System.out.println("Cookie: " + cookie.getName());
            httpServletResponse.addCookie(cookie);
            return ResponseEntity.ok("User has logged in");
        } else {
            return new ResponseEntity<>("User not found,please register yourself", HttpStatus.NOT_FOUND);
        }
    }

    //    User validate token **********************************************************************************************
    @GetMapping("/user/validate")
    public ResponseEntity<String> validateToken(@CookieValue(name = "authorization", defaultValue = "") String token) {
        LoginTokenDto loginTokenDto = new LoginTokenDto();
        loginTokenDto.setToken(token);
        if (!loginTokenDto.getToken().isEmpty()) {
            if (userServiceInterface.userTokenLogin(loginTokenDto)) {
                return ResponseEntity.ok("Welcome back");
            }
        }
        return new ResponseEntity<>("You are not authorized, please login again", HttpStatus.UNAUTHORIZED);
    }

    //    User logout *****************************************************************************************************
    @PostMapping("/user/logout")
    public ResponseEntity<String> userLogout(HttpServletResponse httpServletResponse) {
        Cookie cookie = new Cookie("Authorization", "");
        cookie.setMaxAge(0);
        httpServletResponse.addCookie(cookie);
        return new ResponseEntity<>("User has logged out", HttpStatus.OK);
    }

    // User profile *****************************************************************************************************
    @GetMapping("/user/profile")
    public ResponseEntity<UserResisterResponseDto> getProfile(@CookieValue(name = "Authorization") String token) {
        if (token != null) {
            if (token.startsWith("Bearer")) {
                token = token.substring(7);
                if (JwtAuthentication.validateToken(token)) {
                    String email = JwtAuthentication.extractSubject(token);
                    UserMailRequestDto userMailRequestDto = new UserMailRequestDto();
                    userMailRequestDto.setEmail(email);
                    UserResisterResponseDto responseDto = userServiceInterface.getProfile(userMailRequestDto);
                    return new ResponseEntity<>(responseDto, HttpStatus.OK);
                }
            }
        }
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    // Edit user profile
    @PutMapping("/user/profile/{id}")
    public UserResisterResponseDto editUser(HttpServletResponse httpServletResponse) {
        UserResisterResponseDto entity = new UserResisterResponseDto();
        String token = httpServletResponse.getHeader("Authorization");
        return entity;
    }
}
