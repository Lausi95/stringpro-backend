package com.stringpro.infrastructure.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Hello")
class HelloController {

    @GetMapping("/hello")
    @Operation(summary = "Hello world endpoint")
    @ApiResponse(responseCode = "200", description = "OK")
    fun hello(): Map<String, String> = mapOf("message" to "Hello, World!")
}
