package ca.uhn.fhir.jpa.starter.auth;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Interceptor
public class ApiKeyAuthInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthInterceptor.class);

	@Value("${HAPI_FHIR_API_KEY:}")
	private String apiKey;

	/**
	 * Intercepts incoming requests before processing
	 */
	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
	public void authenticate(RequestDetails requestDetails) {
		// Skip authentication if no API key is configured
		if (apiKey == null || apiKey.isEmpty()) {
			return;
		}

		// Safety check for null requestDetails
		if (requestDetails == null) {
			return;
		}

		// Check if request is coming through OAuth proxy
		String forwardedFor = requestDetails.getHeader("X-Forwarded-For");
		String proxyAuth = requestDetails.getHeader("X-Auth-Request-User");

		// If coming through OAuth proxy with auth headers, allow
		if (forwardedFor != null && proxyAuth != null) {
			return;
		}

		// Direct access requires API key
		String apiKeyHeader = requestDetails.getHeader("X-API-KEY");
		if (apiKeyHeader == null || !apiKeyHeader.equals(apiKey)) {
			throw new AuthenticationException("Invalid or missing API key");
		}
	}

	/**
	 * Intercepts incoming requests at the HTTP level
	 * This catches requests that might not reach the RequestDetails stage
	 */
	@Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
	public void authenticateHttpLevel(HttpServletRequest theRequest, HttpServletResponse theResponse) {
		// Skip authentication if no API key is configured
		if (apiKey == null || apiKey.isEmpty()) {
			return;
		}

		// Skip for web UI resources like CSS, JS, images
		String requestURI = theRequest.getRequestURI();
		if (requestURI != null && (requestURI.contains("/css/") ||
				requestURI.contains("/js/") ||
				requestURI.contains("/img/") ||
				requestURI.endsWith(".ico"))) {
			return;
		}

		// Check if request is coming through OAuth proxy
		String forwardedFor = theRequest.getHeader("X-Forwarded-For");
		String proxyAuth = theRequest.getHeader("X-Auth-Request-User");

		// If coming through OAuth proxy with auth headers, allow
		if (forwardedFor != null && proxyAuth != null) {
			return;
		}

		// Direct access requires API key
		String apiKeyHeader = theRequest.getHeader("X-API-KEY");
		if (apiKeyHeader == null || !apiKeyHeader.equals(apiKey)) {
			// We can't throw an exception here as it won't be properly caught
			// But we've already authenticated in the previous hook for FHIR operations
			// This is just a safety net for requests that bypass the normal FHIR processing
			logger.debug("Request without valid API key to: {}", requestURI);
		}
	}
}
