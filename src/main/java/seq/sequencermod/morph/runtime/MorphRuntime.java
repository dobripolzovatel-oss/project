    package seq.sequencermod.morph.runtime;

    import java.util.Objects;
    import java.util.UUID;
    import java.util.function.Predicate;

    /**
     * Фасад рантайма морфа.
     * Этот класс изолирует интеграцию с твоей системой (рендер/атрибуты/неткод).
     *
     * TODO: реализуй applyMorphServerSide / clearMorphServerSide / notifyClientMorphApplied
     * под свои существующие менеджеры и пакеты.
     */
    public final class MorphRuntime {

        // Конфиг можно переинициализировать при старте сервера/мода.
        private static volatile MorphConfig CONFIG = MorphConfig.loadOrCreate();

        // Валидатор сущностей — чтобы проверить существование id и "живость".
        // В реальной интеграции внедри реализацию, которая ходит в реестр сущностей.
        public interface EntityValidator {
            boolean exists(String entityId);
            boolean isLiving(String entityId); // для флага allowNonLivingMorphs
        }

        private static volatile EntityValidator ENTITY_VALIDATOR = new EntityValidator() {
            @Override public boolean exists(String entityId) { return true; } // по умолчанию разрешаем всё
            @Override public boolean isLiving(String entityId) { return true; }
        };

        private MorphRuntime() {}

        public static void setConfig(MorphConfig cfg) {
            CONFIG = Objects.requireNonNull(cfg);
        }

        public static MorphConfig getConfig() {
            return CONFIG;
        }

        public static void setEntityValidator(EntityValidator validator) {
            ENTITY_VALIDATOR = Objects.requireNonNull(validator);
        }

        /**
         * Основная серверная точка: применить морф игроку.
         * @param playerUuid UUID игрока
         * @param entityId строковый id сущности (e.g. "minecraft:allay", "minecraft:evoker_fangs")
         * @param permissionCheck предикат на разрешение (можешь передать проверку прав/опов)
         * @return текст ошибки или null, если всё ок
         */
        public static String applyMorph(UUID playerUuid, String entityId, Predicate<UUID> permissionCheck) {
            if (playerUuid == null) return "Player UUID is null";
            if (entityId == null || entityId.isBlank()) return "Entity id is empty";
            if (permissionCheck != null && !permissionCheck.test(playerUuid)) {
                return "No permission";
            }
            if (!ENTITY_VALIDATOR.exists(entityId)) {
                return "Unknown entity: " + entityId;
            }
            if (!CONFIG.allowNonLivingMorphs && !ENTITY_VALIDATOR.isLiving(entityId)) {
                return "Non-living morphs are disabled by config";
            }

            // Записать состояние
            MorphStateStore.set(playerUuid, entityId);

            // TODO: твоя серверная логика применения морфа (data/capabilities/attributes/пр.)
            applyMorphServerSide(playerUuid, entityId);

            // Уведомить клиента для рендера
            notifyClientMorphApplied(playerUuid, entityId);

            return null;
        }

        /**
         * Снять морф с игрока.
         */
        public static void clearMorph(UUID playerUuid, Predicate<UUID> permissionCheck) {
            if (playerUuid == null) return;
            if (permissionCheck != null && !permissionCheck.test(playerUuid)) return;

            MorphStateStore.clear(playerUuid);

            // TODO: снять морф в серверной логике (вернуть рендер/атрибуты к норме)
            clearMorphServerSide(playerUuid);

            // Уведомить клиента
            notifyClientMorphApplied(playerUuid, null);
        }

        /**
         * Узнать текущий морф игрока (server authoritative).
         */
        public static String getCurrentMorph(UUID playerUuid) {
            return MorphStateStore.get(playerUuid);
        }

        // ====== ТУТ НУЖНА ТВОЯ ИНТЕГРАЦИЯ ======

        /**
         * Примени морф на серверной стороне (твоё внутреннее API).
         * Здесь удобно дернуть твой существующий MorphServer/IMorphingComponent и т.п.
         */
        private static void applyMorphServerSide(UUID playerUuid, String entityId) {
            // TODO: integrate with your server-side morph pipeline.
            // Например: MorphServer.apply(playerUuid, entityId);
        }

        /**
         * Сними морф на серверной стороне (твоё внутреннее API).
         */
        private static void clearMorphServerSide(UUID playerUuid) {
            // TODO: integrate with your server-side morph pipeline.
            // Например: MorphServer.clear(playerUuid);
        }

        /**
         * Отправь на клиент пакет, чтобы клиент переключил рендер.
         * В Части 2 (Fabric) мы добавили реальную S2C-отправку; тут — заглушка.
         */
        private static void notifyClientMorphApplied(UUID playerUuid, String entityIdOrNull) {
            // TODO: send S2C packet to player with new morph id (or null to clear).
            // Например (Fabric): ServerPlayNetworking.send(player, CHANNEL, buf);
        }
    }