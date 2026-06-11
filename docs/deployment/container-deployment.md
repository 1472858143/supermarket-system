# 容器化部署说明

## 目标环境

- 华为云 ECS
- Ubuntu 22.04 Server 64 位
- amd64 架构
- 2 核 CPU、4 GB 内存、40 GB 系统盘
- 仅通过公网 IP 的 HTTP 80 端口演示访问

## 开放端口

- `22/tcp`：SSH，仅用于部署和维护。
- `80/tcp`：Nginx 前端入口。

不要在安全组中开放 `8080/tcp` 和 `3306/tcp`。

## 安装 Docker

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
docker --version
docker compose version
```

## 创建部署用户和目录

```bash
sudo useradd -m -s /bin/bash deploy
sudo usermod -aG docker deploy
sudo mkdir -p /opt/supermarket-system
sudo chown -R deploy:deploy /opt/supermarket-system
sudo install -d -m 700 -o deploy -g deploy /home/deploy/.ssh
```

把 GitHub Actions 使用的部署公钥写入：

```bash
sudo -u deploy tee -a /home/deploy/.ssh/authorized_keys
sudo chmod 600 /home/deploy/.ssh/authorized_keys
```

## 首次准备代码和环境变量

```bash
sudo -iu deploy
cd /opt/supermarket-system
git clone https://github.com/1472858143/supermarket-system.git .
cp .env.example .env
chmod 600 .env
nano .env
```

`.env` 中必须改掉所有 `replace-with-` 开头的值。至少修改：

```text
MYSQL_ROOT_PASSWORD
DB_PASSWORD
JWT_SECRET
BOOTSTRAP_ADMIN_PASSWORD
BOOTSTRAP_USER_PASSWORD
```

## 手动启动

```bash
cd /opt/supermarket-system
docker compose up -d --build
docker compose ps
set -a
. ./.env
set +a
./scripts/verify-compose.sh
```

## GitHub Actions 自动部署

仓库需要配置以下 Secrets：

```text
DEPLOY_HOST=服务器公网 IP
DEPLOY_PORT=22
DEPLOY_USER=deploy
DEPLOY_PATH=/opt/supermarket-system
DEPLOY_SSH_KEY=部署用户私钥
```

推送 `main` 后，GitHub Actions 会测试、构建、登录服务器、更新代码、重新构建容器并执行冒烟验证。

## 日常运维

查看服务：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f --tail=200 backend
docker compose logs -f --tail=200 frontend
docker compose logs -f --tail=200 mysql
```

重启服务：

```bash
docker compose restart
```

备份数据库：

```bash
mkdir -p backups
set -a
. ./.env
set +a
docker compose exec -T mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" market > "backups/market-$(date +%Y%m%d-%H%M%S).sql"
```

禁止执行会删除数据库卷的命令：

```bash
docker compose down -v
```

## 首次验收

```bash
curl -I http://服务器公网IP/
curl -fsS http://服务器公网IP/api/health
```

预期：

- 前端返回 HTTP 200。
- 健康检查返回包含 `"status":"UP"` 的 JSON。
- 公网无法访问 `8080` 和 `3306`。
- 使用 `.env` 中设置的演示账号可以登录。
