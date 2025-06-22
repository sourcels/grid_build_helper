package com.example.gridbuildhelper.ui.widget;

import com.example.gridbuildhelper.config.ConfigManager;
import com.example.gridbuildhelper.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class ProfileListWidget extends EntryListWidget<ProfileListWidget.ProfileEntry> {
    private final Consumer<ModConfig.Profile> onSelect;

    private final int debugWidth;
    private final int debugHeight;
    private final int debugTop;
    private final int debugBottom;
    private final int debugItemHeight;


    public ProfileListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight, Consumer<ModConfig.Profile> onSelect) {
        super(client, width, height, top, bottom, itemHeight);
        this.onSelect = onSelect;
        this.debugWidth = width;
        this.debugHeight = height;
        this.debugTop = top;
        this.debugBottom = bottom;
        this.debugItemHeight = itemHeight;

        System.out.println("ProfileListWidget Constructor -> Width: " + width + ", Height: " + height + ", Top: " + top + ", Bottom: " + bottom + ", ItemHeight: " + itemHeight);

        refreshList();
    }

    public void refreshList() {
        this.clearEntries();
        for (ModConfig.Profile profile : ConfigManager.config.profiles) {
            this.addEntry(new ProfileEntry(profile, onSelect));
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        //builder.put();
    }

    @Override
    public int getRowWidth() {
        return this.width - 12;
    }

    public class ProfileEntry extends Entry<ProfileEntry> {
        private final ModConfig.Profile profile;
        private final Consumer<ModConfig.Profile> onClick;

        public ProfileEntry(ModConfig.Profile profile, Consumer<ModConfig.Profile> onClick) {
            this.profile = profile;
            this.onClick = onClick;
        }

        @Override
        public void render(
                DrawContext context,
                int index,
                int y,
                int x,
                int entryWidth,
                int entryHeight,
                int mouseX,
                int mouseY,
                boolean hovered,
                float delta
        ) {
            MinecraftClient client = MinecraftClient.getInstance();

            System.out.println("ProfileEntry Render -> X: " + x + ", Y: " + y + ", EntryWidth: " + entryWidth + ", EntryHeight: " + entryHeight + ", ExpectedItemHeight: " + debugItemHeight);

            int textColor = profile.name.equals(ConfigManager.selectedProfileInternal.name) ? 0xFFD700 : 0xFFFFFF;

            context.fill(x, y, x + entryWidth, y + entryHeight, 0x6600FFFF); // transparent blue background

            if (hovered) {
                // Draw a slightly darker background on hover, but ensure it's on top of the blue fill or within it
                context.fill(x, y, x + entryWidth, y + entryHeight, 0x44000000); // light dimmed background on hover
            }

            String label = profile.name.equals(ConfigManager.selectedProfileInternal.name)
                    ? "▶ " + profile.name
                    : profile.name;

            int textX = x + 4;
            int textY = y + (entryHeight - client.textRenderer.fontHeight) / 2;

            // Print text details
            System.out.println("   Text: '" + label + "', Text X: " + textX + ", Text Y: " + textY + ", Font Height: " + client.textRenderer.fontHeight);


            context.drawText(client.textRenderer, Text.literal(label), textX, textY, textColor, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            onClick.accept(profile);
            return true;
        }
    }
}