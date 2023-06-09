package chung.me.livechatapi.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

private const val BEARER_PREFIX = "Bearer "
const val USER_ID = "USER_ID"

@Component
class JwtAuthenticationFilter(
  private val jwtService: JwtService,
  private val userDetailsService: UserDetailsService,
) : OncePerRequestFilter() {

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      filterChain.doFilter(request, response)
      return
    }

    val jwt = authHeader.substringAfter(BEARER_PREFIX)
    val userId = jwtService.extractUserId(jwt)

    val wrapper = object : HttpServletRequestWrapper(request) {
      override fun getHeaders(name: String?): Enumeration<String> {
        return if (name == USER_ID) {
          Collections.enumeration(listOf(userId))
        } else {
          super.getHeaders(name)
        }
      }
    }

    if (SecurityContextHolder.getContext().authentication == null) {
      val userDetails = userDetailsService.loadUserByUsername(userId)
      if (jwtService.isTokenValid(jwt, userDetails)) {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
          userDetails,
          null,
          userDetails.authorities
        ).apply {
          this.details = WebAuthenticationDetailsSource().buildDetails(request)
        }
      }
    }

    filterChain.doFilter(wrapper, response)
  }
}
