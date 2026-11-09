# Financial API

API REST para controle financeiro pessoal, feita em Spring Boot. Cadastro de usuário, login com JWT e CRUD de transações (receitas e despesas), com filtro por mês, por tipo e um resumo mensal (total de entradas, saídas, saldo, maior transação, etc).

Comecei esse projeto pra treinar Spring Security com JWT do zero (sem OAuth2, sem Auth0, só o fluxo manual com a lib jjwt) e pra ter algo mais completo pro portfólio do que os CRUDs básicos que eu já tinha feito antes.

## Stack

- Java 21
- Spring Boot 4.0.6
- Spring Security
- Spring Data JPA / Hibernate
- MySQL
- Flyway (migrations)
- JWT via jjwt
- Gradle e Maven
- JUnit Jupiter 5.14.1, Mockito e H2 (testes)

## Como rodar

Pré-requisitos: JDK 21 e MySQL 8.0.13+ (ou 8.4). Maven 3.9+ para os comandos Maven; o wrapper Gradle está incluído.

1. Crie um banco vazio de desenvolvimento:

```sql
CREATE DATABASE financial_api;
```

2. Configure estas variáveis de ambiente no terminal ou na configuração de execução da IDE:

| Variável | Uso |
| --- | --- |
| `DB_URL` | Padrão: `jdbc:mysql://localhost:3306/financial_api` |
| `DB_USERNAME` | Usuário do banco (obrigatório) |
| `DB_PASSWORD` | Senha do banco (obrigatória) |
| `JWT_SECRET_KEY` | Chave aleatória em Base64 com pelo menos 32 bytes decodificados (obrigatória) |
| `JWT_EXPIRATION_MS` | Validade do token em milissegundos; padrão `3600000` |

As credenciais não devem ser gravadas no código. O Spring não carrega `.env` automaticamente. No PowerShell, é possível gerar uma chave para a sessão sem imprimi-la:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET_KEY = [Convert]::ToBase64String($bytes)
```

3. Na pasta do projeto, execute uma das opções:

```powershell
mvn spring-boot:run
# ou
.\gradlew.bat bootRun
```

A API atende em `http://localhost:8080`. O Flyway cria as tabelas e o Hibernate valida o esquema. Os testes não exigem essas variáveis nem uma instalação de MySQL.

**Banco existente:** V1 e V2 foram corrigidas porque a primeira não declarava a chave primária do `AUTO_INCREMENT` e a segunda referenciava uma tabela inexistente. Também foram alinhados os tipos com as entidades. Se essas versões já constarem em um histórico Flyway, não execute `repair` nem recrie o banco com dados sem revisar o histórico e preparar uma migração específica. Nenhum banco MySQL existente foi alterado nesta revisão.

## Endpoints

### Autenticação

**Cadastro**
```
POST /auth/signup
```
```json
{
  "name": "Samuel",
  "email": "samuel@email.com",
  "password": "123456"
}
```

**Login**
```
POST /auth/login
```
```json
{
  "email": "samuel@email.com",
  "password": "123456"
}
```
Retorna o token JWT:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600000
}
```

A partir daqui, todo endpoint de transação precisa do header:
```
Authorization: Bearer <token>
```

### Transações

| Método | Rota                    | O que faz                                  |
|--------|-------------------------|---------------------------------------------|
| POST   | `/transactions`         | Cria uma transação                          |
| GET    | `/transactions/list`    | Lista todas as transações do usuário logado |
| GET    | `/transactions/filterMonth?year=2026&month=8` | Filtra por mês               |
| GET    | `/transactions/filterType?type=EXPENSE`       | Filtra por tipo (INCOME/EXPENSE) |
| GET    | `/transactions/summary?year=2026&month=8`     | Resumo do mês (total, saldo, maior transação, média) |
| PUT    | `/transactions/{id}`    | Atualiza uma transação (só o dono pode)     |
| DELETE | `/transactions/{id}`    | Remove uma transação (só o dono pode)       |

Exemplo de criação:
```json
{
  "date": "2026-08-19",
  "description": "supermercado",
  "amount": 350.00,
  "type": "EXPENSE"
}
```

O `user` da transação é sempre o dono do token — não precisa (e não deve) mandar isso no corpo da requisição.

## Estrutura do projeto

```
src/main/java/com/example/financial_api/demo/
├── configuration/   -> SecurityConfig, JwtAuthenticationFilter, ApplicationConfig
├── controller/      -> AuthController, TransactionController
├── entity/          -> User, Transaction, DTOs
├── repository/      -> UserRepository, TransactionRepository
└── service/         -> AuthService, JwtService, TransactionService
```

## Validação e regras

- Transações exigem data válida, descrição não vazia de até 255 caracteres, tipo e valor positivo de até `99999999.99`, com no máximo duas casas decimais.
- Cadastro exige nome e e-mail válidos de até 100 caracteres e senha não vazia. Senhas respeitam o limite de 72 bytes UTF-8 do BCrypt.
- `user`, `id` e `createdAt` recebidos no corpo não substituem o proprietário, identificador ou data de criação de uma transação.
- `/filterType` sem `type` lista todas as transações do usuário.
- Resumo mensal: `transactionCount` conta receitas e despesas; `averageTransaction` é a soma dos valores de todas as transações dividida pela quantidade, arredondada para duas casas com HALF_UP. Mês vazio retorna zeros. O saldo é receita menos despesa.
- Os valores públicos continuam como `Double` para preservar a API; os cálculos do resumo usam `BigDecimal` e a coluna monetária permanece `DECIMAL(10,2)`.
- Erros: `400` para dados/períodos inválidos, `401` para credenciais/token inválidos ou ausência de autenticação, `403` para editar/excluir transação alheia, `404` para transação inexistente e `409` para e-mail duplicado. Exclusão bem-sucedida retorna `204`.
- CORS permite a origem `http://localhost:8080`, conforme configuração existente, agora habilitada na cadeia de segurança.

## Testes

Na pasta `C:\projetos\financial_api`:

```powershell
mvn test
# Alternativa com o build original:
.\gradlew.bat test
```

Os testes unitários usam JUnit 5 e Mockito para serviços e filtro JWT. Os testes de integração usam H2 em modo MySQL, executam as migrations reais e validam o esquema com Hibernate, além dos endpoints via MockMvc. O contexto inicia em uma porta temporária e é encerrado ao final.

Para manter JUnit **5**, os testes iniciam o contexto explicitamente: o `SpringExtension` do Spring 7 exige JUnit 6 ([documentação oficial](https://docs.spring.io/spring-framework/docs/7.0.2/javadoc-api/org/springframework/test/context/junit/jupiter/SpringExtension.html)). As versões estão alinhadas no Maven e no Gradle.

Resultado verificado em 31/10/2026: **53 testes, 0 falhas, 0 erros e 0 ignorados** com `mvn test` (43 unitários e 10 de integração/inicialização). O comando `.\gradlew.bat test` também concluiu com sucesso, executando os mesmos 53 testes.

Relatórios: `target/surefire-reports/` (Maven) e `build/reports/tests/test/index.html` (Gradle). O H2 não substitui a validação em MySQL real; esta revisão não acessou o banco configurado anteriormente.

## Correções da revisão

| Problema | Correção |
| --- | --- |
| `mvn test` falhava por ausência de POM | Adicionado `pom.xml`, mantendo Gradle, Java 21 e Spring Boot 4.0.6. |
| Contagem comparava enum com texto e ficava zero | Contagem de todas as transações e média coerente, sem divisão por zero. |
| Resumo vazio retornava campos nulos e somas tinham imprecisão binária | Zeros explícitos e aritmética com `BigDecimal`. |
| Atualização sobrescrevia `createdAt` | Data de criação, ID e proprietário preservados. |
| Dados inválidos chegavam ao banco | Validação de cadastro/login, valores, datas, tipos e descrições; erros 400. |
| Mês inválido e transação ausente geravam erro genérico | Respostas 400/404; operações de outro usuário retornam 403 sem salvar ou excluir. |
| Filtro opcional por tipo buscava registros com tipo nulo | Ausência de filtro retorna todas as transações do usuário. |
| Hash de senha podia aparecer no JSON da entidade | `@JsonIgnore` no getter; erros de validação também não incluem valores rejeitados. |
| JWT inválido podia ser engolido pelo filtro | Retorno 401 e captura restrita a falhas de autenticação; falhas posteriores seguem o tratamento normal. |
| JWT sem sujeito/expiração causava falha ou validação incompleta | Validação rejeita claims ausentes, assinatura inválida e token expirado. |
| Login inválido gerava erro genérico | Resposta 401 sem detalhar credenciais. |
| Duplicidade só era tratada no controller | Regra no serviço, restrição única no banco e tratamento de cadastro concorrente. |
| Senha com caracteres multibyte podia ultrapassar o limite BCrypt | Limite validado em bytes antes de autenticar ou codificar. |
| CORS definido mas não ativado | Configuração conectada à cadeia de segurança. |
| Migrations inválidas e entidades incompatíveis com SQL | PK, FK para `user`, e-mail único, identidade automática, enum textual, valor decimal e datas alinhados. |
| Flyway sem integração de autoconfiguração para Boot 4 | Starter Flyway e versão do módulo MySQL gerenciada pelo Spring Boot. |
| Configuração continha segredos locais | Substituídos por variáveis de ambiente; testes usam somente chave de teste. |
| Teste de contexto dependia da configuração local | Banco em memória e configuração própria de testes, sem credenciais reais. |

As camadas controller, service, repository, entity e configuration e os caminhos dos endpoints foram preservados.

## Próximos passos

- Paginação nas listagens
- Categorias de transação (hoje só tem INCOME/EXPENSE)
- Ampliar os testes de integração para MySQL real
- Deploy no Railway

## Autor

Samuel Erbet de Souza Oliveira — [LinkedIn](#) · [GitHub](#)
