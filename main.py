from contextlib import asynccontextmanager

from apscheduler.schedulers.background import BackgroundScheduler
from fastapi import FastAPI

from movement import move


@asynccontextmanager
async def lifespan(app: FastAPI):
    scheduler = BackgroundScheduler()
    scheduler.add_job(
        move,
        trigger="interval",
        seconds=20,
        id="mouse_move",
        replace_existing=True,
    )
    scheduler.start()

    try:
        yield
    finally:
        scheduler.shutdown(wait=False)


app = FastAPI(lifespan=lifespan)


@app.get("/")
def root():
    return {"status": "running"}


if __name__ == "__main__":
    import uvicorn
    from log_config import LOG_CONFIG

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8666,
        reload=False,
        log_config=LOG_CONFIG,
    )