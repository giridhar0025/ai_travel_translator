from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.routers import auth as auth_router
from app.routers import conversations as conversations_router
from app.routers import favorites as favorites_router
from app.routers import languages as languages_router
from app.routers import me as me_router
from app.routers import phrases as phrases_router
from app.routers import recents as recents_router
from app.routers import settings as settings_router
from app.routers import stt as stt_router
from app.routers import translate as translate_router
from app.routers import tts as tts_router

settings = get_settings()

app = FastAPI(
    title="Travel Translator API",
    version="0.1.0",
    description="Backend for the Travel Translator AI mobile apps.",
    docs_url="/docs",
    redoc_url=None,
)

if settings.cors_origins_list:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins_list,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

API_PREFIX = "/api/v1"

app.include_router(auth_router.router, prefix=API_PREFIX)
app.include_router(me_router.router, prefix=API_PREFIX)
app.include_router(translate_router.router, prefix=API_PREFIX)
app.include_router(languages_router.router, prefix=API_PREFIX)
app.include_router(stt_router.router, prefix=API_PREFIX)
app.include_router(tts_router.router, prefix=API_PREFIX)
app.include_router(conversations_router.router, prefix=API_PREFIX)
app.include_router(phrases_router.router, prefix=API_PREFIX)
app.include_router(settings_router.router, prefix=API_PREFIX)
app.include_router(favorites_router.router, prefix=API_PREFIX)
app.include_router(recents_router.router, prefix=API_PREFIX)


@app.get("/health", tags=["meta"])
def health() -> dict[str, str]:
    return {"status": "ok"}
