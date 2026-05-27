from sqlalchemy.ext.asyncio import AsyncSession


class BaseRepo:
    """Thin base for async repositories. No business logic — just data access.

    Phase 1 ships skeleton repos; downstream phases extend them with feature-specific
    methods (e.g. ConversationRepo.list_for_user) as those features land.
    """

    def __init__(self, session: AsyncSession) -> None:
        self.session = session
