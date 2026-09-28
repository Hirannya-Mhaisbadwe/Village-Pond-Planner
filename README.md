# 🌊 OptiNeer: AI & Geospatial Village Pond Planning System

> **CSD Assignment 1 — Technical System & Deployment Guide**  
> **Author**: Hirannya Mhaisbadwe (Department of Computer Science & Engineering, IIT Bhilai)  
> **Repository**: [Hirannya-Mhaisbadwe/Village-Pond-Planner](https://github.com/Hirannya-Mhaisbadwe/Village-Pond-Planner)  
> **📺 Live Video Demonstration**: [https://youtu.be/C__Dr5hiEIo](https://youtu.be/C__Dr5hiEIo)

---

## 📌 Executive Summary

**OptiNeer** is a full-stack, distributed, cloud-native geospatial decision support platform designed to automate rural water conservation planning. The system combines:
1. **Digital Elevation Model (DEM) Topographical Processing** with deterministic **D8 Hydrological Flow-Routing**.
2. **Multi-Year Meteorological Analytics** (Open-Meteo & NASA POWER APIs) using the **Rational Runoff Method** ($Q = C \cdot I \cdot A$).
3. **Multi-Criteria Decision Analysis (MCDA)** for ranking optimal pond depression sites with Haversine spatial diversification.
4. **Custom High-Performance Go Load Balancer & Reverse Proxy** with active background health checks, least-connections routing, streaming uploads, and a live web telemetry dashboard.
5. **Interactive Web-GIS Interface** rendered via Leaflet.js with dynamic contour overlays, elevation heatmaps, and hydraulic scorecards.

---

## 🏗️ System Architecture & Port Mapping

The production environment is deployed across a 4-node distributed container cluster hosted on `10.1.75.51`:

```
                                [ Client Web Browser ]
                                          │
                                          ▼ HTTP (Port 6221)
                        ┌───────────────────────────────────┐
                        │   Custom Go Load Balancer Engine  │
                        │     (stu6_sys1 @ Port 6000)       │
                        └─┬───────────────┬───────────────┬─┘
                          │               │               │
            ┌─────────────┴──┐     ┌──────┴─────────┐   ┌─┴──────────────┐
            ▼                ▼     ▼                ▼   ▼                ▼
    [ Worker 1 (sys2) ]      [ Worker 2 (sys3) ]     [ Worker 3 (sys4) ]
    Spring Boot App          Spring Boot App         Spring Boot App
    (Port 5000 -> 5222)      (Port 5000 -> 5223)     (Port 5000 -> 5224)
            │                        │                        │
            └────────────────────────┼────────────────────────┘
                                     ▼ JDBC
                       ┌───────────────────────────┐
                       │   PostgreSQL Database     │
                       │   (stu6_sys1 @ Port 7000) │
                       └───────────────────────────┘
```

### 🌐 Container Port Forwarding Matrix

| Node Hostname | SSH Port | Role | Container Internal Port | External Accessible Port / URL |
| :--- | :---: | :--- | :---: | :--- |
| **`stu6_sys1`** | `2221` | **PostgreSQL Database** | `7000` | `10.1.75.51:7221` |
| **`stu6_sys1`** | `2221` | **Go Load Balancer** | `6000` | 👉 **`http://10.1.75.51:6221`** |
| **`stu6_sys1`** | `2221` | **Live LB Dashboard** | `6000` | 👉 **`http://10.1.75.51:6221/lb-dashboard`** |
| **`stu6_sys2`** | `2222` | **Spring Boot Worker 1** | `5000` | `http://10.1.75.51:5222` |
| **`stu6_sys3`** | `2223` | **Spring Boot Worker 2** | `5000` | `http://10.1.75.51:5223` |
| **`stu6_sys4`** | `2224` | **Spring Boot Worker 3** | `5000` | `http://10.1.75.51:5224` |

---

## ⚡ Complete Deployment Guide

### 1. Build Binaries Locally (on Development Machine)

```powershell
# 1. Package the Spring Boot JAR
mvn clean package -DskipTests

# 2. Upload JAR to Worker Nodes (sys2, sys3, sys4)
scp -P 2222 target\Pond-Planning-Application-0.0.1-SNAPSHOT.jar student@10.1.75.51:~/app.jar
scp -P 2223 target\Pond-Planning-Application-0.0.1-SNAPSHOT.jar student@10.1.75.51:~/app.jar
scp -P 2224 target\Pond-Planning-Application-0.0.1-SNAPSHOT.jar student@10.1.75.51:~/app.jar

# 3. Upload Go Load Balancer Source to sys1
scp -P 2221 -r loadbalancer\* student@10.1.75.51:~/loadbalancer/
```

---

### 2. Launch PostgreSQL Database on `stu6_sys1`

SSH into `sys1`:
```bash
ssh -p 2221 student@10.1.75.51

# Start PostgreSQL service and initialize pond_db
sudo service postgresql start || sudo systemctl start postgresql
sudo -u postgres psql -c "CREATE DATABASE pond_db;" || true
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'postgres123';"
```

---

### 3. Launch Spring Boot Workers (`sys2`, `sys3`, `sys4`)

Run this background daemon command on each worker node:

* **On `stu6_sys2` (`ssh -p 2222 student@10.1.75.51`):**
  ```bash
  sudo fuser -k 5000/tcp || true
  nohup java -Xmx384m -jar ~/app.jar --server.port=5000 > app.log 2>&1 &
  ```

* **On `stu6_sys3` (`ssh -p 2223 student@10.1.75.51`):**
  ```bash
  sudo fuser -k 5000/tcp || true
  nohup java -Xmx384m -jar ~/app.jar --server.port=5000 > app.log 2>&1 &
  ```

* **On `stu6_sys4` (`ssh -p 2224 student@10.1.75.51`):**
  ```bash
  sudo fuser -k 5000/tcp || true
  nohup java -Xmx384m -jar ~/app.jar --server.port=5000 > app.log 2>&1 &
  ```

*(To verify startup, check logs with `tail -f app.log` until `Started PondPlanningApplication` appears).*

---

### 4. Build & Launch Go Load Balancer on `stu6_sys1`

SSH into `sys1`:
```bash
ssh -p 2221 student@10.1.75.51
cd ~/loadbalancer

# Terminate any old instances & free port 6000
pkill -9 -f pond-lb || true
sudo fuser -k 6000/tcp || true

# Recompile and run in background
go build -o pond-lb .
nohup ./pond-lb -port=6000 -config=config.json > lb.log 2>&1 &

# Follow live traffic logs
tail -f lb.log
```

---

## 📊 Live Monitoring & Verifications

* **Main Application UI**: [http://10.1.75.51:6221](http://10.1.75.51:6221)
* **Real-time Load Balancer Dashboard**: [http://10.1.75.51:6221/lb-dashboard](http://10.1.75.51:6221/lb-dashboard)
* **Raw Cluster Health Telemetry**: [http://10.1.75.51:6221/lb-status](http://10.1.75.51:6221/lb-status)

---

## 🛠️ REST API Specification

| HTTP Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Node health probe used by Load Balancer |
| `GET` | `/api/location/search?village={v}&tehsil={t}` | Geocodes village coordinates with offline fallback |
| `POST` | `/api/planning/pipeline` | Full end-to-end DEM, contour, rainfall, runoff, and pond sizing analysis |
| `POST` | `/api/planning/analyze` | Fast terrain elevation contour and slope profiling |
| `POST` | `/api/terrain/getcontours` | Extracts discretized elevation contour polylines |
| `POST` | `/api/runoff/estimate` | Rational runoff computation based on soil and precipitation data |
| `POST` | `/api/pond/size` | Earthwork excavation and embankment geometry sizing |
| `GET` | `/lb-dashboard` | Real-time visual cluster traffic distribution web dashboard |
| `GET` | `/lb-status` | JSON telemetry of active connections and per-node latencies |

---

## 🧮 Mathematical & Algorithmic Formulation

### 1. D8 Flow Routing & Accumulation
For each grid cell $(i, j)$ in DEM elevation matrix $Z$:
$$k^* = \arg\max_{k \in \{1 \dots 8\}} \frac{z(i,j) - z(k)}{d_k}$$
$$A_{acc}(i,j) = 1 + \sum_{m \in \text{Inflow}(i,j)} A_{acc}(m)$$

### 2. Rational Runoff Discharge & Volume
$$Q_{peak} = 0.278 \cdot C \cdot I \cdot A_{catchment} \quad (\text{m}^3/\text{s})$$
$$V_{annual} = 10 \cdot P_{annual} \cdot C \cdot A_{catchment} \quad (\text{m}^3)$$

### 3. Multi-Criteria Suitability Scoring (MCDA)
$$F_{score}(i,j) = 0.50 \cdot \widehat{A}_{acc}(i,j) + 0.30 \cdot (1 - \widehat{z}(i,j)) + 0.20 \cdot \Phi_{slope}(S(i,j))$$
Subject to minimum spatial separation: $\text{Haversine}(Site_a, Site_b) \ge 400\,\text{m}$.

---

## 🧪 Local Testing & Development

Run the full automated test suite locally:
```bash
mvn test
```
* **Result**: 18 / 18 Unit and Integration Tests Passing ($100\%$).
* **Local Web Interface**: `http://localhost:8080`
