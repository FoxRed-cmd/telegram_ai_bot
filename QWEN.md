# Telegram AI Bot — Проект

## Обзор проекта

**Telegram AI Bot** — это микросервисное приложение для создания умного Telegram-бота с поддержкой RAG (Retrieval-Augmented Generation). Бот отвечает на вопросы пользователей, используя как собственные знания языковой модели, так и информацию из загруженных документов.

### Архитектура

Проект состоит из двух основных сервисов:

| Сервис | Описание | Порт |
|--------|----------|------|
| **ai-service** | AI-сервис на Spring AI с поддержкой векторного поиска (pgvector), обработкой документов и генерацией ответов через LLM | 8081 |
| **bot-service** | Telegram-бот на Spring Boot, обрабатывающий входящие сообщения и взаимодействующий с ai-service через Kafka | 8082 |

### Технологический стек

- **Языки**: Kotlin 1.9.25, Java 21
- **Фреймворки**: Spring Boot 3.5.6, Spring AI 1.0.3
- **Базы данных**: PostgreSQL 16 с расширением pgvector (векторное хранилище)
| **Брокер сообщений**: Apache Kafka (Bitnami)
- **LLM**: Поддержка OpenAI-compatible API (LM Studio, Ollama)
- **Embedding**: nomic-embed-text-v1.5 (768 измерений)
- **Telegram API**: telegrambots 9.1.0
- **Контейнеризация**: Docker, Docker Compose
- **Сборка**: Gradle Kotlin DSL

### Компоненты инфраструктуры

- **Kafka UI** — админ-панель для управления Kafka (порт 8765)
- **pgvector** — векторное хранилище документов (порт 5432)
- **AI Admin Panel** — веб-интерфейс для загрузки документов и настройки AI (порт 8081)

## Сборка и запуск

### Предварительные требования

1. **Docker Desktop** — установлен и запущен
2. **Git** — для клонирования репозитория
3. **LM Studio** или **Ollama** — для локального запуска LLM

### Загрузка моделей

#### LM Studio
```bash
lms get qwen/qwen3-4b-2507          # чат-модель
lms get nomic-embed-text-v1.5@f16   # embedding-модель
lms server start                     # запуск сервера на порту 1234
```

#### Ollama
```bash
ollama pull qwen3:4b
ollama pull nomic-embed-text
```

### Запуск через Docker Compose

1. Создать файл `.env` с токеном бота:
```bash
echo "TELEGRAM_BOT_TOKEN=<your_token>" >> .env
```

2. Запустить все сервисы:
```bash
docker-compose up -d --build
```

### Переменные окружения

#### ai-service
| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| `AI_URL` | URL LLM-сервера | `http://host.docker.internal:1234` |
| `CHAT_MODEL` | Модель для чата | `qwen/qwen3-4b-2507` |
| `EMBEDDING_MODEL` | Модель для эмбеддингов | `text-embedding-nomic-embed-text-v1.5@f16` |
| `VECTOR_DIMENSIONS` | Размерность вектора | `768` |
| `CHAT_TEMPERATURE` | Температура генерации | `0.7` |
| `SIMILARITY_THRESHOLD` | Порог схожести | `0.65` |
| `TOP_K_VALUE` | Количество контекстных документов | `6` |
| `POSTGRES_URL` | JDBC URL PostgreSQL | `jdbc:postgresql://pgvector:5432/test` |
| `KAFKA_SERVERS` | Kafka bootstrap servers | `kafka1:9090` |

#### bot-service
| Переменная | Описание |
|------------|----------|
| `TELEGRAM_BOT_TOKEN` | Токен Telegram-бота (из .env) |
| `KAFKA_SERVERS` | Kafka bootstrap servers |

### Остановка сервисов
```bash
docker-compose down
```

## Разработка

### Структура проекта

```
telegram_ai_bot/
├── ai_service/           # AI-сервис
│   ├── src/main/kotlin/com/viaibot/
│   │   ├── ai/          # Основная логика AI
│   │   │   ├── config/  # Конфигурация AI
│   │   │   ├── entity/  # JPA-сущности
│   │   │   ├── repository/ # Репозитории
│   │   │   ├── service/ # Сервисы
│   │   │   └── controller/ # REST-контроллеры
│   │   └── common/kafka/dto/ # Kafka DTO
│   └── src/main/resources/
│       ├── application.yml
│       ├── templates/   # Thymeleaf шаблоны
│       └── static/      # Статические файлы
│
├── bot_service/          # Telegram-бот
│   ├── src/main/kotlin/com/viaibot/
│   │   ├── bot/         # Основная логика бота
│   │   │   ├── config/  # Конфигурация
│   │   │   └── service/ # Сервисы
│   │   └── common/kafka/dto/ # Kafka DTO
│   └── src/main/resources/
│       └── application.yml
│
└── docker-compose.yml    # Оркестрация сервисов
```

### Kafka-топики

| Топик | Описание | Направление |
|-------|----------|-------------|
| `incoming-message` | Сообщения от бота к AI | bot-service → ai-service |
| `answer-message` | Ответы от AI к боту | ai-service → bot-service |

### Режимы работы бота

Пользователи могут переключать режимы через команды:

- `/simple` — бот отвечает, используя контекст из документов + собственные знания LLM
- `/strict` — бот отвечает только на основе загруженных документов
- `/custom` — бот отвечает по пользовательскому системному промпту (настраивается в admin-панели)

### Тестирование

```bash
# ai_service
cd ai_service && ./gradlew test

# bot_service
cd bot_service && ./gradlew test
```

## Админ-панель

Веб-интерфейс доступен по адресу: **http://localhost:8081**

Функционал:
- Загрузка документов (PDF, TXT, DOCX и др.)
- Просмотр загруженных документов
- Удаление документов
- Настройка системного промпта для режима `/custom`
- Настройка параметров AI (температура, порог схожести, top-k)

## Kafka UI

Интерфейс для мониторинга Kafka: **http://localhost:8765**

## PostgreSQL

Подключение к базе данных:
- **Host**: localhost
- **Port**: 5432
- **Database**: test
- **User**: test
- **Password**: test

## Известные ограничения

- Максимальная длина сообщения: 4096 символов (разбивается на части)
- Максимальный размер загружаемого файла: 100 MB
- Для работы требуется внешний LLM-сервер (LM Studio или Ollama)
