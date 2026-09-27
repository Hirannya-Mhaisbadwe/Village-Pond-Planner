package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func main() {
	cfg, err := LoadConfig()
	if err != nil {
		log.Fatalf("[FATAL] Failed to load configuration: %v", err)
	}

	fmt.Println("==================================================================")
	fmt.Println("       ⚡ OptiNeer Custom Load Balancer (Go Engine) ⚡            ")
	fmt.Println("==================================================================")
	fmt.Printf("[CONFIG] Listening Port    : %d (maps to external port 3221)\n", cfg.Port)
	fmt.Printf("[CONFIG] Balancing Algo    : %s\n", cfg.Algorithm)
	fmt.Printf("[CONFIG] Health Interval   : %v\n", cfg.HealthInterval)
	fmt.Printf("[CONFIG] Health Endpoint   : %s\n", cfg.HealthCheckPath)
	fmt.Printf("[CONFIG] Max Proxy Retries : %d\n", cfg.MaxRetries)
	fmt.Println("[CONFIG] Registered Backends:")
	for i, b := range cfg.Backends {
		fmt.Printf("   [%d] %s\n", i+1, b)
	}
	fmt.Println("------------------------------------------------------------------")

	lb, err := NewLoadBalancer(cfg)
	if err != nil {
		log.Fatalf("[FATAL] Failed to initialize load balancer: %v", err)
	}

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Start background health checking
	healthChecker := NewHealthChecker(lb, cfg.HealthTimeout)
	go healthChecker.Start(ctx, cfg.HealthInterval)

	// Create reverse proxy handler
	proxyHandler := NewProxyHandler(lb)

	server := &http.Server{
		Addr:           fmt.Sprintf(":%d", cfg.Port),
		Handler:        proxyHandler,
		ReadTimeout:    120 * time.Second,
		WriteTimeout:   120 * time.Second,
		IdleTimeout:    180 * time.Second,
		MaxHeaderBytes: 10 * 1024 * 1024, // 10MB Header buffer
	}

	// Handle graceful shutdown
	stopChan := make(chan os.Signal, 1)
	signal.Notify(stopChan, os.Interrupt, syscall.SIGTERM)

	go func() {
		fmt.Printf("[SERVER] Load balancer running on http://0.0.0.0:%d\n", cfg.Port)
		fmt.Printf("[SERVER] -> Dashboard URL : http://10.1.75.51:3221/lb-dashboard\n")
		fmt.Printf("[SERVER] -> Status API    : http://10.1.75.51:3221/lb-status\n")
		fmt.Printf("[SERVER] -> Main App UI   : http://10.1.75.51:3221/\n")
		fmt.Println("==================================================================")
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("[FATAL] HTTP server error: %v", err)
		}
	}()

	<-stopChan
	fmt.Println("\n[SERVER] Shutting down load balancer gracefully...")

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer shutdownCancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		log.Printf("[ERROR] Server forced to shutdown: %v", err)
	}

	fmt.Println("[SERVER] Load balancer exited.")
}
