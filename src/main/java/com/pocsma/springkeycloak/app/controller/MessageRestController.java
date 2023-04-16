package com.pocsma.springkeycloak.app.controller;

import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageRestController {
	
	@Autowired
    private AccessToken accessToken;
	
	@GetMapping("/getMessage")
	public ResponseEntity<String> getMessageSecurizedByKeycloak(
			@RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
		
		return new ResponseEntity<>	("It works. User logged: " + accessToken.getPreferredUsername(), HttpStatus.OK);	
	}	
}
