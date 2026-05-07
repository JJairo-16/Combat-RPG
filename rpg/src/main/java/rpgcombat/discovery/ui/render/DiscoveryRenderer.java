package rpgcombat.discovery.ui.render;

import static rpgcombat.discovery.ui.filter.DiscoveryFilter.hasFilters;
import static rpgcombat.discovery.ui.render.DiscoveryText.*;
import static rpgcombat.discovery.ui.render.DiscoveryUiStyle.*;

import java.util.ArrayList;
import java.util.List;

import rpgcombat.discovery.ui.filter.DiscoveryFilter;
import rpgcombat.discovery.ui.models.DiscoveryCategoryView;
import rpgcombat.discovery.ui.models.DiscoveryEntryView;
import rpgcombat.discovery.ui.models.DiscoveryOverview;
import rpgcombat.discovery.ui.state.DiscoveryUiState;

/** Renderitza el visor sense gestionar entrades de teclat ni estat. */
public final class DiscoveryRenderer {

    /** Renderitza tota la pantalla del visor. */
    public String render(DiscoveryOverview overview, DiscoveryUiState state, int width, int height) {
        StringBuilder out = new StringBuilder(width * Math.max(height, MIN_HEIGHT));
        out.append(RESET);

        if (width < MIN_WIDTH || height < MIN_HEIGHT) {
            appendRawLine(out, RED + BOLD + fitPlain(TOO_SMALL, width) + RESET);
            appendRawLine(out, MUTED + fitPlain(RESIZE_WINDOW, width) + RESET);
            fillRest(out, height, 2);
            return out.toString();
        }

        DiscoveryLayout layout = DiscoveryLayout.forWidth(width, state.categoriesCollapsed());
        int contentRows = Math.max(1, height - DiscoveryUiStyle.HEADER_LINES - DiscoveryUiStyle.FOOTER_LINES);

        List<DiscoveryCategoryView> categories = DiscoveryUiState.safeCategories(overview);
        DiscoveryCategoryView category = state.selectedCategoryView(overview);
        state.resetFilterIfNeeded(category);

        List<DiscoveryEntryView> entries = category == null ? List.of() : state.filteredEntries(category);
        DiscoveryEntryView entry = state.selectedEntryView(entries);

        state.updateScrolls(categories.size(), entries.size(), contentRows);

        appendHeader(out, overview, width);
        appendColumnHeader(out, category, layout, state);

        List<String> categoryLines = categoryLines(categories, layout.categoryWidth(), contentRows, state);
        List<String> entryLines = entryLines(category, entries, layout.entryWidth(), contentRows, state);
        List<String> detailLines = detailLines(category, entry, layout.detailWidth(), contentRows);

        for (int i = 0; i < contentRows; i++) {
            appendRawLine(out,
                    lineAt(categoryLines, i)
                            + COLUMN_SEPARATOR
                            + lineAt(entryLines, i)
                            + COLUMN_SEPARATOR
                            + lineAt(detailLines, i));
        }

        appendFooter(out, width, category, state);
        return out.toString();
    }

    /** Afegeix la capçalera amb progrés global. */
    private void appendHeader(StringBuilder out, DiscoveryOverview overview, int width) {
        int discovered = overview.discovered();
        int total = overview.total();
        int percent = total <= 0 ? 0 : (int) Math.round(discovered * 100.0 / total);
        int barWidth = progressWidth(width);

        String rightPlain = discovered + " / " + total + "  " + plainProgressBar(discovered, total, barWidth)
                + " " + percent + "%";

        int spaces = Math.max(
                MIN_SPACING,
                width - HEADER_VISIBLE_SIDE_PADDING - displayWidth(TITLE) - displayWidth(rightPlain));

        appendRawLine(out, BORDER + "╭" + "─".repeat(Math.max(0, width - OUTER_BORDER_WIDTH)) + "╮" + RESET);

        out.append(BORDER).append("│ ").append(RESET);
        out.append(BOLD).append(CYAN).append(TITLE).append(RESET);
        out.append(" ".repeat(spaces));
        out.append(GREEN).append(discovered).append(RESET)
                .append(MUTED).append(" / ").append(RESET)
                .append(total)
                .append("  ")
                .append(progressBar(discovered, total, barWidth))
                .append(" ")
                .append(YELLOW).append(percent).append("%").append(RESET);
        out.append(BORDER).append(" │").append(RESET).append("\033[K\n");

        appendRawLine(out, BORDER + "╰" + "─".repeat(Math.max(0, width - OUTER_BORDER_WIDTH)) + "╯" + RESET);
        appendRawLine(out, "");
    }

    /** Afegeix els títols de les columnes. */
    private void appendColumnHeader(
            StringBuilder out,
            DiscoveryCategoryView category,
            DiscoveryLayout layout,
            DiscoveryUiState state) {

        String categoryTitle = state.categoriesCollapsed() ? COLUMN_CATEGORY_COMPACT : COLUMN_CATEGORIES;

        appendRawLine(out,
                cell(categoryTitle, layout.categoryWidth(), BOLD + CYAN)
                        + COLUMN_SEPARATOR
                        + entryHeaderCell(category, layout.entryWidth(), state)
                        + COLUMN_SEPARATOR
                        + cell(COLUMN_DETAIL, layout.detailWidth(), BOLD + CYAN));

        appendRawLine(out,
                MUTED
                        + "─".repeat(layout.categoryWidth())
                        + "─┼─"
                        + "─".repeat(layout.entryWidth())
                        + "─┼─"
                        + "─".repeat(layout.detailWidth())
                        + RESET);
    }

    /** Crea la capçalera d'entrades amb filtre actiu. */
    private String entryHeaderCell(DiscoveryCategoryView category, int width, DiscoveryUiState state) {
        if (category == null) {
            return cell(COLUMN_DISCOVERIES, width, BOLD + CYAN);
        }

        String title = upper(category.title());

        if (!hasFilters(category)) {
            return cell(title, width, BOLD + CYAN);
        }

        String filter = FILTER_PREFIX + state.activeFilter().label();
        int reservedRight = displayWidth(filter) + FILTER_RIGHT_PADDING;
        int titleWidth = Math.max(DETAIL_CONTENT_MIN_WIDTH, width - reservedRight - FILTER_LEFT_GAP);

        String fittedTitle = fitPlain(title, titleWidth);
        int spaces = Math.max(
                FILTER_LEFT_GAP,
                width - displayWidth(fittedTitle) - displayWidth(filter) - FILTER_RIGHT_PADDING);

        return BOLD + CYAN
                + fittedTitle
                + RESET
                + " ".repeat(spaces)
                + MUTED
                + filter
                + RESET
                + " ".repeat(FILTER_RIGHT_PADDING);
    }

    /** Crea les línies de la columna de categories. */
    private List<String> categoryLines(
            List<DiscoveryCategoryView> categories,
            int width,
            int contentRows,
            DiscoveryUiState state) {

        List<String> lines = new ArrayList<>(contentRows);

        if (state.categoriesCollapsed()) {
            int end = Math.min(categories.size(), state.categoryScroll() + contentRows);
            for (int i = state.categoryScroll(); i < end; i++) {
                DiscoveryCategoryView category = categories.get(i);
                boolean selected = i == state.selectedCategory();

                String label = centerPlain(compactCategoryLabel(category.title()), width);
                lines.add(selected ? selectedCell(label, width) : cell(label, width, MUTED));
            }

            padEmpty(lines, contentRows, width);
            return lines;
        }

        int end = Math.min(categories.size(), state.categoryScroll() + contentRows);
        for (int i = state.categoryScroll(); i < end; i++) {
            DiscoveryCategoryView category = categories.get(i);
            boolean selected = state.activePanel() == DiscoveryUiState.Panel.CATEGORIES
                    && i == state.selectedCategory();

            String cursor = selected ? "▸ " : "  ";
            String progress = category.discovered() + "/" + category.total();
            String barPlain = plainMiniProgressBar(category.discovered(), category.total(), CATEGORY_PROGRESS_WIDTH);

            int titleWidth = Math.max(
                    1,
                    width - displayWidth(cursor) - displayWidth(" ") - displayWidth(barPlain)
                            - displayWidth(" ") - displayWidth(progress));

            String title = fitPlain(category.title(), titleWidth);
            String plain = cursor + title + " " + barPlain + " " + progress;

            if (selected) {
                lines.add(selectedCell(plain, width));
            } else {
                String styled = cursor
                        + WHITE + title + RESET
                        + " "
                        + miniProgressBar(category.discovered(), category.total(), CATEGORY_PROGRESS_WIDTH)
                        + " "
                        + MUTED + progress + RESET;

                lines.add(padStyled(styled, plain, width));
            }
        }

        padEmpty(lines, contentRows, width);
        return lines;
    }

    /** Crea les línies de la columna d'entrades. */
    private List<String> entryLines(
            DiscoveryCategoryView category,
            List<DiscoveryEntryView> entries,
            int width,
            int contentRows,
            DiscoveryUiState state) {

        List<String> lines = new ArrayList<>(contentRows);

        if (category == null) {
            lines.add(cell(NO_CATEGORIES, width, MUTED));
            padEmpty(lines, contentRows, width);
            return lines;
        }

        if (entries.isEmpty()) {
            if (hasFilters(category) && state.activeFilter() != DiscoveryFilter.ALL) {
                lines.add(cell(NO_FILTER_RESULTS + state.activeFilter().label(), width, MUTED));
            } else {
                lines.add(cell(NO_ENTRIES, width, MUTED));
            }
            padEmpty(lines, contentRows, width);
            return lines;
        }

        int end = Math.min(entries.size(), state.entryScroll() + contentRows);
        for (int i = state.entryScroll(); i < end; i++) {
            DiscoveryEntryView entry = entries.get(i);
            boolean selected = state.activePanel() == DiscoveryUiState.Panel.ENTRIES && i == state.selectedEntry();

            String cursor = selected ? "▸ " : "  ";
            String icon = entry.discovered() ? "◆ " : "◇ ";
            String title = fitPlain(entry.title(), Math.max(1, width - displayWidth(cursor) - displayWidth(icon)));
            String plain = cursor + icon + title;

            if (selected) {
                lines.add(selectedCell(plain, width));
            } else {
                String styled = cursor
                        + (entry.discovered() ? GREEN : MUTED)
                        + icon
                        + RESET
                        + (entry.discovered() ? WHITE : MUTED)
                        + title
                        + RESET;

                lines.add(padStyled(styled, plain, width));
            }
        }

        padEmpty(lines, contentRows, width);
        return lines;
    }

    /** Crea les línies del panell de detall. */
    private List<String> detailLines(
            DiscoveryCategoryView category,
            DiscoveryEntryView entry,
            int width,
            int contentRows) {

        List<String> lines = new ArrayList<>(contentRows);

        if (entry == null) {
            addDetailWrapped(lines, SELECT_ENTRY, width, MUTED);
            padEmpty(lines, contentRows, width);
            return lines;
        }

        boolean discovered = entry.discovered();

        lines.add(detailCell(discovered ? upper(entry.title()) : maskTitle(entry.title()), width,
                discovered ? BOLD + WHITE : BOLD + LOCKED));
        lines.add(detailBadgeCell(discovered ? STATUS_DISCOVERED : STATUS_LOCKED, width,
                discovered ? GREEN + BOLD : LOCKED));
        lines.add(cell("─".repeat(width), width, MUTED));

        if (discovered) {
            appendDiscoveredDetail(lines, entry, width);
        } else {
            appendLockedDetail(lines, category, entry, width);
        }

        trimAndPad(lines, contentRows, width);
        return lines;
    }

    /** Afegeix el detall d'una entrada descoberta. */
    private void appendDiscoveredDetail(List<String> lines, DiscoveryEntryView entry, int width) {
        if (hasText(entry.shortDescription())) {
            addDetailWrapped(lines, entry.shortDescription(), width, "");
            lines.add(blankCell(width));
        }

        appendDetailsBlock(lines, entry.details(), entry.shortDescription(), width, false);

        if (hasText(entry.discoveredWhen())) {
            lines.add(detailCell(SECTION_DISCOVERED, width, MAGENTA + BOLD));
            addDetailWrapped(lines, entry.discoveredWhen(), width, "");
        }
    }

    /** Afegeix el detall d'una entrada bloquejada. */
    private void appendLockedDetail(
            List<String> lines,
            DiscoveryCategoryView category,
            DiscoveryEntryView entry,
            int width) {

        if (hasText(entry.shortDescription())) {
            addDetailWrapped(lines, maskSentence(entry.shortDescription()), width, LOCKED);
            lines.add(blankCell(width));
        } else {
            addDetailWrapped(lines, LOCKED_VALUE, width, LOCKED);
            lines.add(blankCell(width));
        }

        List<String> details = entry.details();

        if (details != null && !details.isEmpty()) {
            appendDetailsBlock(lines, details, entry.shortDescription(), width, true);
        } else {
            appendLockedDetailsFromCategoryShape(lines, category, width);
        }

        lines.add(detailCell("Pista", width, YELLOW + BOLD));

        if (hasText(entry.hint())) {
            addDetailWrapped(lines, entry.hint(), width, "");
        } else {
            addDetailWrapped(lines, "Continua explorant per revelar aquesta fitxa.", width, "");
        }
    }

    /** Afegeix dades ocultes segons entrades ja descobertes. */
    private static void appendLockedDetailsFromCategoryShape(
            List<String> lines,
            DiscoveryCategoryView category,
            int width) {

        DetailShape shape = inferDetailShape(category);

        if (shape.labels().isEmpty() && shape.freeFormLineCount() <= 0) {
            return;
        }

        lines.add(detailCell("Dades", width, CYAN + BOLD));

        int freeFormCount = Math.max(0, shape.freeFormLineCount());
        for (int i = 0; i < freeFormCount; i++) {
            addBulletWrapped(lines, LOCKED_VALUE, width, LOCKED);
        }

        for (String label : shape.labels()) {
            addBulletWrapped(lines, label + " " + LOCKED_VALUE, width, LOCKED);
        }

        lines.add(blankCell(width));
    }

    /** Dedueix l'estructura de detall d'una categoria. */
    private static DetailShape inferDetailShape(DiscoveryCategoryView category) {
        List<String> labels = new ArrayList<>();
        int freeFormLineCount = 0;

        if (category == null || category.entries() == null) {
            return new DetailShape(labels, freeFormLineCount);
        }

        for (DiscoveryEntryView entry : category.entries()) {
            if (entry == null || !entry.discovered() || entry.details() == null) {
                continue;
            }

            int currentFreeFormCount = 0;

            for (String detail : entry.details()) {
                if (!hasText(detail)) {
                    continue;
                }

                if (hasText(entry.shortDescription()) && detail.trim().equals(entry.shortDescription().trim())) {
                    continue;
                }

                String label = extractDetailLabel(detail);

                if (hasText(label)) {
                    if (!labels.contains(label)) {
                        labels.add(label);
                    }
                } else {
                    currentFreeFormCount++;
                }
            }

            freeFormLineCount = Math.max(freeFormLineCount, currentFreeFormCount);
        }

        return new DetailShape(labels, freeFormLineCount);
    }

    /** Extreu l'etiqueta inicial d'un detall. */
    private static String extractDetailLabel(String detail) {
        if (!hasText(detail)) {
            return "";
        }

        String trimmed = detail.trim();
        int colon = trimmed.indexOf(':');

        if (colon < 0) {
            return "";
        }

        String label = trimmed.substring(0, colon + 1).trim();

        if (!isValidDetailLabel(label)) {
            return "";
        }

        return label;
    }

    /** Valida si un text pot ser etiqueta de detall. */
    private static boolean isValidDetailLabel(String label) {
        if (!hasText(label)) {
            return false;
        }

        String clean = label.trim();

        if (!clean.endsWith(":")) {
            return false;
        }

        String body = clean.substring(0, clean.length() - 1).trim();
        String lower = lower(body);

        if (body.isEmpty()) {
            return false;
        }

        if (body.length() > 28) {
            return false;
        }

        if (countWords(body) > 3) {
            return false;
        }

        if (startsWithAny(lower,
                "és ", "es ",
                "aquest ", "aquesta ",
                "quan ", "si ",
                "en ", "amb ",
                "per ", "cada ",
                "durant ", "després ", "despres ")) {
            return false;
        }

        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);

            if (Character.isLetter(c)
                    || Character.isWhitespace(c)
                    || c == '\''
                    || c == '·'
                    || c == '/'
                    || c == '-') {
                continue;
            }

            return false;
        }

        return true;
    }

    /** Indica si un text comença per algun prefix. */
    private static boolean startsWithAny(String text, String... prefixes) {
        for (String prefix : prefixes) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /** Forma esperada dels detalls d'una categoria. */
    private record DetailShape(List<String> labels, int freeFormLineCount) {
    }

    /** Afegeix el bloc de dades d'una entrada. */
    private static void appendDetailsBlock(
            List<String> lines,
            List<String> details,
            String shortDescription,
            int width,
            boolean locked) {

        if (details == null || details.isEmpty()) {
            return;
        }

        lines.add(detailCell(SECTION_DATA, width, CYAN + BOLD));

        for (String detail : details) {
            if (!hasText(detail)) {
                continue;
            }
            if (hasText(shortDescription) && detail.trim().equals(shortDescription.trim())) {
                continue;
            }

            String plain = locked ? maskDetail(detail) : detail;
            addBulletWrapped(lines, plain, width, locked ? LOCKED : "");
        }

        lines.add(blankCell(width));
    }

    /** Afegeix el peu d'ajuda. */
    private void appendFooter(StringBuilder out, int width, DiscoveryCategoryView category, DiscoveryUiState state) {
        appendRawLine(out, "");
        appendRawLine(out, MUTED + "─".repeat(width) + RESET);
        appendRawLine(out, MUTED + " " + fitPlain(footerText(category, state), Math.max(1, width - 1)) + RESET);
    }

    /** Retorna el text d'ajuda del peu. */
    private String footerText(DiscoveryCategoryView category, DiscoveryUiState state) {
        String filterHelp = hasFilters(category)
                ? "   " + HELP_FILTER_NEXT + "   " + HELP_FILTER_PREVIOUS
                : "";

        if (state.activePanel() == DiscoveryUiState.Panel.CATEGORIES) {
            return FOOTER_CATEGORY + filterHelp;
        }

        if (state.categoriesCollapsed()) {
            return FOOTER_COLLAPSED + filterHelp;
        }

        return FOOTER_ENTRIES + filterHelp;
    }

    /** Retalla i emplena línies fins al límit. */
    private static void trimAndPad(List<String> lines, int maxLines, int width) {
        if (lines.size() > maxLines) {
            while (lines.size() > Math.max(1, maxLines)) {
                lines.remove(lines.size() - 1);
            }

            int last = lines.size() - 1;
            lines.set(last, detailCell(TRUNCATED_MARKER, width, MUTED));
        }

        padEmpty(lines, maxLines, width);
    }

    /** Emplena amb línies buides fins a la mida indicada. */
    private static void padEmpty(List<String> lines, int targetSize, int width) {
        while (lines.size() < targetSize) {
            lines.add(blankCell(width));
        }
    }

    /** Retorna una cel·la buida. */
    private static String blankCell(int width) {
        return " ".repeat(Math.max(0, width));
    }

    /** Retorna l'abreviatura d'una categoria. */
    private static String compactCategoryLabel(String title) {
        if (title == null || title.isBlank()) {
            return "?";
        }

        return switch (title) {
            case "Armes" -> "AR";
            case "Races" -> "RA";
            case "Perks" -> "PK";
            case "Perks divines" -> "PD";
            case "Missions" -> "MI";
            case "Accions" -> "AC";
            case "Efectes" -> "EF";
            case "Sinergies" -> "SI";
            default -> title.length() <= 2 ? title.toUpperCase() : title.substring(0, 2).toUpperCase();
        };
    }

    /** Crea una barra de progrés amb colors. */
    private static String progressBar(int value, int total, int width) {
        int safeWidth = Math.max(1, width);
        int filled = total <= 0 ? 0 : clamp((int) Math.round(value * safeWidth * 1.0 / total), 0, safeWidth);

        return GREEN
                + "|"
                + "█".repeat(filled)
                + MUTED
                + "░".repeat(Math.max(0, safeWidth - filled))
                + GREEN
                + "|"
                + RESET;
    }

    /** Crea una barra de progrés sense colors. */
    private static String plainProgressBar(int value, int total, int width) {
        int safeWidth = Math.max(1, width);
        int filled = total <= 0 ? 0 : clamp((int) Math.round(value * safeWidth * 1.0 / total), 0, safeWidth);

        return "|"
                + "█".repeat(filled)
                + "░".repeat(Math.max(0, safeWidth - filled))
                + "|";
    }

    /** Crea una barra de progrés petita amb colors. */
    private static String miniProgressBar(int value, int total, int width) {
        int safeWidth = Math.max(1, width);
        int filled = total <= 0 ? 0 : clamp((int) Math.round(value * safeWidth * 1.0 / total), 0, safeWidth);

        return GREEN
                + "▰".repeat(filled)
                + MUTED
                + "▱".repeat(Math.max(0, safeWidth - filled))
                + RESET;
    }

    /** Crea una barra de progrés petita sense colors. */
    private static String plainMiniProgressBar(int value, int total, int width) {
        int safeWidth = Math.max(1, width);
        int filled = total <= 0 ? 0 : clamp((int) Math.round(value * safeWidth * 1.0 / total), 0, safeWidth);

        return "▰".repeat(filled) + "▱".repeat(Math.max(0, safeWidth - filled));
    }

    /** Calcula l'amplada de la barra de progrés. */
    private static int progressWidth(int terminalWidth) {
        if (terminalWidth < HEADER_MEDIUM_BREAKPOINT) {
            return HEADER_PROGRESS_SMALL_WIDTH;
        }
        if (terminalWidth < HEADER_LARGE_BREAKPOINT) {
            return HEADER_PROGRESS_MEDIUM_WIDTH;
        }
        return HEADER_PROGRESS_LARGE_WIDTH;
    }

    /** Afegeix una línia crua i neteja la resta. */
    static void appendRawLine(StringBuilder out, String line) {
        out.append(line == null ? "" : line).append("\033[K\n");
    }

    /** Omple les línies restants amb buit. */
    private static void fillRest(StringBuilder out, int height, int usedLines) {
        int rest = Math.max(0, height - usedLines);
        for (int i = 0; i < rest; i++) {
            appendRawLine(out, "");
        }
    }
}
