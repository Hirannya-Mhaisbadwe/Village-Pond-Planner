package main

import (
	"context"
	"fmt"
	"net/http"
	"time"
)

type HealthChecker struct {
	lb     *LoadBalancer
	client *http.Client
}

func NewHealthChecker(lb *LoadBalancer, timeout time.Duration) *HealthChecker {
	return &HealthChecker{
		lb: lb,
		client: &http.Client{
			Timeout: timeout,
		},
	}
}

func (hc *HealthChecker) Start(ctx context.Context, interval time.Duration) {
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	// Initial check on start
	hc.checkAll()

	for {
		select {
		case <-ctx.Done():
			fmt.Println("[HEALTH] Stopped health checker.")
			return
		case <-ticker.C:
			hc.checkAll()
		}
	}
}

func (hc *HealthChecker) checkAll() {
	for _, b := range hc.lb.GetBackends() {
		go hc.checkBackend(b)
	}
}

func (hc *HealthChecker) checkBackend(b *Backend) {
	healthURL := fmt.Sprintf("%s%s", b.URL.String(), hc.lb.config.HealthCheckPath)
	start := time.Now()

	req, err := http.NewRequest(http.MethodGet, healthURL, nil)
	if err != nil {
		hc.updateStatus(b, false, time.Since(start), err.Error())
		return
	}
	req.Header.Set("User-Agent", "Pond-LoadBalancer-HealthCheck/1.0")

	resp, err := hc.client.Do(req)
	latency := time.Since(start)

	if err != nil {
		hc.updateStatus(b, false, latency, err.Error())
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 200 && resp.StatusCode < 400 {
		hc.updateStatus(b, true, latency, fmt.Sprintf("HTTP %d", resp.StatusCode))
	} else {
		hc.updateStatus(b, false, latency, fmt.Sprintf("HTTP %d", resp.StatusCode))
	}
}

func (hc *HealthChecker) updateStatus(b *Backend, alive bool, latency time.Duration, info string) {
	wasAlive := b.IsAlive()
	b.SetAlive(alive)
	b.SetLatency(latency)

	if wasAlive != alive {
		status := "HEALTHY (ONLINE)"
		if !alive {
			status = "UNHEALTHY (OFFLINE)"
		}
		fmt.Printf("[HEALTH] Node %s changed status -> %s | Latency: %v | Info: %s\n",
			b.URL.String(), status, latency.Round(time.Millisecond), info)
	}
}
