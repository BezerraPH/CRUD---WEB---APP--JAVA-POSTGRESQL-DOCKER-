# Instalação rápida (outra máquina ou GitHub)

## Pré-requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) com Compose v2
- Portas livres: **8080** (app), **8081** (Adminer), **5432** (PostgreSQL, opcional na rede)

## 1. Obter o código

```powershell
git clone https://github.com/BezerraPH/WEB_CRUD---Java_SpringBoot-PostgreSQL-Docker.git
cd WEB_CRUD---Java_SpringBoot-PostgreSQL-Docker
```

(Substitua pela URL do seu repositório, se for outro nome.)

## 2. Configurar ambiente

```powershell
copy .env.example .env
notepad .env
```

Defina uma senha forte em `POSTGRES_PASSWORD`. **Não commite o arquivo `.env`.**

## 3. Subir a aplicação

```powershell
docker compose up -d --build
docker compose ps
```

Aguarde os serviços `postgres`, `indicadores-app` e `adminer` ficarem saudáveis.

## 4. Acessar

| Serviço | URL |
|---------|-----|
| Aplicação | http://localhost:8080 |
| Adminer (SQL) | http://localhost:8081 |

Na rede interna, troque `localhost` pelo IP do servidor.

## 5. Primeira execução

Na subida, a aplicação:

- Cria tabelas de metadados e de cálculos, se não existirem
- Importa formulários do `modulos.yml` para o banco
- Permite criar novos formulários e fórmulas pela interface

## Comandos úteis

```powershell
docker compose logs -f indicadores-app   # ver logs da app
docker compose down                      # parar
docker compose down -v                   # parar e apagar volume do banco (CUIDADO)
```

## Problema de senha do PostgreSQL

Se alterou `POSTGRES_PASSWORD` no `.env` mas o container já existia, o volume antigo mantém a senha anterior. Veja a seção correspondente no [README.md](../README.md).

## Estrutura do repositório

```
.
├── docker-compose.yml      # PostgreSQL + app + Adminer
├── .env.example            # modelo de variáveis (copiar para .env)
├── sql/schema.sql          # tabelas iniciais (SISGCORP, PCE)
├── indicadores-crud/       # aplicação Spring Boot
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── docs/
│   ├── INSTALACAO.md       # este arquivo
│   └── ARQUITETURA.md
└── README.md               # documentação completa
```
