/**
 * API Interceptor to handle 401 Unauthorized errors globally.
 * It automatically attempts to refresh the JWT token using the refresh token cookie.
 */

const originalFetch = window.fetch;
let isRefreshing = false;
let refreshPromise = null;

window.fetch = async (...args) => {
    let response = await originalFetch(...args);

    // If unauthorized, attempt to refresh token
    if (response.status === 401) {
        const url = typeof args[0] === 'string' ? args[0] : args[0].url;
        
        // Avoid infinite loop if the refresh request itself returns 401
        // Also avoid redirecting on login/register routes where 401 is a normal "invalid credentials" response
        if (
            url.includes('/api/auth/refresh') || 
            url.includes('/api/auth/login') || 
            url.includes('/api/auth/register')
        ) {
            return response;
        }

        try {
            // Use a mutex-like pattern to avoid concurrent refresh calls
            if (!isRefreshing) {
                isRefreshing = true;
                
                // Use relative path for proxy
                const apiBase = '/api';

                refreshPromise = originalFetch(`${apiBase}/auth/refresh`, {
                    method: 'POST',
                    credentials: 'include'
                }).finally(() => {
                    isRefreshing = false;
                });
            }

            const refreshResponse = await refreshPromise;

            if (refreshResponse.ok) {
                // Retry the original request
                return await originalFetch(...args);
            } else {
                // Refresh failed (expired or invalid), logout user
                console.warn('Session expired. Redirecting to login...');
                window.location.href = '/logout';
            }
        } catch (error) {
            console.error('Error during token refresh:', error);
            window.location.href = '/logout';
        }
    }

    return response;
};
