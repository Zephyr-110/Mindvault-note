from main.python.api.ai_chat_controller import AIChatController
from main.python.config.settings import settings


app = AIChatController.app


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        app,
        host=settings.API_HOST,
        port=settings.API_PORT,
        log_level="info",
    )