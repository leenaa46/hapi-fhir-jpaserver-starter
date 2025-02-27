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

	@Value("${API_KEY:secure_api_key_for_etl_service}")
	private String apiKey;

	/**
	 * Intercepts incoming requests before processing
	 */
	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
	public void authenticate(RequestDetails requestDetails, HttpServletRequest request) {
		// Safety check for null values
		if (requestDetails == null || apiKey == null || apiKey.isEmpty() || request == null) {
			return;
		}

		// Check if this is a direct API access on port 9090
		int serverPort = request.getServerPort();
		boolean isDirectApiAccess = serverPort == 9090;

		// For direct API access on port 9090, API key is always required
		if (isDirectApiAccess) {
			String apiKeyHeader = requestDetails.getHeader("X-API-KEY");
			if (apiKeyHeader == null || !apiKeyHeader.equals(apiKey)) {
				logger.warn("Unauthorized access attempt on port 9090 from IP: {}", request.getRemoteAddr());
				throw new AuthenticationException("Access denied. Valid API key required.");
			}
			return;
		}

		// For port 8080 (oauth flow), check if request is coming through OAuth proxy
		String forwardedFor = requestDetails.getHeader("X-Forwarded-For");
		String proxyAuth = requestDetails.getHeader("X-Auth-Request-User");

		// If coming through OAuth proxy with auth headers, allow
		if (forwardedFor != null && proxyAuth != null) {
			return;
		}

		// Fall back to API key for non-OAuth flows
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
		if (apiKey == null || apiKey.isEmpty() || theRequest == null) {
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

		// Check if this is a direct API access on port 9090
		int serverPort = theRequest.getServerPort();
		boolean isDirectApiAccess = serverPort == 9090;

		// For direct API access on port 9090, API key is always required
		if (isDirectApiAccess) {
			String apiKeyHeader = theRequest.getHeader("X-API-KEY");
			if (apiKeyHeader == null || !apiKeyHeader.equals(apiKey)) {
				// Set unauthorized status
				theResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				try {
					theResponse.getWriter().write("Access denied. Valid API key required.");
					theResponse.getWriter().flush();
				} catch (Exception e) {
					logger.error("Error writing unauthorized response", e);
				}
			}
			return;
		}

		// For port 8080 (oauth flow), check if request is coming through OAuth proxy
		String forwardedFor = theRequest.getHeader("X-Forwarded-For");
		String proxyAuth = theRequest.getHeader("X-Auth-Request-User");

		// If coming through OAuth proxy with auth headers, allow
		if (forwardedFor != null && proxyAuth != null) {
			return;
		}

		// Fall back to API key check for other requests
		String apiKeyHeader = theRequest.getHeader("X-API-KEY");
		if (apiKeyHeader == null || !apiKeyHeader.equals(apiKey)) {
			// We can't throw an exception here, but we can set HTTP status
			theResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			try {
				theResponse.getWriter().write("Invalid or missing API key");
				theResponse.getWriter().flush();
			} catch (Exception e) {
				logger.error("Error writing unauthorized response", e);
			}
		}
	}
}
