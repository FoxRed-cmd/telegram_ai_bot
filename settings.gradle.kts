rootProject.name = "telegram_ai_bot"

include(":common")
include(":ai_service")
include(":bot_service")

project(":ai_service").projectDir = file("ai_service")
project(":bot_service").projectDir = file("bot_service")
