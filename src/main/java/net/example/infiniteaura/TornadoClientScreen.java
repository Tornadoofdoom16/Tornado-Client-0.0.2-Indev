package net.example.infiniteaura;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.List;

public class TornadoClientScreen extends Screen {
    private final Screen parent;
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;
    private TextFieldWidget listInput;
    private TextFieldWidget botNameInput;
    private boolean editingFriends = false;

    public TornadoClientScreen(Screen parent) {
        super(Text.of("Tornado Client Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = 20; 
        int center = this.width / 2;

        // 1. Module Enabled
        this.addDrawableChild(ButtonWidget.builder(Text.of("Module Enabled: " + settings.enabled), button -> {
            settings.enabled = !settings.enabled;
            button.setMessage(Text.of("Module Enabled: " + settings.enabled));
        }).dimensions(center - 100, y, 200, 20).build());

        y += 24;

        // 2. Legit Combat
        this.addDrawableChild(ButtonWidget.builder(Text.of("Legit Crystal: " + settings.legitCrystal), button -> {
            settings.legitCrystal = !settings.legitCrystal;
            button.setMessage(Text.of("Legit Crystal: " + settings.legitCrystal));
        }).dimensions(center - 100, y, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Legit Aura: " + settings.legitKillAura), button -> {
            settings.legitKillAura = !settings.legitKillAura;
            button.setMessage(Text.of("Legit Aura: " + settings.legitKillAura));
        }).dimensions(center + 2, y, 98, 20).build());
        
        y += 24;
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Elytra Mace: " + settings.elytraMace), button -> {
            settings.elytraMace = !settings.elytraMace;
            button.setMessage(Text.of("Elytra Mace: " + settings.elytraMace));
        }).dimensions(center - 100, y, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("PRO BOT AI: " + settings.pvpBot), button -> {
            settings.pvpBot = !settings.pvpBot;
            button.setMessage(Text.of("PRO BOT AI: " + settings.pvpBot));
        }).dimensions(center + 2, y, 98, 20).build());

        y += 24;

        // ... rest of UI ...
        // (Just like before, assuming the rest of the buttons are here)
        
        // --- LIST MANAGEMENT ---
        this.addDrawableChild(ButtonWidget.builder(Text.of("Editing: " + (editingFriends ? "FRIENDS" : "TARGETS")), button -> {
            editingFriends = !editingFriends;
            button.setMessage(Text.of("Editing: " + (editingFriends ? "FRIENDS" : "TARGETS")));
        }).dimensions(center - 100, y + 144, 200, 20).build()); // Adjusted Y

        listInput = new TextFieldWidget(this.textRenderer, center - 100, y + 168, 150, 20, Text.of("Name"));
        this.addDrawableChild(listInput);

        this.addDrawableChild(ButtonWidget.builder(Text.of("Add"), button -> {
            String name = listInput.getText();
            List<String> currentList = editingFriends ? settings.friendsList : settings.targetList;
            if (!name.isEmpty() && !currentList.contains(name)) {
                currentList.add(name);
                listInput.setText("");
            }
        }).dimensions(center + 55, y + 168, 45, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 5, 0xFFFFFF);
        
        int listY = 10;
        String listTitle = editingFriends ? "§aFriends List (Safe):" : "§cTarget List (Attack):";
        List<String> listToShow = editingFriends ? settings.friendsList : settings.targetList;
        
        context.drawTextWithShadow(this.textRenderer, listTitle, 10, listY, 0xFFFFFF);
        for (String name : listToShow) {
            listY += 10;
            context.drawTextWithShadow(this.textRenderer, "- " + name, 15, listY, 0xDDDDDD);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}