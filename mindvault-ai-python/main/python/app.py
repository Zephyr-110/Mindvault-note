import sys

from fastapi import FastAPI

from main.python.api.ai_chat_controller import ai_router
from main.python.api.vector_database_controller import vector_router
from main.python.config.settings import settings

sys.stdout.reconfigure(line_buffering=True)

app = FastAPI()
app.include_router(ai_router)
app.include_router(vector_router)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        app,
        host=settings.API_HOST,
        port=settings.API_PORT,
        log_level="info",
    )