# CRUD de Indicadores (Java + Spring Boot)

Sistema web para **lançamento e gestão de indicadores operacionais**. Os dados ficam no **PostgreSQL** e alimentam painéis no **Grafana**. A interface é pensada para **uso em computadores** (menu lateral, formulário e tabela lado a lado).

Documentação complementar:

- [Instalação rápida (clone / outra máquina)](docs/INSTALACAO.md)
- [Arquitetura](docs/ARQUITETURA.md)

---

## Início rápido (GitHub + Docker)

```powershell
git clone https://github.com/BezerraPH/WEB_CRUD---Java_SpringBoot-PostgreSQL-Docker.git
cd WEB_CRUD---Java_SpringBoot-PostgreSQL-Docker
copy .env.example .env
# Edite .env e defina POSTGRES_PASSWORD
docker compose up -d --build
```

Acesse **http://localhost:8080** (Adminer: **http://localhost:8081**).

> **Segurança:** o arquivo `.env` não vai para o Git. Use senha forte e restrinja o acesso na rede.

---

## Índice

1. [O que o sistema faz](#o-que-o-sistema-faz)
2. [Requisitos](#requisitos)
3. [Tutorial: replicar em outro computador](#tutorial-replicar-em-outro-computador)
4. [Tutorial: operar no dia a dia](#tutorial-operar-no-dia-a-dia)
5. [Integração com Grafana](#integração-com-grafana)
6. [Criar indicador sem programar](#criar-indicador-sem-programar)
7. [Módulos avançados (YAML / PCE)](#módulos-avançados-yaml--pce)
8. [Desenvolvimento local](#desenvolvimento-local)
9. [Manutenção e problemas comuns](#manutenção-e-problemas-comuns)
10. [Estrutura do projeto](#estrutura-do-projeto)

---

## O que o sistema faz

| Função | Descrição |
|--------|-----------|
| Lançamento | Preencher formulários e gravar registros no PostgreSQL |
| Listagem | Ver, editar e excluir lançamentos na mesma tela |
| Construtor | Criar novos indicadores pela interface (nova tabela + metadados) |
| Grafana | Consulta SQL direta nas tabelas — sem API intermediária |
| Sem login | Acesso por link na rede; restrinja por firewall se necessário |

**Regra de dados:** a única chave única é `id`. Várias entradas no **mesmo dia** são permitidas.

---

## Requisitos

| Item | Versão sugerida |
|------|-----------------|
| Docker Desktop | Com Compose v2 |
| Portas livres | `8080` (app), `8081` (Adminer), `5432` (PostgreSQL) |
| Rede | Máquinas do Grafana e operadores devem alcançar o IP do servidor |

---

## Tutorial: replicar em outro computador

### Passo 1 — Obter o projeto

**Git (recomendado):**

```powershell
git clone https://github.com/BezerraPH/WEB_CRUD---Java_SpringBoot-PostgreSQL-Docker.git
cd WEB_CRUD---Java_SpringBoot-PostgreSQL-Docker
```

Ou copie a pasta do projeto (pendrive/rede) para o servidor.

### Passo 2 — Configurar variáveis de ambiente

```powershell
copy .env.example .env
notepad .env
```

Exemplo de `.env` (use sua própria senha):

```env
POSTGRES_DB=indicadores
POSTGRES_USER=postgres
POSTGRES_PASSWORD=sua_senha_segura
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
APP_PORT=8080
ADMINER_PORT=8081
```

- Em **Docker**, `POSTGRES_HOST` deve ser `postgres` (nome do serviço no Compose).
- Em **desenvolvimento local** (Maven sem Docker), use `POSTGRES_HOST=localhost`.

### Passo 3 — Subir os containers

```powershell
docker compose up -d --build
```

Aguarde os três serviços ficarem saudáveis:

```powershell
docker compose ps
```

### Passo 4 — Validar o acesso

| Serviço | URL local | Uso |
|---------|-----------|-----|
| **Aplicação** | http://localhost:8080 | Operadores lançam dados |
| **Adminer** | http://localhost:8081 | Consulta SQL manual |

Na rede interna, substitua `localhost` pelo IP do servidor (ex.: `http://10.47.22.57:8080`).

### Passo 5 — Adminer (primeira vez)

1. Abra http://IP:8081  
2. Sistema: **PostgreSQL**  
3. Servidor: `postgres`  
4. Usuário: `postgres`  
5. Senha: valor de `POSTGRES_PASSWORD` no `.env`  
6. Banco: `indicadores`

### Passo 6 — Grafana (outro servidor)

No Grafana, crie fonte de dados **PostgreSQL**:

| Campo | Valor |
|-------|-------|
| Host | IP da máquina onde roda o Docker |
| Port | `5432` |
| Database | `indicadores` |
| User / Password | `postgres` / senha do `.env` |
| SSL | Desligado (rede interna) |

Teste com: `SELECT COUNT(*) FROM dados_indicadores;`

### Passo 7 — Compartilhar com a equipe

Envie o link `http://IP:8080` apenas para pessoas autorizadas. Não há tela de login.

---

## Tutorial: operar no dia a dia

### Tela inicial (`/`)

Duas opções:

1. **Acessar formulários** — lista todos os indicadores  
2. **Criar formulário novo** — construtor visual

### Lançar dados

1. Menu lateral ou lista → escolha o indicador (ex.: SISGCORP, PCE, PAS)  
2. URL: `/m/{slug}`  
3. Preencha os campos (um abaixo do outro)  
4. Clique em **Gravar lançamento**  
5. O registro aparece na tabela à direita (em telas largas)

### Editar ou excluir um lançamento

Na tabela de registros:

- **Editar** — carrega os valores no formulário à esquerda  
- **Excluir** — pede confirmação antes de remover

### Gerenciar formulários

1. **Formulários** no cabeçalho ou `/formularios`  
2. Em cada item: **Preencher**, **Fórmulas**, **Editar** e **Excluir**  
3. Formulários iniciais em `modulos.yml` são importados ao banco na subida; depois são geridos pela interface

### Atalhos úteis

| URL | Função |
|-----|--------|
| `/` | Início |
| `/formularios` | Lista de formulários |
| `/formularios/novo` | Criar indicador |
| `/m/sisgcorp` | Lançamento SISGCORP |
| `/m/pce` | Lançamento PCE |

URLs antigas (`/admin/...`, `/painel`) redirecionam automaticamente.

---

## Integração com Grafana

### Princípio

O Grafana lê **diretamente** as tabelas do banco `indicadores`. Não é necessário exportar CSV.

### Exemplos de consulta

**Último registro SISGCORP:**

```sql
SELECT * FROM dados_indicadores
ORDER BY data_coleta DESC, id DESC
LIMIT 1;
```

**Soma de recebimentos PCE no período:**

```sql
SELECT SUM(pcerec_armas) AS total_armas
FROM pce_controle_destruicao
WHERE infog_data BETWEEN '2026-01-01' AND '2026-12-31';
```

**Último saldo PCE:**

```sql
SELECT saldoatualdearmas
FROM pce_controle_destruicao
ORDER BY infog_data DESC, id DESC
LIMIT 1;
```

### Campo Nº TRAM (PCE)

- Rótulo na tela: **Nº TRAM** (sigla, não “trâmite”)  
- Coluna no banco: `infog_nrtram`

---

## Criar indicador sem programar

1. Início → **Criar formulário novo** (ou `/formularios/novo`)  
2. Informe **título** e, se quiser, **identificador (slug)** e **nome da tabela**  
3. Adicione campos:
   - **Data** — obrigatório pelo menos um (ordenação e Grafana)  
   - **Número** — valores quantitativos  
   - **Texto** — observações curtas  
4. **Criar formulário**

O sistema:

- Cria a tabela física no PostgreSQL  
- Registra metadados em `formulario_def`, `formulario_grupo`, `formulario_campo`  
- Exibe o formulário em `/m/{slug}` e no menu lateral  

Para **novo campo** depois: `/formularios/{slug}/editar` → adicionar campo.

---

## Fórmulas e módulos iniciais (YAML)

Qualquer formulário pode ter **fórmulas** (`+`, `-`, `*`, `/`) pela tela **Fórmulas** em `/formularios/calculos/{slug}`.

Formulários pré-configurados ficam em `indicadores-crud/src/main/resources/modulos.yml` e são **importados para o banco** na subida da aplicação. Depois disso, edite/exclua/fórmulas pela interface.

O **PCE** traz fórmulas padrão de saldo; use **Restaurar fórmulas padrão PCE** se precisar voltar ao original.

---

## Desenvolvimento local

Pré-requisito: PostgreSQL acessível (container ou instalado).

```powershell
cd indicadores-crud
$env:POSTGRES_HOST="localhost"
$env:POSTGRES_PORT="5432"
$env:POSTGRES_DB="indicadores"
$env:POSTGRES_USER="postgres"
$env:POSTGRES_PASSWORD="sua_senha"
mvn spring-boot:run
```

Acesse http://localhost:8080

---

## Manutenção e problemas comuns

### Ver logs da aplicação

```powershell
docker compose logs -f indicadores-app
```

### Reiniciar só a aplicação

```powershell
docker compose restart indicadores-app
```

### Reset completo do banco (apaga todos os dados)

```powershell
docker compose down -v
docker compose up -d --build
```

Recria tabelas a partir de `sql/schema.sql`. Formulários dinâmicos criados pela interface serão perdidos.

### Erro “formulario_def não existe”

Na primeira subida, o app executa `db/02-formularios-meta.sql` automaticamente. Se persistir, verifique logs e permissões do usuário `postgres`.

### Porta 8080 em uso

Altere `APP_PORT` no `.env` (ex.: `8082`) e suba de novo.

### `password authentication failed for user "postgres"`

A senha no `.env` não bate com a senha gravada no **volume** do PostgreSQL (criado na primeira subida). O container Postgres ignora `POSTGRES_PASSWORD` depois que o volume já existe.

**Opção A — alinhar senha sem apagar dados** (no PowerShell, use a senha do seu `.env`):

```powershell
docker exec indicadores-postgres psql -U postgres -c "ALTER USER postgres WITH PASSWORD 'admin_654';"
docker restart indicadores-crud
```

**Opção B — recriar tudo do zero** (apaga todos os lançamentos):

```powershell
docker compose down -v
docker compose up -d --build
```

Confirme no Adminer (http://localhost:8081) com a mesma senha do `.env` antes de reiniciar a app.

---

## Estrutura do projeto

```
.
├── docker-compose.yml      # PostgreSQL + app + Adminer
├── .env.example            # Modelo de variáveis (copiar para .env)
├── .gitignore              # Não versiona .env, target/, Legislação/, etc.
├── sql/schema.sql          # Tabelas iniciais (SISGCORP, PCE)
├── docs/
│   ├── INSTALACAO.md       # Clone e Docker em outra máquina
│   └── ARQUITETURA.md
└── indicadores-crud/       # Spring Boot
    ├── src/main/java/      # Controllers, services, config
    ├── src/main/resources/
    │   ├── modulos.yml     # Módulos fixos
    │   ├── application.yml
    │   ├── templates/      # Thymeleaf (HTML)
    │   └── static/         # CSS e JS
    └── Dockerfile
```

### Stack

| Camada | Tecnologia |
|--------|------------|
| Backend | Java 17, Spring Boot 3 |
| Interface | Thymeleaf, Bootstrap 5, CSS institucional |
| Banco | PostgreSQL 16 |
| Deploy | Docker Compose |

---

## Segurança

- Não há autenticação na aplicação web.  
- Proteja o acesso na rede (firewall/VLAN).  
- Não exponha a porta `5432` na internet sem necessidade.  
- Mantenha senha forte em `POSTGRES_PASSWORD`.
#   W E B _ C R U D - - - J a v a _ S p r i n g B o o t - P o s t g r e S Q L - D o c k e r 
 
 #   W E B - C R U D - J A V A - P o s t g r e S Q L - D o c k e r -  
 