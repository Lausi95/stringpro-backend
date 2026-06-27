package com.stringpro.infrastructure.web

import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import com.stringpro.infrastructure.config.SecurityConfig

@WebMvcTest(HelloController::class)
@Import(SecurityConfig::class)
class HelloControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `should return hello message when authenticated`() {
        mvc.get("/hello") {
            with(jwt())
        }.andExpect {
            status { isOk() }
            jsonPath("$.message") { value("Hello, World!") }
        }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mvc.get("/hello")
            .andExpect {
                status { isUnauthorized() }
            }
    }
}
