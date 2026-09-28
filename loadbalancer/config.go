package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"strings"
	"time"
)

type Config struct {
	Port              int           `json:"port"`
	Algorithm         string        `json:"algorithm"` // "round-robin", "least-connections", "ip-hash"
	HealthCheckPath   string        `json:"health_check_path"`
	HealthIntervalSec int           `json:"health_interval_sec"`
	HealthTimeoutSec  int           `json:"health_timeout_sec"`
	MaxRetries        int           `json:"max_retries"`
	Backends          []string      `json:"backends"`
	HealthInterval    time.Duration `json:"-"`
	HealthTimeout     time.Duration `json:"-"`
}

func LoadConfig() (*Config, error) {
	configPath := flag.String("config", "config.json", "Path to configuration file")
	port := flag.Int("port", 3000, "Port to listen on (3000 inside container -> 3221 outside)")
	backendsFlag := flag.String("backends", "", "Comma-separated list of backend URLs (e.g. http://10.1.75.51:3222,http://10.1.75.51:3223,http://10.1.75.51:3224)")
	algo := flag.String("algo", "round-robin", "Load balancing algorithm: round-robin, least-connections, ip-hash")
	flag.Parse()

	cfg := &Config{
		Port:              *port,
		Algorithm:         *algo,
		HealthCheckPath:   "/api/health",
		HealthIntervalSec: 8,
		HealthTimeoutSec:  6,
		MaxRetries:        3,
		Backends: []string{
			"http://10.1.75.51:5222",
			"http://10.1.75.51:5223",
			"http://10.1.75.51:5224",
		},
	}

	// Try reading JSON config if present
	if data, err := os.ReadFile(*configPath); err == nil {
		if err := json.Unmarshal(data, cfg); err != nil {
			fmt.Printf("[CONFIG] Warning: could not parse %s: %v. Using defaults.\n", *configPath, err)
		} else {
			fmt.Printf("[CONFIG] Loaded settings from %s\n", *configPath)
		}
	}

	// CLI flags override config file
	if *port != 3000 {
		cfg.Port = *port
	}
	if *algo != "round-robin" {
		cfg.Algorithm = *algo
	}
	if *backendsFlag != "" {
		cfg.Backends = strings.Split(*backendsFlag, ",")
		for i := range cfg.Backends {
			cfg.Backends[i] = strings.TrimSpace(cfg.Backends[i])
		}
	}

	cfg.HealthInterval = time.Duration(cfg.HealthIntervalSec) * time.Second
	cfg.HealthTimeout = time.Duration(cfg.HealthTimeoutSec) * time.Second

	return cfg, nil
}
