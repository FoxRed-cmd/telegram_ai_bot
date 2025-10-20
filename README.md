# AI Telegram Bot

## Локальный запуск с LM Studio Desktop, Docker и собственным telegram ботом

Для запуска необходимо установить приложение LM Studio
и загрузить модели, по умолчанию в приложении используются:
- `qwen/qwen3-4b-2507` - модель для чата
- `text-embedding-nomic-embed-text-v1.5@f16` - модель для embedding-a

[Загрузить LM Studio](https://lmstudio.ai/home)

Загрузить модели можно используя пользовательский интерфейс или
с помощью командной строки `cmd` или `powershell`

[Сервис для поиска моделей](https://huggingface.co/)

Загрузка моделей:
```bash
lms get qwen/qwen3-4b-2507
```
```bash
lms get nomic-embed-text-v1.5
```

После загрузки можно посмотреть список моделей с помощью команды
```bash
lms ls
```
Вывод:
```
You have 5 models, taking up 8.60 GB of disk space.

LLM                               PARAMS    ARCH      SIZE
google/gemma-3-1b (1 variant)     1B  ****  gemma3    720.50 MB
qwen/qwen3-4b-2507 (1 variant)    4B        qwen3     2.50 GB
qwen/qwen3-8b (1 variant)         8B        qwen3     5.03 GB

EMBEDDING                                      PARAMS    ARCH          SIZE
text-embedding-nomic-embed-text-v1.5@f16                 Nomic BERT    274.29 MB
text-embedding-nomic-embed-text-v1.5@q4_k_m              Nomic BERT    84.11 MB
```
Чтобы передать название через переменные среды в приложение,
нужно отредактировать в файле `docker-compose` соответствующие переменные
```yaml
  ai-service:
    build: ./ai_service
    environment:
      EMBEDDING_MODEL: text-embedding-nomic-embed-text-v1.5@f16
      CHAT_MODEL: qwen/qwen3-4b-2507
```
После необходимо запустить сервер
```bash
lms server start
```
Вывод:
```
Success! Server is now running on port 1234
```

После установки LM Studio необходимо загрузить проект из репозитория
- [ZIP файл](https://github.com/FoxRed-cmd/telegram_ai_bot/archive/refs/heads/main.zip)
 или используя `git`

[Загрузить Git](https://git-scm.com/)

```bash
git clone https://github.com/FoxRed-cmd/telegram_ai_bot.git
```

Создать своего бота в Telegram используя [BotFather](https://t.me/BotFather) и получить token

После загрузки открыть папку проекта в командной строке, отретактировать `docker-compose`
если это необходимо и выполнить команду для запуска (Docker должен быть установлен и запущен)

[Загрузить Docker Desktop](https://www.docker.com/products/docker-desktop/)

```bash
cd telegram_ai_bot
```
```bash
echo "TELEGRAM_BOT_TOKEN=<token>" >> .env # Заменить <token> на валидный
```
```bash
docker-compose up -d --build
```
Вывод:
```
[+] Running 9/9
 ✔ telegram_ai_bot-ai-service               Built                                                                                                                                                             0.0s 
 ✔ telegram_ai_bot-bot-service              Built                                                                                                                                                             0.0s 
 ✔ Network telegram_ai_bot_default          Created                                                                                                                                                           0.1s 
 ✔ Volume telegram_ai_bot_pgvector_data     Created                                                                                                                                                           0.0s 
 ✔ Container telegram_ai_bot-kafka1-1       Started                                                                                                                                                           1.5s 
 ✔ Container telegram_ai_bot-pgvector-1     Started                                                                                                                                                           1.5s 
 ✔ Container telegram_ai_bot-kafka-ui-1     Started                                                                                                                                                           1.6s 
 ✔ Container telegram_ai_bot-ai-service-1   Started                                                                                                                                                           1.7s 
 ✔ Container telegram_ai_bot-bot-service-1  Started
```
После этого бот уже должен начать отвечать

Для загрузки документов в БД можно использовать
[admin панель](http://localhost:8081/documents)

