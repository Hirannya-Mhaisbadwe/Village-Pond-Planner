package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

type BackendStatus struct {
	URL               string `json:"url"`
	Alive             bool   `json:"alive"`
	ActiveConnections int64  `json:"active_connections"`
	TotalRequests     int64  `json:"total_requests"`
	TotalErrors       int64  `json:"total_errors"`
	LastLatencyMs     int64  `json:"last_latency_ms"`
	LastChecked       string `json:"last_checked"`
}

type ClusterStatusResponse struct {
	LoadBalancerAlgorithm string          `json:"load_balancer_algorithm"`
	TotalClusterRequests  int64           `json:"total_cluster_requests"`
	TotalActiveConns      int64           `json:"total_active_connections"`
	HealthyBackends       int             `json:"healthy_backends"`
	TotalBackends         int             `json:"total_backends"`
	Timestamp             time.Time       `json:"timestamp"`
	Backends              []BackendStatus `json:"backends"`
}

func ServeStatusJSON(w http.ResponseWriter, r *http.Request, lb *LoadBalancer) {
	resp := buildClusterStatus(lb)
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Access-Control-Allow-Origin", "*")
	json.NewEncoder(w).Encode(resp)
}

func buildClusterStatus(lb *LoadBalancer) ClusterStatusResponse {
	backends := lb.GetBackends()
	statusList := make([]BackendStatus, 0, len(backends))

	var totalReqs, totalActive int64
	healthyCount := 0

	for _, b := range backends {
		alive := b.IsAlive()
		if alive {
			healthyCount++
		}
		conns := b.GetActiveConns()
		reqs := b.GetTotalRequests()
		totalActive += conns
		totalReqs += reqs

		statusList = append(statusList, BackendStatus{
			URL:               b.URL.String(),
			Alive:             alive,
			ActiveConnections: conns,
			TotalRequests:     reqs,
			TotalErrors:       b.GetTotalErrors(),
			LastLatencyMs:     b.GetLatency(),
			LastChecked:       b.LastChecked.Format(time.RFC3339),
		})
	}

	return ClusterStatusResponse{
		LoadBalancerAlgorithm: lb.config.Algorithm,
		TotalClusterRequests:  totalReqs,
		TotalActiveConns:      totalActive,
		HealthyBackends:       healthyCount,
		TotalBackends:         len(backends),
		Timestamp:             time.Now(),
		Backends:              statusList,
	}
}

func ServeDashboardHTML(w http.ResponseWriter, r *http.Request, lb *LoadBalancer) {
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	html := `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>OptiNeer Cluster | Load Balancer Dashboard</title>
  <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;700;800&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #07131b;
      --card: #0d2230;
      --accent: #14b8a6;
      --accent-glow: rgba(20, 184, 166, 0.25);
      --text: #e2e8f0;
      --muted: #829ab1;
      --success: #10b981;
      --danger: #ef4444;
      --border: rgba(255, 255, 255, 0.08);
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background: var(--bg);
      color: var(--text);
      font-family: 'Outfit', sans-serif;
      padding: 32px 24px;
      min-height: 100vh;
    }
    .container { max-width: 1100px; margin: 0 auto; }
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 28px;
      padding-bottom: 20px;
      border-bottom: 1px solid var(--border);
    }
    .brand { display: flex; align-items: center; gap: 14px; }
    .brand-icon {
      width: 44px; height: 44px; border-radius: 12px;
      background: linear-gradient(135deg, #14b8a6, #065f46);
      display: grid; place-items: center; font-size: 22px; font-weight: 800; color: #fff;
      box-shadow: 0 0 20px var(--accent-glow);
    }
    h1 { font-size: 24px; font-weight: 800; letter-spacing: -0.03em; }
    .subtitle { color: var(--muted); font-size: 13px; margin-top: 2px; }
    .status-badge {
      display: inline-flex; align-items: center; gap: 8px;
      padding: 8px 16px; border-radius: 99px;
      background: rgba(16, 185, 129, 0.12); color: var(--success);
      font-size: 13px; font-weight: 700; border: 1px solid rgba(16, 185, 129, 0.3);
    }
    .pulse-dot {
      width: 8px; height: 8px; border-radius: 50%;
      background: var(--success); box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.25);
    }
    .stats-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px; margin-bottom: 28px;
    }
    .stat-card {
      background: var(--card); border: 1px solid var(--border);
      border-radius: 16px; padding: 20px;
      backdrop-filter: blur(12px);
    }
    .stat-label { color: var(--muted); font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; }
    .stat-val { font-size: 32px; font-weight: 800; color: #fff; margin-top: 6px; }
    .nodes-section h2 { font-size: 18px; margin-bottom: 16px; font-weight: 700; }
    .node-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 16px; }
    .node-card {
      background: var(--card); border: 1px solid var(--border);
      border-radius: 16px; padding: 22px; position: relative;
      transition: transform 0.2s ease, border-color 0.2s ease;
    }
    .node-card:hover { transform: translateY(-3px); border-color: rgba(20, 184, 166, 0.4); }
    .node-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .node-url { font-family: 'JetBrains Mono', monospace; font-size: 14px; font-weight: 600; color: #38bdf8; }
    .node-tag {
      font-size: 11px; font-weight: 700; padding: 4px 10px; border-radius: 99px;
      text-transform: uppercase; letter-spacing: 0.05em;
    }
    .tag-online { background: rgba(16, 185, 129, 0.15); color: var(--success); border: 1px solid rgba(16, 185, 129, 0.3); }
    .tag-offline { background: rgba(239, 68, 68, 0.15); color: var(--danger); border: 1px solid rgba(239, 68, 68, 0.3); }
    .metrics-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.04); font-size: 13px; }
    .metrics-row:last-child { border-bottom: none; }
    .metrics-row span:first-child { color: var(--muted); }
    .metrics-row span:last-child { font-weight: 700; color: #fff; font-family: 'JetBrains Mono', monospace; }
    .quick-links { margin-top: 32px; display: flex; gap: 12px; }
    .btn {
      padding: 10px 18px; border-radius: 10px; font-size: 13px; font-weight: 700;
      text-decoration: none; color: #fff; background: #134e4a; border: 1px solid rgba(20, 184, 166, 0.3);
      display: inline-flex; align-items: center; gap: 8px; transition: background 0.2s ease;
    }
    .btn:hover { background: #0f766e; }
  </style>
</head>
<body>
  <div class="container">
    <header>
      <div class="brand">
        <div class="brand-icon">⚡</div>
        <div>
          <h1>OptiNeer Cluster Load Balancer</h1>
          <div class="subtitle">Distributed Pond Planning Engine · Go Reverse Proxy</div>
        </div>
      </div>
      <div class="status-badge"><span class="pulse-dot"></span> Live Traffic Distribution</div>
    </header>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">Algorithm</div>
        <div class="stat-val" id="val-algo" style="font-size: 22px; text-transform: capitalize;">—</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Total Cluster Requests</div>
        <div class="stat-val" id="val-requests">—</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Active Connections</div>
        <div class="stat-val" id="val-conns">—</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Healthy Nodes</div>
        <div class="stat-val" id="val-health">—</div>
      </div>
    </div>

    <div class="nodes-section">
      <h2>Backend Cluster Nodes</h2>
      <div class="node-grid" id="nodes-container">
        <!-- Rendered by JS -->
      </div>
    </div>

    <div class="quick-links">
      <a href="/" class="btn">🚀 Open Application Web UI</a>
      <a href="/lb-status" target="_blank" class="btn">📊 View Raw JSON Metrics</a>
    </div>
  </div>

  <script>
    async function updateDashboard() {
      try {
        const res = await fetch('/lb-status');
        if (!res.ok) return;
        const data = await res.json();
        document.getElementById('val-algo').textContent = data.load_balancer_algorithm;
        document.getElementById('val-requests').textContent = Number(data.total_cluster_requests).toLocaleString();
        document.getElementById('val-conns').textContent = data.total_active_connections;
        document.getElementById('val-health').textContent = data.healthy_backends + ' / ' + data.total_backends;

        const container = document.getElementById('nodes-container');
        container.innerHTML = '';

        data.backends.forEach(function(node, idx) {
          var tagClass = node.alive ? 'tag-online' : 'tag-offline';
          var tagText = node.alive ? 'HEALTHY' : 'OFFLINE';
          var errColor = node.total_errors > 0 ? '#ef4444' : '#10b981';

          var card = document.createElement('div');
          card.className = 'node-card';
          card.innerHTML = 
            '<div class="node-header">' +
              '<span class="node-url">' + node.url + '</span>' +
              '<span class="node-tag ' + tagClass + '">' + tagText + '</span>' +
            '</div>' +
            '<div class="metrics-row"><span>Node Role</span><span>Worker ' + (idx + 1) + ' (Backend + GIS)</span></div>' +
            '<div class="metrics-row"><span>Active Conns</span><span>' + node.active_connections + '</span></div>' +
            '<div class="metrics-row"><span>Total Requests</span><span>' + node.total_requests + '</span></div>' +
            '<div class="metrics-row"><span>Failed Requests</span><span style="color: ' + errColor + '">' + node.total_errors + '</span></div>' +
            '<div class="metrics-row"><span>Last Latency</span><span>' + node.last_latency_ms + ' ms</span></div>';
          container.appendChild(card);
        });
      } catch (err) {
        console.error('Failed to refresh LB dashboard', err);
      }
    }

    updateDashboard();
    setInterval(updateDashboard, 2000);
  </script>
</body>
</html>`
	fmt.Fprint(w, html)
}
