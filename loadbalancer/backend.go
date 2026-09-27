package main

import (
	"net/http"
	"net/http/httputil"
	"net/url"
	"sync"
	"sync/atomic"
	"time"
)

type Backend struct {
	URL          *url.URL
	Alive        bool
	mux          sync.RWMutex
	ReverseProxy *httputil.ReverseProxy

	// Metrics
	ActiveConns   int64     `json:"active_connections"`
	TotalRequests int64     `json:"total_requests"`
	TotalErrors   int64     `json:"total_errors"`
	LastLatencyMs int64     `json:"last_latency_ms"`
	LastChecked   time.Time `json:"last_checked"`
}

func NewBackend(rawURL string) (*Backend, error) {
	parsedURL, err := url.Parse(rawURL)
	if err != nil {
		return nil, err
	}

	proxy := httputil.NewSingleHostReverseProxy(parsedURL)

	// Custom transport for handling long-lived connections, large multipart streams, and keep-alives
	proxy.Transport = &http.Transport{
		MaxIdleConns:        100,
		MaxIdleConnsPerHost: 30,
		IdleConnTimeout:     90 * time.Second,
		DisableCompression:  false,
	}

	originalDirector := proxy.Director
	proxy.Director = func(req *http.Request) {
		originalDirector(req)
		req.Host = parsedURL.Host
	}

	b := &Backend{
		URL:          parsedURL,
		Alive:        true,
		ReverseProxy: proxy,
		LastChecked:  time.Now(),
	}

	return b, nil
}

func (b *Backend) SetAlive(alive bool) {
	b.mux.Lock()
	b.Alive = alive
	b.LastChecked = time.Now()
	b.mux.Unlock()
}

func (b *Backend) IsAlive() bool {
	b.mux.RLock()
	defer b.mux.RUnlock()
	return b.Alive
}

func (b *Backend) IncConns() {
	atomic.AddInt64(&b.ActiveConns, 1)
	atomic.AddInt64(&b.TotalRequests, 1)
}

func (b *Backend) DecConns() {
	atomic.AddInt64(&b.ActiveConns, -1)
}

func (b *Backend) IncErrors() {
	atomic.AddInt64(&b.TotalErrors, 1)
}

func (b *Backend) GetActiveConns() int64 {
	return atomic.LoadInt64(&b.ActiveConns)
}

func (b *Backend) GetTotalRequests() int64 {
	return atomic.LoadInt64(&b.TotalRequests)
}

func (b *Backend) GetTotalErrors() int64 {
	return atomic.LoadInt64(&b.TotalErrors)
}

func (b *Backend) SetLatency(d time.Duration) {
	atomic.StoreInt64(&b.LastLatencyMs, d.Milliseconds())
}

func (b *Backend) GetLatency() int64 {
	return atomic.LoadInt64(&b.LastLatencyMs)
}
