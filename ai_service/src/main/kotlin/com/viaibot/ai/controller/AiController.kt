package com.viaibot.ai.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class AiController {

    @GetMapping("/hello")
    fun GetHelloWorld(): ResponseEntity<String> {
        return ResponseEntity.ok("Hello World!")
    }
}