package main

import (
	"fmt"
	"hash/fnv"
	"net"
	"net/http"
	"sync"
	"sync/atomic"
)

type LoadBalancer struct {
	backends []*Backend
	current  uint64
	config   *Config
	mux      sync.RWMutex
}

func NewLoadBalancer(cfg *Config) (*LoadBalancer, error) {
	lb := &LoadBalancer{
		backends: make([]*Backend, 0, len(cfg.Backends)),
		config:   cfg,
	}

	for _, rawURL := range cfg.Backends {
		b, err := NewBackend(rawURL)
		if err != nil {
			return nil, fmt.Errorf("failed to parse backend %s: %w", rawURL, err)
		}
		lb.backends = append(lb.backends, b)
	}

	return lb, nil
}

func (lb *LoadBalancer) GetNextBackend(r *http.Request) *Backend {
	lb.mux.RLock()
	defer lb.mux.RUnlock()

	switch lb.config.Algorithm {
	case "least-connections":
		return lb.leastConnections()
	case "ip-hash":
		return lb.ipHash(r)
	default:
		return lb.roundRobin()
	}
}

// Round Robin
func (lb *LoadBalancer) roundRobin() *Backend {
	total := len(lb.backends)
	if total == 0 {
		return nil
	}

	for i := 0; i < total; i++ {
		idx := int(atomic.AddUint64(&lb.current, 1)-1) % total
		if lb.backends[idx].IsAlive() {
			return lb.backends[idx]
		}
	}
	// Resilient Soft Fallback: If all backends are temporarily unalive, dispatch to round robin node
	idx := int(atomic.LoadUint64(&lb.current)) % total
	return lb.backends[idx]
}

// Least Connections
func (lb *LoadBalancer) leastConnections() *Backend {
	var minBackend *Backend
	minConns := int64(1<<62 - 1)

	for _, b := range lb.backends {
		if !b.IsAlive() {
			continue
		}
		conns := b.GetActiveConns()
		if conns < minConns {
			minConns = conns
			minBackend = b
		}
	}

	if minBackend == nil && len(lb.backends) > 0 {
		// Fallback to round robin if none available
		return lb.roundRobin()
	}
	return minBackend
}

// IP Hash for session stickiness
func (lb *LoadBalancer) ipHash(r *http.Request) *Backend {
	aliveList := make([]*Backend, 0, len(lb.backends))
	for _, b := range lb.backends {
		if b.IsAlive() {
			aliveList = append(aliveList, b)
		}
	}
	if len(aliveList) == 0 {
		return nil
	}

	ip, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		ip = r.RemoteAddr
	}

	h := fnv.New32a()
	h.Write([]byte(ip))
	idx := int(h.Sum32()) % len(aliveList)
	return aliveList[idx]
}

// GetBackends returns the list of all backends
func (lb *LoadBalancer) GetBackends() []*Backend {
	lb.mux.RLock()
	defer lb.mux.RUnlock()
	return lb.backends
}
