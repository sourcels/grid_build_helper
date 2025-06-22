package com.example.gridbuildhelper.ui;

import com.example.gridbuildhelper.config.ConfigManager;
import com.example.gridbuildhelper.config.ModConfig;
import com.example.gridbuildhelper.ui.widget.ProfileListWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class ModConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget startXField, startYField, startZField, stepField, radiusField, colorField;
    private TextFieldWidget profileNameField;
    private ButtonWidget confirmButton, renameButton, addButton, deleteButton;
    private ProfileListWidget profileListWidget;

    public ModConfigScreen(Screen parent) {
        super(Text.translatable("gridbuildhelper.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int leftX = this.width / 2 - 150;
        int topY = 40;
        int spacing = 25;
        int fieldWidth = 100; // Ширина полей ввода
        int fieldLabelWidth = 100;
        int listPadding = 20;
        int listX = leftX + fieldLabelWidth + fieldWidth + listPadding;
        int listWidth = 120; // Уменьшаем ширину списка.
        int listHeight = this.height - 80;
        int listTop = 40;
        int listBottom = this.height - 40;
        int itemHeight = 8;

        // Поле ввода имени профиля
        profileNameField = new TextFieldWidget(this.textRenderer, leftX, topY, fieldWidth, 20, Text.literal(""));
        profileNameField.setText(ConfigManager.selectedProfileInternal.name);
        this.addDrawableChild(profileNameField);

        // Кнопка добавления профиля
        addButton = ButtonWidget.builder(Text.literal("+"), b -> {
            String name = profileNameField.getText().trim();
            if (!name.isEmpty() && ConfigManager.getProfileByName(name) == null) {
                ModConfig.Profile newProfile = new ModConfig.Profile(name);
                ConfigManager.config.profiles.add(newProfile);
                ConfigManager.selectedProfileInternal = newProfile;
                ConfigManager.config.lastUsedProfile = name;
                ConfigManager.saveProfile();
                refresh();
            }
        }).position(leftX + fieldWidth + 5, topY).size(20, 20).build();
        this.addDrawableChild(addButton);

        // Кнопка переименования
        renameButton = ButtonWidget.builder(Text.literal("✏"), b -> {
            String newName = profileNameField.getText().trim();
            if (!newName.isEmpty()) {
                ConfigManager.renameProfile(ConfigManager.selectedProfileInternal.name, newName);
                ConfigManager.selectedProfileInternal.name = newName;
                profileNameField.setText(newName);
                refresh();
            }
        }).position(leftX + fieldWidth + 30, topY).size(20, 20).build();
        this.addDrawableChild(renameButton);

        // Кнопка удаления
        deleteButton = ButtonWidget.builder(Text.literal("🗑"), b -> {
            String toRemove = ConfigManager.selectedProfileInternal.name;
            if (ConfigManager.config.profiles.size() > 1) {
                ConfigManager.removeProfile(toRemove);
                ConfigManager.selectedProfileInternal = ConfigManager.config.profiles.getFirst();
                ConfigManager.config.lastUsedProfile = ConfigManager.selectedProfileInternal.name;
                ConfigManager.saveProfile();
                profileNameField.setText(ConfigManager.selectedProfileInternal.name);
                refresh();
            }
        }).position(leftX + fieldWidth + 55, topY).size(20, 20).build();
        this.addDrawableChild(deleteButton);

        topY += spacing * 2;

        // Поля конфигурации
        startXField = addLabeledField("gridbuildhelper.config.start_x", ConfigManager.selectedProfileInternal.startX, leftX, topY, fieldLabelWidth, fieldWidth); topY += spacing;
        startYField = addLabeledField("gridbuildhelper.config.start_y", ConfigManager.selectedProfileInternal.startY, leftX, topY, fieldLabelWidth, fieldWidth); topY += spacing;
        startZField = addLabeledField("gridbuildhelper.config.start_z", ConfigManager.selectedProfileInternal.startZ, leftX, topY, fieldLabelWidth, fieldWidth); topY += spacing;
        stepField = addLabeledField("gridbuildhelper.config.step", ConfigManager.selectedProfileInternal.step, leftX, topY, fieldLabelWidth, fieldWidth); topY += spacing;
        radiusField = addLabeledField("gridbuildhelper.config.radius", ConfigManager.selectedProfileInternal.radius, leftX, topY, fieldLabelWidth, fieldWidth); topY += spacing;
        colorField = addLabeledField("gridbuildhelper.config.color", ConfigManager.selectedProfileInternal.outlineColor, leftX, topY, fieldLabelWidth, fieldWidth); topY += spacing;

        // Кнопки внизу
        confirmButton = ButtonWidget.builder(Text.translatable("gridbuildhelper.config.confirm"), b -> {
            try { ConfigManager.selectedProfileInternal.startX = Integer.parseInt(startXField.getText()); } catch (Exception ignored) {}
            try { ConfigManager.selectedProfileInternal.startY = Integer.parseInt(startYField.getText()); } catch (Exception ignored) {}
            try { ConfigManager.selectedProfileInternal.startZ = Integer.parseInt(startZField.getText()); } catch (Exception ignored) {}
            try { ConfigManager.selectedProfileInternal.step = Integer.parseInt(stepField.getText()); } catch (Exception ignored) {}
            try { ConfigManager.selectedProfileInternal.radius = Integer.parseInt(radiusField.getText()); } catch (Exception ignored) {}
            String colorText = colorField.getText().toUpperCase();
            if (colorText.length() == 6) {
                ConfigManager.selectedProfileInternal.outlineColor = colorText;
            }
            ConfigManager.saveProfile();
            this.close();
        }).position(this.width / 2 - 100, this.height - 30).size(90, 20).build();
        this.addDrawableChild(confirmButton);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gridbuildhelper.config.cancel"), b -> {
            this.close();
        }).position(this.width / 2 + 10, this.height - 30).size(90, 20).build());

        profileListWidget = new ProfileListWidget(this.client, listWidth, listHeight, listTop, listBottom, itemHeight, this::onProfileSelected);
        profileListWidget.setX(listX);
        this.addSelectableChild(profileListWidget);
        this.addDrawableChild(profileListWidget);
    }

    private TextFieldWidget addLabeledField(String translationKey, Object value, int x, int y, int labelWidth, int fieldWidth) {
        this.addDrawableChild(ButtonWidget.builder(Text.translatable(translationKey), b -> {})
                .position(x, y)
                .size(labelWidth, 20)
                .build());
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x + labelWidth + 5, y, fieldWidth, 20, Text.literal(""));
        field.setText(value.toString());
        this.addDrawableChild(field);
        return field;
    }

    private void onProfileSelected(ModConfig.Profile profile) {
        ConfigManager.selectedProfileInternal = profile;
        ConfigManager.config.lastUsedProfile = profile.name;
        ConfigManager.saveProfile();
        profileNameField.setText(profile.name);
        loadProfileIntoFields();
        refresh();
    }

    private void loadProfileIntoFields() {
        startXField.setText(String.valueOf(ConfigManager.selectedProfileInternal.startX));
        startYField.setText(String.valueOf(ConfigManager.selectedProfileInternal.startY));
        startZField.setText(String.valueOf(ConfigManager.selectedProfileInternal.startZ));
        stepField.setText(String.valueOf(ConfigManager.selectedProfileInternal.step));
        radiusField.setText(String.valueOf(ConfigManager.selectedProfileInternal.radius));
        colorField.setText(ConfigManager.selectedProfileInternal.outlineColor);
    }

    private void refresh() {
        profileListWidget.refreshList();
        deleteButton.active = ConfigManager.config.profiles.size() > 1;
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(this.parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}