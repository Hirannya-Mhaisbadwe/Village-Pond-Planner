# Distributed Pond Planning Cluster & Custom Go Load Balancer

This architecture distributes the **OptiNeer Village Pond Planning Application** across 4 systems/containers with a shared PostgreSQL database and a high-performance Go Load Balancer.

---

## 1. System Allocation & Network Port Mapping

According to the container port forwarding rules:
- **Application 1 (Internal Port `3000`)** $\rightarrow 3000 + \text{ID}$ (`3221`, `3222`, `3223`, `3224`)
- **Application 2 (Internal Port `4000`)** $\rightarrow 4000 + \text{ID}$ (`4221`, `4222`, `4223`, `4224`)
- **Application 3 (Internal Port `5000`)** $\rightarrow 5000 + \text{ID}$ (`5221`, `5222`, `5223`, `5224`)
- **Application 4 (Internal Port `6000`)** $\rightarrow 6000 + \text{ID}$ (`6221`, `6222`, `6223`, `6224`)
- **Application 5 (Internal Port `7000`)** $\rightarrow 7000 + \text{ID}$ (`7221`, `7222`, `7223`, `7224`)

### Allocated Architecture:

| System Name | SSH Login Command | Internal Port | External Port (`10.1.75.51`) | Role & Hosted Service |
|:---|:---|:---|:---|:---|
| **`stu6_sys1`** | `ssh -p 2221 student@10.1.75.51` | `3000`<br>`7000` | **`3221`** (Public Web UI)<br>**`7221`** (Shared PostgreSQL) | **Load Balancer + Central Database**<br>• Custom Go Load Balancer (Port `3000` $\rightarrow$ `3221`)<br>• Central PostgreSQL database (Port `7000` $\rightarrow$ `7221`) |
| **`stu6_sys2`** | `ssh -p 2222 student@10.1.75.51` | `4000` | **`4222`** (Worker 1) | **Backend Instance 1** (Spring Boot + Web UI) |
| **`stu6_sys3`** | `ssh -p 2223 student@10.1.75.51` | `4000` | **`4223`** (Worker 2) | **Backend Instance 2** (Spring Boot + Web UI) |
| **`stu6_sys4`** | `ssh -p 2224 student@10.1.75.51` | `4000` | **`4224`** (Worker 3) | **Backend Instance 3** (Spring Boot + Web UI) |

*(Note: If you wish to use internal port `5000` instead, the external ports become `5222`, `5223`, `5224`.)*

---

## 2. Architecture Diagram

```
                              [ Client Browser ]
                                      │
                                      ▼
             Public Entry: http://10.1.75.51:3221 (stu6_sys1:3000)
                     ┌──────────────────────────────────┐
                     │     Custom Go Load Balancer      │
                     │  (Round-Robin / Health Checking) │
                     └────────────────┬─────────────────┘
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          ▼                           ▼                           ▼
[ Worker 1: stu6_sys2 ]     [ Worker 2: stu6_sys3 ]     [ Worker 3: stu6_sys4 ]
(Spring Boot on port 4000)  (Spring Boot on port 4000)  (Spring Boot on port 4000)
Ext Port: 10.1.75.51:4222   Ext Port: 10.1.75.51:4223   Ext Port: 10.1.75.51:4224
          │                           │                           │
          └───────────────────────────┼───────────────────────────┘
                                      ▼
                      [ Shared PostgreSQL Database ]
                         Hosted on stu6_sys1:7000
                       (External: 10.1.75.51:7221)
```

---

## 3. Step-by-Step Deployment Instructions

### Step 1: Set Up Shared PostgreSQL on `stu6_sys1`
1. Connect via SSH:
   ```bash
   ssh -p 2221 student@10.1.75.51
   ```
2. Run database setup:
   ```bash
   sudo apt-get update && sudo apt-get install -y postgresql postgresql-contrib
   sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'postgres123';"
   sudo -u postgres psql -c "CREATE DATABASE pond_db OWNER postgres;"
   
   # Configure PostgreSQL to listen on all interfaces on port 7000 (External 7221)
   PG_CONF=$(sudo find /etc/postgresql/ -name "postgresql.conf" | head -n 1)
   PG_HBA=$(sudo find /etc/postgresql/ -name "pg_hba.conf" | head -n 1)
   sudo sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/g" "$PG_CONF"
   sudo sed -i "s/listen_addresses = 'localhost'/listen_addresses = '*'/g" "$PG_CONF"
   sudo sed -i "s/port = 5432/port = 7000/g" "$PG_CONF"
   sudo sed -i "s/#port = 5432/port = 7000/g" "$PG_CONF"
   echo "host all all 0.0.0.0/0 md5" | sudo tee -a "$PG_HBA"
   sudo service postgresql restart
   ```

---

### Step 2: Build Application JAR Locally and Copy to Worker Systems
From your local Windows terminal in `c:\Users\Dell\Desktop\JavaBackend\Pond-Planning-Application`:
```powershell
# 1. Package the Spring Boot JAR
.\mvnw.cmd clean package -DskipTests

# 2. Copy JAR to stu6_sys2, stu6_sys3, stu6_sys4
scp -P 2222 target\Pond-Planning-Application-0.0.1-SNAPSHOT.jar student@10.1.75.51:~/app.jar
scp -P 2223 target\Pond-Planning-Application-0.0.1-SNAPSHOT.jar student@10.1.75.51:~/app.jar
scp -P 2224 target\Pond-Planning-Application-0.0.1-SNAPSHOT.jar student@10.1.75.51:~/app.jar
```

---

### Step 3: Run Backend on Worker 1 (`stu6_sys2` on Port 4000 $\rightarrow$ 4222)
```bash
ssh -p 2222 student@10.1.75.51
java -Xmx512m -jar ~/app.jar \
  --server.port=4000 \
  --spring.datasource.url="jdbc:postgresql://10.1.75.51:7221/pond_db?sslmode=disable" \
  --spring.datasource.driver-class-name="org.postgresql.Driver" \
  --spring.datasource.username="postgres" \
  --spring.datasource.password="postgres123" \
  --spring.jpa.properties.hibernate.dialect="org.hibernate.dialect.PostgreSQLDialect" \
  --spring.jpa.hibernate.ddl-auto=update
```

---

### Step 4: Run Backend on Worker 2 (`stu6_sys3` on Port 4000 $\rightarrow$ 4223)
```bash
ssh -p 2223 student@10.1.75.51
java -Xmx512m -jar ~/app.jar \
  --server.port=4000 \
  --spring.datasource.url="jdbc:postgresql://10.1.75.51:7221/pond_db?sslmode=disable" \
  --spring.datasource.driver-class-name="org.postgresql.Driver" \
  --spring.datasource.username="postgres" \
  --spring.datasource.password="postgres123" \
  --spring.jpa.properties.hibernate.dialect="org.hibernate.dialect.PostgreSQLDialect" \
  --spring.jpa.hibernate.ddl-auto=update
```

---

### Step 5: Run Backend on Worker 3 (`stu6_sys4` on Port 4000 $\rightarrow$ 4224)
```bash
ssh -p 2224 student@10.1.75.51
java -Xmx512m -jar ~/app.jar \
  --server.port=4000 \
  --spring.datasource.url="jdbc:postgresql://10.1.75.51:7221/pond_db" \
  --spring.datasource.driver-class-name="org.postgresql.Driver" \
  --spring.datasource.username="postgres" \
  --spring.datasource.password="postgres123" \
  --spring.jpa.hibernate.ddl-auto=update
```

---

### Step 6: Deploy and Run Custom Go Load Balancer on `stu6_sys1`
1. Copy the `loadbalancer` folder to `stu6_sys1`:
   ```powershell
   scp -P 2221 -r loadbalancer student@10.1.75.51:~/loadbalancer
   ```
2. Connect to `stu6_sys1`:
   ```bash
   ssh -p 2221 student@10.1.75.51
   cd ~/loadbalancer
   sudo apt-get update && sudo apt-get install -y golang-go
   go build -o pond-lb *.go
   ./pond-lb -port=3000 -config=config.json
   ```

---

## 4. Verification & Testing

1. **Access Web Application (via Load Balancer)**:  
   👉 **`http://10.1.75.51:3221`**
2. **Access Live Cluster Dashboard**:  
   👉 **`http://10.1.75.51:3221/lb-dashboard`**
3. **Query Health Status JSON**:  
   ```bash
   curl http://10.1.75.51:3221/lb-status
   ```
