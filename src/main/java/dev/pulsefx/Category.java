package dev.pulsefx;

public enum Category {
    VISUALS("Visuals", "Настройка визуальных эффектов и отображения объектов."),
    HUD("HUD", "Элементы интерфейса на экране."),
    UTILITIES("Utilities", "Небольшие удобства."),
    COSMETICS("Cosmetics", "Украшения персонажа."),
    GROUPS("Groups", "Групповые функции."),
    CONFIGS("Configs", "Сохранение и загрузка настроек.");

    public final String label, subtitle;
    Category(String label, String subtitle) { this.label = label; this.subtitle = subtitle; }
}
