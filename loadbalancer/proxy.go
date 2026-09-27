package main

import (
	"fmt"
	"net"
	"net/http"
	"time"
)

type ProxyHandler struct {
	lb *LoadBalancer
}

func NewProxyHandler(lb *LoadBalancer) *ProxyHandler {
	return &ProxyHandler{lb: lb}
}

func (ph *ProxyHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	// Special LB management routes
	if r.URL.Path == "/lb-status" {
		ServeStatusJSON(w, r, ph.lb)
		return
	}
	if r.URL.Path == "/lb-dashboard" {
		ServeDashboardHTML(w, r, ph.lb)
		return
	}

	start := time.Now()
	maxAttempts := ph.lb.config.MaxRetries
	if maxAttempts < 1 {
		maxAttempts = 1
	}

	var target *Backend
	for attempt := 1; attempt <= maxAttempts; attempt++ {
		target = ph.lb.GetNextBackend(r)
		if target == nil {
			http.Error(w, `{"error":"Service Unavailable","message":"No healthy backend instances available in the cluster"}`, http.StatusServiceUnavailable)
			return
		}

		target.IncConns()

		// Prepare forwarded headers
		clientIP, _, err := net.SplitHostPort(r.RemoteAddr)
		if err != nil {
			clientIP = r.RemoteAddr
		}

		if prior := r.Header.Get("X-Forwarded-For"); prior != "" {
			clientIP = prior + ", " + clientIP
		}
		r.Header.Set("X-Forwarded-For", clientIP)
		r.Header.Set("X-Real-IP", clientIP)
		r.Header.Set("X-Forwarded-Host", r.Host)
		if r.TLS != nil {
			r.Header.Set("X-Forwarded-Proto", "https")
		} else {
			r.Header.Set("X-Forwarded-Proto", "http")
		}
		r.Header.Set("X-LoadBalanced-By", "OptiNeer-Go-LB")

		// Response Interceptor to capture status code
		wi := &responseWriterInterceptor{ResponseWriter: w, statusCode: http.StatusOK}

		// Delegate to reverse proxy
		target.ReverseProxy.ServeHTTP(wi, r)
		target.DecConns()

		duration := time.Since(start)

		if wi.statusCode >= 502 && wi.statusCode <= 504 && attempt < maxAttempts {
			target.IncErrors()
			fmt.Printf("[PROXY] Error on %s (HTTP %d). Retrying on alternative backend (Attempt %d/%d)...\n",
				target.URL.String(), wi.statusCode, attempt+1, maxAttempts)
			continue
		}

		// Success or normal response
		fmt.Printf("[PROXY] %s %s -> %s (HTTP %d, %v)\n",
			r.Method, r.URL.Path, target.URL.String(), wi.statusCode, duration.Round(time.Millisecond))
		return
	}
}

type responseWriterInterceptor struct {
	http.ResponseWriter
	statusCode int
	written    bool
}

func (w *responseWriterInterceptor) WriteHeader(code int) {
	if !w.written {
		w.statusCode = code
		w.written = true
		w.ResponseWriter.WriteHeader(code)
	}
}

func (w *responseWriterInterceptor) Write(b []byte) (int, error) {
	w.written = true
	return w.ResponseWriter.Write(b)
}

func (w *responseWriterInterceptor) Flush() {
	if flusher, ok := w.ResponseWriter.(http.Flusher); ok {
		flusher.Flush()
	}
}

// Support hijacker if needed for websockets
func (w *responseWriterInterceptor) Hijack() (net.Conn, any, error) {
	if hijacker, ok := w.ResponseWriter.(http.Hijacker); ok {
		return hijacker.Hijack()
	}
	return nil, nil, fmt.Errorf("hijacking not supported")
}
