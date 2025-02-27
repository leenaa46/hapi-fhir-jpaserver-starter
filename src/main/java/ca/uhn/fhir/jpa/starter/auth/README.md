# Authentication System

## Overview

This server uses a dual authentication approach:

1. **OAuth Authentication** (port 8080):
   - Requests go through oauth2-proxy
   - Users authenticate via Auth0
   - Protected by session cookies

2. **API Key Authentication** (port 9090):
   - Direct access for ETL services and automated systems
   - Requires `X-API-KEY` header with the configured API key
   - No OAuth flow required

## How to Access the API

### For UI Access (Humans)
Use port 8080, which will redirect to Auth0 for login.

### For Programmatic Access (Systems)
Use port 9090 with the API key:

```bash
curl -H "X-API-KEY: your_api_key" http://server:9090/fhir/metadata
```

## How It Works

The `ApiKeyAuthInterceptor` class handles API key validation for all incoming requests.
It checks for the presence of a valid API key in the `X-API-KEY` header for requests
not coming through the oauth2-proxy.
