package ca.uhn.fhir.jpa.starter.auth;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Interceptor
public class ApiKeyAuthInterceptor {

	@Value("${HAPI_FHIR_API_KEY:}")
	private String apiKey;

	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
	public void authenticate(RequestDetails requestDetails) {
		// Skip authentication if no API key is configured
		if (apiKey == null || apiKey.isEmpty()) {
			return;
		}

		// Skip authentication for OAuth protected endpoints (coming through the proxy)
		String requestPath = requestDetails.getRequestPath();
		if (!requestPath.startsWith("/fhir/")) {
			return;
		}

		// Skip authentication for non-API routes
		String remoteAddress = requestDetails.getHeader("X-Forwarded-For");
		if (remoteAddress != null) {
			// If this header is set, the request is coming from the OAuth proxy
			return;
		}

		// Check for API key in headers
		String apiKeyHeader = requestDetails.getHeader("X-API-KEY");
		if (apiKeyHeader == null || !apiKeyHeader.equals(apiKey)) {
			throw new AuthenticationException("Invalid or missing API key");
		}
	}
}
