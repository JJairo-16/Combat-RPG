package rpgcombat.discovery.ui.render;

import static rpgcombat.discovery.ui.render.DiscoveryText.clamp;
import static rpgcombat.discovery.ui.render.DiscoveryUiStyle.*;

/** Amplades calculades de les columnes del visor de descobriments. */
public record DiscoveryLayout(int categoryWidth, int entryWidth, int detailWidth) {

    /** Calcula el layout segons l'amplada disponible i l'estat de categories. */
    public static DiscoveryLayout forWidth(int width, boolean categoriesCollapsed) {
        int categoryWidth = categoriesCollapsed
                ? COLLAPSED_CATEGORY_WIDTH
                : clamp(width / CATEGORY_WIDTH_RATIO, CATEGORY_MIN_WIDTH, CATEGORY_MAX_WIDTH);

        int entryWidth = categoriesCollapsed
                ? clamp(width / ENTRY_WIDTH_RATIO, COLLAPSED_ENTRY_MIN_WIDTH, COLLAPSED_ENTRY_MAX_WIDTH)
                : clamp(width / ENTRY_WIDTH_RATIO, ENTRY_MIN_WIDTH, ENTRY_MAX_WIDTH);

        int detailWidth = Math.max(
                DETAIL_MIN_WIDTH,
                width - categoryWidth - entryWidth - COLUMN_SEPARATOR_TOTAL_WIDTH);

        return new DiscoveryLayout(categoryWidth, entryWidth, detailWidth);
    }
}